package app.royalreader.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderDimmingTest {

    @Test
    fun `full brightness applies no overlay`() {
        assertEquals(0f, ReaderDimming.overlayAlpha(255))
        assertEquals(0f, ReaderDimming.overlayAlpha(300)) // clamped above range
    }

    @Test
    fun `darker environments dim more, capped at 70 percent`() {
        val half = ReaderDimming.overlayAlpha(128)
        val dark = ReaderDimming.overlayAlpha(0)
        assertTrue(half in 0.30f..0.40f)
        assertEquals(ReaderDimming.MIN_SURFACE, 1f - dark, 0.001f)
    }

    @Test
    fun `dimming increases monotonically as brightness falls`() {
        var previous = -1f
        for (brightness in 255 downTo 0 step 15) {
            val alpha = ReaderDimming.overlayAlpha(brightness)
            assertTrue(alpha >= previous)
            previous = alpha
        }
    }
}
