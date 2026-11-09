package app.royalreader.data.remote

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoyalRoadParserTest {

    private fun parse(html: String) = Jsoup.parse(html, RoyalRoadClient.BASE_URL)

    @Test
    fun `fiction list card parses id title cover and description`() {
        val doc = parse(
            """
            <html><body>
              <div class="fiction-list-item">
                <img class="fiction-image" src="/images/cover_small.jpg">
                <a class="fiction-title" href="/fiction/84603/the-innkeeper-foundation">The Innkeeper Foundation</a>
                <div class="fiction-list-item-body"><p>An innkeeper mystery adventure.</p></div>
              </div>
            </body></html>
            """.trimIndent()
        )
        val novels = RoyalRoadParser.parseFictionList(doc)
        assertEquals(1, novels.size)
        val novel = novels.first()
        assertEquals(84603L, novel.id)
        assertEquals("The Innkeeper Foundation", novel.title)
        assertEquals("An innkeeper mystery adventure.", novel.description)
        assertTrue(novel.coverURL?.endsWith("/images/cover_small.jpg") == true)
        assertNull(novel.rating)
    }

    @Test
    fun `fiction list fallback uses anchors when cards are missing`() {
        val doc = parse(
            """
            <html><body>
              <a class="fiction-title" href="/fiction/42/great">Great Fiction</a>
            </body></html>
            """.trimIndent()
        )
        val novels = RoyalRoadParser.parseFictionList(doc)
        assertEquals(1, novels.size)
        assertEquals("Great Fiction", novels.first().title)
    }

    @Test
    fun `fiction page parses novel details and chapter table`() {
        val doc = parse(
            """
            <html><body>
              <h1 class="fiction-title">Deep Dungeon Deep</h1>
              <a class="fiction-author" href="/user/profile/77">Jane Writer</a>
              <img class="fiction-image" src="https://www.royalroad.com/images/cover_large.jpg">
              <div class="description"><p>A dungeon story.</p></div>
              <span class="tags"><a class="genre">Fantasy</a><a class="genre">Adventure</a></span>
              <table id="chapter-table"><tbody>
                <tr>
                  <td><a href="/fiction/555/deep-dungeon/chapter/9001/001-start">Chapter 1</a></td>
                  <td><time title="2022-04-09T01:08:58Z" datetime="2022-04-09T01:08:58Z">Apr 9</time></td>
                </tr>
                <tr>
                  <td><a href="/fiction/555/deep-dungeon/chapter/9002/002-deeper">Chapter 2</a></td>
                  <td><time title="2022-04-10T01:08:58Z" datetime="2022-04-10T01:08:58Z">Apr 10</time></td>
                </tr>
              </tbody></table>
            </body></html>
            """.trimIndent()
        )
        val details = RoyalRoadParser.parseFiction(doc, novelID = 555L)
        assertEquals("Deep Dungeon Deep", details.novel.title)
        assertEquals("Jane Writer", details.novel.author)
        assertEquals(listOf("Fantasy", "Adventure"), details.novel.tags)
        assertEquals(2, details.chapters.size)
        val first = details.chapters.first()
        assertEquals(9001L, first.id)
        assertEquals(555L, first.novelID)
        assertEquals(0, first.index)
        assertEquals("/fiction/555/deep-dungeon/chapter/9001/001-start", first.url)
        assertNotNull(first.publishedAtMs)
        assertEquals(1, details.chapters[1].index)
    }

    @Test
    fun `chapter page parses title paragraphs and neighbours`() {
        val doc = parse(
            """
            <html><body>
              <h1 class="font-white">001 – Anchor</h1>
              <div class="chapter-inner">
                <p>It was a dark and stormy tavern.</p>
                <p>Bob poured a suspicious ale.</p>
              </div>
              <a class="prev" href="/fiction/555/deep/chapter/9001/001-start" title="Prev Chapter">Prev Chapter</a>
              <a class="next" href="/fiction/555/deep/chapter/9003/003-next" title="Next Chapter">Next Chapter</a>
            </body></html>
            """.trimIndent()
        )
        val content = RoyalRoadParser.parseChapter(doc)
        assertEquals("001 – Anchor", content.title)
        assertEquals(listOf("It was a dark and stormy tavern.", "Bob poured a suspicious ale."), content.paragraphs)
        assertEquals(9003L, content.nextChapterID)
        assertEquals(9001L, content.previousChapterID)
    }

    @Test
    fun `chapter page without container falls back to plain paragraphs`() {
        val doc = parse(
            """
            <html><body>
              <h1>Title only</h1>
              <p>Paragraph one.</p>
              <p>Paragraph two.</p>
            </body></html>
            """.trimIndent()
        )
        val content = RoyalRoadParser.parseChapter(doc)
        assertEquals(listOf("Paragraph one.", "Paragraph two."), content.paragraphs)
    }

    @Test
    fun `timestamps parse from RR title attribute forms`() {
        assertEquals(
            RoyalRoadParser.parseTimestampString("2022-04-09T01:08:58Z"),
            RoyalRoadParser.parseTimestampString("2022-04-09 01:08:58"),
        )
        assertNull(RoyalRoadParser.parseTimestampString("garbage"))
    }
}
