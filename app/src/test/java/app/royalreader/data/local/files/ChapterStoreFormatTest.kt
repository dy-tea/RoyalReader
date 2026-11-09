package app.royalreader.data.local.files

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ChapterStoreFormatTest {

    @Test
    fun `chapter payload roundtrip preserves paragraphs`() {
        val paragraphs = listOf(
            "It was a dark and stormy night… the ale was *suspicious*.",
            "Second paragraph with unicode: é 卑弥呼 🙂",
            "",
        )
        val bytes = ByteArrayOutputStream().also { writerTo(paragraphs, it) }.toByteArray()

        val restored = readerFrom(ByteArrayInputStream(bytes))
        // Trailing empty paragraph is dropped by design (split filters blanks).
        assertEquals(paragraphs.take(2), restored)
    }
}
