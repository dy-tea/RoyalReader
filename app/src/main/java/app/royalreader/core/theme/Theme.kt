package app.royalreader.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TODO: improve these
private val LightColors = lightColorScheme(
    primary = Color(0xFF8B5E3C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF5E2D3),
    secondary = Color(0xFF1F2937),
    background = Color(0xFFFAF7F2),
    surface = Color(0xFFFAF7F2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0A96D),
    onPrimary = Color(0xFF2B1B0E),
    primaryContainer = Color(0xFF4A3320),
    secondary = Color(0xFFD1D5DB),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
)

@Composable
fun RoyalReaderTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}