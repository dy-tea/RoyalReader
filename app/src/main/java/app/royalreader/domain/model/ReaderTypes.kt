package app.royalreader.domain.model

import androidx.annotation.Px

/**
 * Reading progress of a given [Novel].
 **/
data class ReadingProgress(
    val novelID: Long,
    val chapterID: Long,
    val paragraphAnchor: Int,
    val updatedAtMs: Long = 0L,
)

/**
 * Per-novel reader settings.
 * - [paletteID] corresponds to a [app.royalreader.core.theme.ReaderPalette]
 **/
data class ReaderSettings(
    val novelID: Long,
    val paletteID: String = DEFAULT_PALETTE_ID,
    val fontSizePx: Int = 18,
    val lineHeight: Float = 1.6f,
) {
    // https://kotlinlang.org/docs/object-declarations.html#companion-objects
    companion object {
        const val DEFAULT_PALETTE_ID = "black_on_white"
    }
}