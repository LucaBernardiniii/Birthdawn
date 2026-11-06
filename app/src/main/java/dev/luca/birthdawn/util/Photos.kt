package dev.luca.birthdawn.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import java.io.File
import java.io.FileOutputStream

/** Copia le foto scelte nella memoria interna (ridimensionate) e le carica per la UI. */
object Photos {
    private const val MAX_STORED_SIZE = 1600
    private val cache = LruCache<String, ImageBitmap>(48)

    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    fun delete(context: Context, name: String) {
        file(context, name).delete()
        cache.snapshot().keys.filter { it.startsWith("$name@") }.forEach { cache.remove(it) }
    }

    /** Importa l'immagine scelta dall'utente. Restituisce il nome del file salvato. */
    fun importFromUri(context: Context, uri: Uri, personId: String): String? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_STORED_SIZE)
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (decoded == null) {
            null
        } else {
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            val bitmap = rotate(decoded, orientation)
            val name = "${personId}_${System.currentTimeMillis()}.jpg"
            FileOutputStream(file(context, name)).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)
            }
            name
        }
    } catch (e: Exception) {
        null
    }

    /** Salva una foto letta da uno stream (es. la foto di un contatto). Restituisce il nome del file. */
    fun importFromStream(context: Context, input: java.io.InputStream, personId: String): String? {
        val tmp = File(context.cacheDir, "contact_photo.tmp")
        return try {
            input.use { i -> FileOutputStream(tmp).use { i.copyTo(it) } }
            importFromUri(context, Uri.fromFile(tmp), personId)
        } catch (e: Exception) {
            null
        } finally {
            tmp.delete()
        }
    }

    /** Carica una foto già salvata, ridotta a circa [maxSize] pixel. */
    fun load(context: Context, name: String, maxSize: Int): ImageBitmap? {
        val key = "$name@$maxSize"
        cache.get(key)?.let { return it }
        val f = file(context, name)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        }
        val bmp = BitmapFactory.decodeFile(f.path, opts)?.asImageBitmap() ?: return null
        cache.put(key, bmp)
        return bmp
    }

    private val accentCache = HashMap<String, Int>()

    /** Svuota le cache in memoria (dopo l'importazione di un backup). */
    fun clearCache() {
        cache.evictAll()
        synchronized(accentCache) { accentCache.clear() }
    }

    /** Colore d'accento estratto dalla foto (vivace se possibile, altrimenti dominante). */
    fun accentColor(context: Context, name: String): Int? {
        synchronized(accentCache) { accentCache[name]?.let { return it } }
        val f = file(context, name)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 200)
        }
        val bmp = BitmapFactory.decodeFile(f.path, opts) ?: return null
        val palette = Palette.from(bmp).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
            ?: return null
        synchronized(accentCache) { accentCache[name] = swatch.rgb }
        return swatch.rgb
    }

    private fun sampleSize(w: Int, h: Int, max: Int): Int {
        var s = 1
        while (w / (s * 2) >= max || h / (s * 2) >= max) s *= 2
        return s
    }

    private fun rotate(bmp: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bmp
        }
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
