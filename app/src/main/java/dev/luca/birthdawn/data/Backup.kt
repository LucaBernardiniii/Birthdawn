package dev.luca.birthdawn.data

import dev.luca.birthdawn.R
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import dev.luca.birthdawn.notify.ReminderScheduler
import dev.luca.birthdawn.ui.theme.AppearanceSettings
import dev.luca.birthdawn.util.Photos
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup completo dei dati di Birthdawn in un file .zip:
 *
 *   manifest.json          informazioni sul backup (formato, versione, data)
 *   files/...              TUTTI i file dell'app: people.json (persone + idee regalo), photos/…
 *   prefs/<nome>.json      TUTTE le impostazioni (tuo compleanno, aspetto, orario notifiche, …)
 *
 * Non c'è un elenco fisso di cosa salvare: qualsiasi file o impostazione aggiunti in futuro
 * finiscono automaticamente nel backup.
 */
object Backup {
    private const val FORMAT = "birthdawn-backup"
    private const val FORMAT_VERSION = 1

    /** Impostazioni interne che non ha senso ripristinare (es. "notifiche di oggi già inviate"). */
    private val EXCLUDED_PREF_KEYS = setOf("last_check")

    data class Summary(val people: Int, val photos: Int, val gifts: Int)

    fun suggestedFileName(): String = "Birthdawn-backup-${LocalDate.now()}.zip"

    // ------------------------------------------------------------------ esportazione

    fun exportTo(context: Context, uri: Uri): Summary {
        val ctx = context.applicationContext
        PeopleRepository.load(ctx)
        val stream = ctx.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException(context.getString(R.string.err_create_file))
        ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
            val manifest = JSONObject().apply {
                put("format", FORMAT)
                put("formatVersion", FORMAT_VERSION)
                put("app", "Birthdawn")
                put("appVersion", versionName(ctx))
                put("createdAt", Instant.now().toString())
            }
            zip.writeEntry("manifest.json", manifest.toString(2).toByteArray())

            // Tutti i file dell'app
            ctx.filesDir.walkTopDown()
                .filter { it.isFile && !isSystemFile(it) && !it.name.endsWith(".tmp") }
                .forEach { f ->
                    val rel = f.relativeTo(ctx.filesDir).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry("files/$rel"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }

            // Tutte le impostazioni
            prefsNames(ctx).forEach { name ->
                val json = prefsToJson(ctx.getSharedPreferences(name, Context.MODE_PRIVATE))
                zip.writeEntry("prefs/$name.json", json.toString(2).toByteArray())
            }
        }
        return summary(ctx)
    }

    // ------------------------------------------------------------------ importazione

    /** Sostituisce TUTTI i dati attuali con quelli del backup. */
    fun importFrom(context: Context, uri: Uri): Summary {
        val ctx = context.applicationContext
        val tmp = File(ctx.cacheDir, "import_tmp").apply { deleteRecursively(); mkdirs() }
        try {
            // 1. Estrae il backup in una cartella temporanea (i dati attuali non vengono toccati)
            val stream = ctx.contentResolver.openInputStream(uri)
                ?: throw IOException(context.getString(R.string.err_read_file))
            val root = tmp.canonicalPath + File.separator
            ZipInputStream(BufferedInputStream(stream)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val out = File(tmp, entry.name).canonicalFile
                    // Protezione: nessun file può finire fuori dalla cartella temporanea
                    if (!out.path.startsWith(root)) throw IOException(context.getString(R.string.err_invalid))
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // 2. Controlla che sia davvero un backup valido
            val manifestFile = File(tmp, "manifest.json")
            val manifest = runCatching { JSONObject(manifestFile.readText()) }.getOrNull()
            if (manifest == null || manifest.optString("format") != FORMAT) {
                throw IOException(context.getString(R.string.err_not_backup))
            }
            if (manifest.optInt("formatVersion", 0) > FORMAT_VERSION) {
                throw IOException(context.getString(R.string.err_newer))
            }
            val peopleFile = File(tmp, "files/people.json")
            if (peopleFile.exists()) {
                runCatching { JSONArray(peopleFile.readText()) }
                    .getOrElse { throw IOException(context.getString(R.string.err_corrupt)) }
            }

            // 3. Sostituisce i file
            ctx.filesDir.listFiles()?.filterNot { isSystemFile(it) }?.forEach { it.deleteRecursively() }
            File(tmp, "files").takeIf { it.exists() }?.copyRecursively(ctx.filesDir, overwrite = true)

            // 4. Sostituisce le impostazioni
            File(tmp, "prefs").listFiles { f -> f.name.endsWith(".json") }?.forEach { f ->
                val prefs = ctx.getSharedPreferences(f.name.removeSuffix(".json"), Context.MODE_PRIVATE)
                restorePrefs(prefs, JSONObject(f.readText()))
            }
        } finally {
            tmp.deleteRecursively()
        }

        // 5. Ricarica tutto
        Photos.clearCache()
        PeopleRepository.reload(ctx)
        AppearanceSettings.load(ctx)
        ReminderScheduler.schedule(ctx)
        return summary(ctx)
    }

    // ------------------------------------------------------------------ utilità

    private fun summary(ctx: Context): Summary {
        val people = PeopleRepository.people.value
        val photos = File(ctx.filesDir, "photos").listFiles()?.count { it.isFile } ?: 0
        return Summary(people.size, photos, people.sumOf { it.gifts.size })
    }

    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    /** File creati da librerie di sistema, non dati dell'utente. */
    private fun isSystemFile(f: File): Boolean =
        f.name.startsWith("profileinstaller") || f.name.startsWith(".")

    /** Tutti i file di impostazioni dell'app (escluse quelle interne delle librerie). */
    private fun prefsNames(ctx: Context): List<String> {
        val dir = File(ctx.applicationInfo.dataDir, "shared_prefs")
        return dir.listFiles { f -> f.name.endsWith(".xml") }
            ?.map { it.name.removeSuffix(".xml") }
            ?.filterNot { it.startsWith("androidx.") || it.startsWith("com.google.") }
            ?: emptyList()
    }

    /** Ogni valore viene salvato con il suo tipo, così si ripristina identico. */
    private fun prefsToJson(prefs: SharedPreferences): JSONObject {
        val out = JSONObject()
        prefs.all.forEach { (key, value) ->
            if (key in EXCLUDED_PREF_KEYS || value == null) return@forEach
            val typed = JSONObject()
            when (value) {
                is Boolean -> typed.put("type", "boolean").put("value", value)
                is Int -> typed.put("type", "int").put("value", value)
                is Long -> typed.put("type", "long").put("value", value)
                is Float -> typed.put("type", "float").put("value", value.toDouble())
                is String -> typed.put("type", "string").put("value", value)
                is Set<*> -> typed.put("type", "stringSet").put("value", JSONArray(value.map { it.toString() }))
                else -> return@forEach
            }
            out.put(key, typed)
        }
        return out
    }

    private fun restorePrefs(prefs: SharedPreferences, json: JSONObject) {
        val editor = prefs.edit()
        // Cancella tutto tranne i valori interni esclusi dal backup
        prefs.all.keys.filterNot { it in EXCLUDED_PREF_KEYS }.forEach { editor.remove(it) }
        json.keys().forEach { key ->
            if (key in EXCLUDED_PREF_KEYS) return@forEach
            val typed = json.optJSONObject(key) ?: return@forEach
            when (typed.optString("type")) {
                "boolean" -> editor.putBoolean(key, typed.getBoolean("value"))
                "int" -> editor.putInt(key, typed.getInt("value"))
                "long" -> editor.putLong(key, typed.getLong("value"))
                "float" -> editor.putFloat(key, typed.getDouble("value").toFloat())
                "string" -> editor.putString(key, typed.getString("value"))
                "stringSet" -> {
                    val arr = typed.getJSONArray("value")
                    editor.putStringSet(key, (0 until arr.length()).map { arr.getString(it) }.toSet())
                }
            }
        }
        editor.commit()
    }

    private fun versionName(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull() ?: "?"
}
