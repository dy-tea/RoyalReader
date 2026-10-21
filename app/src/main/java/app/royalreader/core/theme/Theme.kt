package app.royalreader.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TODO: improve these
private val LightColors = lightColorScheme(
    primary = Color(0xFFE5D9B6),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFFF5D996),
    secondary = Color(0xFFE5D9B6),
    background = Color(0xFFE5D9B6),
    surface = Color(0xFFE5D9B6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF1E201E),
    onPrimary = Color(0xFF000000),
    onPrimaryContainer = Color(0xFF2E2002),
    secondary = Color(0xFF3C3D37),
    background = Color(0xFF697565),
    surface = Color(0xFFECDFCC)
)

@Composable
fun RoyalReaderTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}