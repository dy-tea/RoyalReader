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
    fun `fiction list card parses id title cover description tags and rating`() {
        val doc = parse(
            """
            <html><body>
              <div class="fiction-list-item row">
                <figure class="col-sm-2">
                  <a href="/fiction/84603/the-innkeeper-foundation">
                    <img class="img-responsive" data-type="cover" src="/covers-full/84603-cover-small.webp">
                  </a>
                </figure>
                <div class="col-sm-10">
                  <h2 class="fiction-title">
                    <a href="/fiction/84603/the-innkeeper-foundation">The Innkeeper Foundation</a>
                  </h2>
                  <span class="tags">
                    <a class="fiction-tag" href="/fictions/search?tagsAdd=fantasy">Fantasy</a>
                    <a class="fiction-tag" href="/fictions/search?tagsAdd=adventure">Adventure</a>
                  </span>
                  <div class="col-sm-6 uppercase bold font-blue-dark" aria-label="Rating: 4.81 out of 5">
                    <span class="star" title="4.81"></span>
                  </div>
                  <div id="description-84603" class="margin-top-10 col-xs-12">
                    <p>An innkeeper mystery adventure.</p>
                  </div>
                </div>
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
        assertEquals(listOf("Fantasy", "Adventure"), novel.tags)
        assertEquals(4.81f, novel.rating)
        assertTrue(novel.coverURL?.endsWith("/covers-full/84603-cover-small.webp") == true)
    }

    @Test
    fun `fiction list fallback uses anchors when cards are missing`() {
        val doc = parse(
            """
            <html><body>
              <figure><a href="/fiction/42/cover-only"><img src="/cover.jpg"></a></figure>
              <a class="fiction-title" href="/fiction/42/cover-only">Great Fiction</a>
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
              <h1 class="font-white">Deep Dungeon Deep</h1>
              <div class="fic-title">
                <h4><span>by </span><a href="/profile/77">Jane Writer</a></h4>
              </div>
              <div class="cover-art-container">
                <img class="thumbnail inline-block" src="https://www.royalroadcdn.com/public/covers-full/555-cover.jpg">
              </div>
              <div class="description">
                <div class="hidden-content"><p>A dungeon story.</p></div>
              </div>
              <span class="tags"><a class="fiction-tag">Fantasy</a><a class="fiction-tag">Adventure</a></span>
              <script type="application/ld+json">
                {"@type":"Book","aggregateRating":{"@type":"AggregateRating","bestRating":5,"ratingValue":4.83},"author":{"name":"Jane Writer"}}
              </script>
              <table class="table no-border" id="chapters" data-chapters="2">
                <tr>
                  <td><a href="/fiction/555/deep-dungeon/chapter/9001/001-start">Chapter 1</a></td>
                  <td><time unixtime="1649478538" datetime="2022-04-09T01:08:58.0000000Z">Apr 9</time></td>
                </tr>
                <tr>
                  <td><a href="/fiction/555/deep-dungeon/chapter/9002/002-deeper">Chapter 2</a></td>
                  <td><time title="2022-04-10T01:08:58Z" datetime="2022-04-10T01:08:58Z">Apr 10</time></td>
                </tr>
              </table>
            </body></html>
            """.trimIndent()
        )
        val details = RoyalRoadParser.parseFiction(doc, novelID = 555L)
        assertEquals("Deep Dungeon Deep", details.novel.title)
        assertEquals("Jane Writer", details.novel.author)
        assertEquals(listOf("Fantasy", "Adventure"), details.novel.tags)
        assertEquals(4.83f, details.novel.rating)
        assertTrue(details.novel.coverURL?.endsWith("/covers-full/555-cover.jpg") == true)
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
              <div class="chapter-inner chapter-content">
                <p>It was a dark and stormy tavern.</p>
                <p>Bob poured a suspicious ale.</p>
              </div>
              <a href="/fiction/555/deep/chapter/9001/001-start">Previous Chapter</a>
              <a href="/fiction/555/deep/chapter/9003/003-next">Next Chapter</a>
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
    fun `chapter page prefers link rel next and prev`() {
        val doc = parse(
            """
            <html><head>
              <link rel="next" href="/fiction/555/deep/chapter/9003/003-next">
              <link rel="prev" href="/fiction/555/deep/chapter/9001/001-start">
            </head><body>
              <h1>Body only</h1>
            </body></html>
            """.trimIndent()
        )
        val content = RoyalRoadParser.parseChapter(doc)
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
