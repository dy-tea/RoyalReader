package app.royalreader.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Palettes for the reading surface.
 **/
data class ReaderPalette (
    val id: String,
    val label: String,
    val background: Color,
    val text: Color,
    val accent: Color
)

object ReaderPalettes {
    val BlackOnWhite = ReaderPalette(
        id = "black_on_white",
        label = "Black on white",
        background = Color(0xFFFFFFFF),
        text = Color(0xFF1A1A1A),
        accent = Color(0xFF1A73E8),
    )

    val WhiteOnBlack = ReaderPalette(
        id = "white_on_black",
        label = "White on black",
        background = Color(0xFF101010),
        text = Color(0xFFE0E0E0),
        accent = Color(0xFFF5A623),
    )

    val Sepia = ReaderPalette(
        id = "sepia",
        label = "Sepia",
        background = Color(0xFFF4E8D0),
        text = Color(0xFF4A3B2A),
        accent = Color(0xFF8B5E3C)
    )

    val GrayOnDark = ReaderPalette(
        id = "gray_on_dark",
        label = "Gray on dark",
        background = Color(0xFF1E1E22),
        text = Color(0xFFC8C8CC),
        accent = Color(0xFF7BA7D7)
    )

    val presets: List<ReaderPalette> = listOf(BlackOnWhite, WhiteOnBlack, Sepia, GrayOnDark)

    fun byID(id: String?): ReaderPalette = presets.firstOrNull { it.id == id } ?: BlackOnWhite
}