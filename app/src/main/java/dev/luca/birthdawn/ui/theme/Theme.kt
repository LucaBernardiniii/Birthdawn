package dev.luca.birthdawn.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils

/** Colore d'accento predefinito dell'app (corallo). */
val AppAccent = Color(0xFFD9475A)

private val LightColors = lightColorScheme(
    primary = Color(0xFFD9475A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADC),
    onPrimaryContainer = Color(0xFF410010),
    secondary = Color(0xFF8C4A55),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF3A0714),
    tertiary = Color(0xFFB8741A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2BC),
    onTertiaryContainer = Color(0xFF2B1700),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF22191A),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF22191A),
    surfaceVariant = Color(0xFFF4DDDF),
    onSurfaceVariant = Color(0xFF524345),
    surfaceContainer = Color(0xFFFBEBEC),
    surfaceContainerHigh = Color(0xFFF6E4E5),
    outline = Color(0xFF857375),
    outlineVariant = Color(0xFFD7C1C3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2B8),
    onPrimary = Color(0xFF67001F),
    primaryContainer = Color(0xFF8E1B30),
    onPrimaryContainer = Color(0xFFFFDADC),
    secondary = Color(0xFFE5BDC2),
    onSecondary = Color(0xFF43292D),
    secondaryContainer = Color(0xFF5C3F43),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFFFFB960),
    onTertiary = Color(0xFF472A00),
    tertiaryContainer = Color(0xFF653E00),
    onTertiaryContainer = Color(0xFFFFE2BC),
    background = Color(0xFF1A1112),
    onBackground = Color(0xFFF0DEDF),
    surface = Color(0xFF1A1112),
    onSurface = Color(0xFFF0DEDF),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD7C1C3),
    surfaceContainer = Color(0xFF271D1E),
    surfaceContainerHigh = Color(0xFF322728),
    outline = Color(0xFFA08C8E),
    outlineVariant = Color(0xFF524345),
)

val supportsDynamicColor: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Genera una palette completa (stile Material You) partendo da un solo colore.
 * Usata per il colore personalizzato e per il colore preso dalla foto.
 */
fun schemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val h = hsl[0]
    // Colori quasi grigi restano neutri, gli altri vengono resi abbastanza saturi da essere leggibili
    val s = if (hsl[1] < 0.08f) 0.06f else hsl[1].coerceIn(0.40f, 0.85f)
    val th = h + 45f // tonalità terziaria, leggermente spostata

    fun c(hue: Float, sat: Float, light: Float) = Color(
        ColorUtils.HSLToColor(
            floatArrayOf(((hue % 360f) + 360f) % 360f, sat.coerceIn(0f, 1f), light.coerceIn(0f, 1f))
        )
    )
    val ns = minOf(s, 0.30f) // saturazione per le superfici

    return if (!dark) lightColorScheme(
        primary = c(h, s, 0.40f),
        onPrimary = Color.White,
        primaryContainer = c(h, s, 0.90f),
        onPrimaryContainer = c(h, s, 0.14f),
        secondary = c(h, s * 0.35f, 0.38f),
        onSecondary = Color.White,
        secondaryContainer = c(h, s * 0.45f, 0.89f),
        onSecondaryContainer = c(h, s * 0.4f, 0.13f),
        tertiary = c(th, s * 0.8f, 0.40f),
        onTertiary = Color.White,
        tertiaryContainer = c(th, s * 0.8f, 0.88f),
        onTertiaryContainer = c(th, s, 0.14f),
        background = c(h, ns, 0.98f),
        onBackground = c(h, 0.10f, 0.11f),
        surface = c(h, ns, 0.98f),
        onSurface = c(h, 0.10f, 0.11f),
        surfaceVariant = c(h, ns * 0.8f, 0.89f),
        onSurfaceVariant = c(h, 0.10f, 0.30f),
        surfaceContainer = c(h, ns, 0.94f),
        surfaceContainerHigh = c(h, ns, 0.91f),
        outline = c(h, 0.08f, 0.50f),
        outlineVariant = c(h, 0.12f, 0.80f),
    ) else darkColorScheme(
        primary = c(h, s, 0.78f),
        onPrimary = c(h, s, 0.17f),
        primaryContainer = c(h, s * 0.9f, 0.30f),
        onPrimaryContainer = c(h, s, 0.90f),
        secondary = c(h, s * 0.35f, 0.78f),
        onSecondary = c(h, s * 0.35f, 0.18f),
        secondaryContainer = c(h, s * 0.3f, 0.28f),
        onSecondaryContainer = c(h, s * 0.4f, 0.90f),
        tertiary = c(th, s * 0.8f, 0.76f),
        onTertiary = c(th, s, 0.18f),
        tertiaryContainer = c(th, s * 0.7f, 0.30f),
        onTertiaryContainer = c(th, s, 0.90f),
        background = c(h, ns * 0.6f, 0.07f),
        onBackground = c(h, 0.15f, 0.90f),
        surface = c(h, ns * 0.6f, 0.07f),
        onSurface = c(h, 0.15f, 0.90f),
        surfaceVariant = c(h, 0.12f, 0.28f),
        onSurfaceVariant = c(h, 0.12f, 0.80f),
        surfaceContainer = c(h, ns * 0.6f, 0.12f),
        surfaceContainerHigh = c(h, ns * 0.6f, 0.16f),
        outline = c(h, 0.08f, 0.60f),
        outlineVariant = c(h, 0.10f, 0.30f),
    )
}

/** true se l'app sta usando il tema scuro (tenendo conto della scelta nelle impostazioni). */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun rememberDarkTheme(): Boolean {
    val appearance by AppearanceSettings.state.collectAsState()
    val system = isSystemInDarkTheme()
    return when (appearance.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> system
    }
}

@Composable
fun BirthdawnTheme(dark: Boolean, content: @Composable () -> Unit) {
    val appearance by AppearanceSettings.state.collectAsState()
    val context = LocalContext.current
    val scheme = remember(appearance, dark) {
        when {
            appearance.mode == AccentMode.SYSTEM && supportsDynamicColor ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            appearance.mode == AccentMode.CUSTOM ->
                schemeFromSeed(Color(appearance.customColor), dark)
            else -> if (dark) DarkColors else LightColors
        }
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
