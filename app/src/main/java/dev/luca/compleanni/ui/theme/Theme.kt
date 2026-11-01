package dev.luca.compleanni.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun CompleanniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
