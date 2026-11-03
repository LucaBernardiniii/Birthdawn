package dev.luca.birthdawn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.luca.birthdawn.data.PhotoFocus
import dev.luca.birthdawn.ui.theme.screenBackground
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val MAX_PHOTO_ZOOM = 5f

/** Altezza standard del banner della persona. */
val BANNER_HEIGHT = 300.dp

/**
 * Porzione della foto (in pixel dell'immagine) da mostrare in un riquadro W x H,
 * riempiendolo sempre senza bordi vuoti.
 */
private fun visibleRect(iw: Float, ih: Float, w: Float, h: Float, f: PhotoFocus): Rect {
    if (w <= 0f || h <= 0f || iw <= 0f || ih <= 0f) return Rect(0f, 0f, iw, ih)
    val scale = max(w / iw, h / ih) * f.zoom.coerceIn(1f, MAX_PHOTO_ZOOM)
    val vw = min(w / scale, iw)
    val vh = min(h / scale, ih)
    val left = (f.x * iw - vw / 2f).coerceIn(0f, iw - vw)
    val top = (f.y * ih - vh / 2f).coerceIn(0f, ih - vh)
    return Rect(left, top, left + vw, top + vh)
}

/** Riporta il punto di fuoco entro i limiti, così la foto copre sempre tutto il riquadro. */
private fun clampFocus(f: PhotoFocus, iw: Float, ih: Float, w: Float, h: Float): PhotoFocus {
    val zoom = f.zoom.coerceIn(1f, MAX_PHOTO_ZOOM)
    if (w <= 0f || h <= 0f) return f.copy(zoom = zoom)
    val scale = max(w / iw, h / ih) * zoom
    val halfX = (w / scale) / iw / 2f
    val halfY = (h / scale) / ih / 2f
    val x = if (halfX >= 0.5f) 0.5f else f.x.coerceIn(halfX, 1f - halfX)
    val y = if (halfY >= 0.5f) 0.5f else f.y.coerceIn(halfY, 1f - halfY)
    return PhotoFocus(x, y, zoom)
}

/** Disegna la foto rispettando il punto di fuoco scelto dall'utente (come ContentScale.Crop, ma regolabile). */
@Composable
fun FocusedImage(bitmap: ImageBitmap, focus: PhotoFocus, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val iw = bitmap.width.toFloat()
        val ih = bitmap.height.toFloat()
        val r = visibleRect(iw, ih, size.width, size.height, focus)
        val sx = floor(r.left).toInt().coerceIn(0, bitmap.width - 1)
        val sy = floor(r.top).toInt().coerceIn(0, bitmap.height - 1)
        val sw = r.width.roundToInt().coerceIn(1, bitmap.width - sx)
        val sh = r.height.roundToInt().coerceIn(1, bitmap.height - sy)
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(sw, sh),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

/**
 * Schermata a tutto schermo per scegliere la parte di foto visibile:
 * trascina per spostare, pizzica per ingrandire. Conferma con "Fatto" in alto a destra.
 */
@Composable
fun PhotoAdjustScreen(
    bitmap: ImageBitmap,
    initial: PhotoFocus,
    onDismiss: () -> Unit,
    onConfirm: (PhotoFocus) -> Unit,
) {
    var focus by remember { mutableStateOf(initial) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()
    val bannerAspect = screenWidth / BANNER_HEIGHT.value
    val colors = MaterialTheme.colorScheme

    BackHandler(onBack = onDismiss)

    Column(
        Modifier
            .fillMaxSize()
            .screenBackground()
            // Intercetta i tocchi, così non arrivano alla schermata sotto
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        // Barra in alto: annulla, titolo, conferma
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Annulla")
            }
            Text(
                "Regola la foto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { onConfirm(focus) },
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Fatto")
            }
        }

        Text(
            "Trascina per spostare, pizzica con due dita per ingrandire",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.weight(1f))

        // Anteprima del banner, a tutta larghezza come nella pagina della persona
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(bannerAspect)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .pointerInput(bitmap) {
                    val iw = bitmap.width.toFloat()
                    val ih = bitmap.height.toFloat()
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val current = focus
                        val zoom = (current.zoom * zoomChange).coerceIn(1f, MAX_PHOTO_ZOOM)
                        val scale = max(w / iw, h / ih) * zoom
                        focus = clampFocus(
                            PhotoFocus(
                                x = current.x - pan.x / (scale * iw),
                                y = current.y - pan.y / (scale * ih),
                                zoom = zoom,
                            ),
                            iw, ih, w, h,
                        )
                    }
                },
        ) {
            FocusedImage(bitmap, focus, Modifier.fillMaxSize())
        }

        Spacer(Modifier.height(24.dp))

        // Anteprima dell'avatar rotondo
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, colors.outlineVariant, CircleShape),
            ) {
                FocusedImage(bitmap, focus, Modifier.fillMaxSize())
            }
            Spacer(Modifier.size(12.dp))
            Text(
                "Così apparirà anche nelle liste",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { focus = PhotoFocus() },
                modifier = Modifier.weight(1f),
            ) { Text("Ripristina") }
            Button(
                onClick = { onConfirm(focus) },
                modifier = Modifier.weight(1f),
            ) { Text("Conferma") }
        }
    }
}
