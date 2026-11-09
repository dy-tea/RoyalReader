package app.royalreader.data.remote

import app.royalreader.domain.model.Chapter
import app.royalreader.domain.model.FictionDetails
import app.royalreader.domain.model.Novel
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parses RoyalRoad HTML into domain models.
 *
 * RoyalRoad's markup is not versioned, so every selection uses several
 * fallbacks and never throws: unsupported layouts simply yield partial data.
 **/
object RoyalRoadParser {
    private val fictionIDRegex = Regex("""/fiction/(\d+)(?:/.*)?""")
    private val chapterIDRegex = Regex("""/fiction/\d+/(?:[^/]+/)?chapter/(\d+)(?:/.*)?""")
    private val dateRegex = Regex("""(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}(?::\d{2})?)""")

    fun parseFictionList(document: Document): List<Novel> {
        val items = document.select("div.fiction-list-item")
        if (items.isNotEmpty()) return items.mapNotNull { parseFictionCard(it) }
        // Fallback: use every fiction title link, enriched with its card if one exists.
        val anchors = document.select("a.fiction-title, a[href*='/fiction/']")
        return anchors.mapNotNull { anchor ->
            closestCard(anchor)?.let { parseFictionCard(it) } ?: parseNovelFromAnchor(anchor)
        }
    }

    private fun parseNovelFromAnchor(anchor: Element): Novel? {
        val id = fictionIDRegex.find(anchor.attr("href"))?.groupValues?.get(1)?.toLongOrNull()
            ?: return null
        return Novel(
            id = id,
            title = anchor.text().ifBlank { anchor.attr("title") },
        )
    }

    private fun closestCard(element: Element): Element? =
        element.parents().firstOrNull { it.hasClass("fiction-list-item") || it.hasClass("fiction-card") }

    fun parseFictionCard(card: Element): Novel? {
        val titleAnchor = card.selectFirst("a.fiction-title, a[href*='/fiction/']") ?: return null
        val id = fictionIDRegex.find(titleAnchor.attr("href"))?.groupValues?.get(1)?.toLongOrNull()
            ?: return null
        return Novel(
            id = id,
            title = titleAnchor.text(),
            author = card.selectFirst(".author-name, .author")?.text().orEmpty(),
            coverURL = parseImageURL(card),
            description = card.selectFirst(".fiction-list-item-body")?.text().orEmpty(),
            rating = parseRating(card),
        )
    }

    fun parseFiction(document: Document, novelID: Long): FictionDetails {
        val novel = Novel(
            id = novelID,
            title = document.selectFirst("h1.fiction-title")?.text()
                ?: document.selectFirst("h1")?.text()
                ?: "",
            author = document.selectFirst("a.fiction-author")?.text()
                ?: document.selectFirst("a[href*='/user/profile']")?.text()
                ?: "",
            coverURL = parseImageURL(document),
            description = document.selectFirst("div.description")?.wholeText()?.trim().orEmpty(),
            tags = document.select("span.tags a.genre, a.genre").map { it.text() }
                .filter { it.isNotBlank() },
            rating = parseRating(document),
        )
        return FictionDetails(novel = novel, chapters = parseChapters(document, novelID))
    }

    fun parseChapters(document: Document, novelID: Long): List<Chapter> {
        val rows = document.select("table#chapter-table tbody tr").ifEmpty {
            document.select("#chapters tr")
        }
        val chapters = mutableListOf<Chapter>()
        for (row in rows) {
            val anchor = row.selectFirst("a[href*='/chapter/']") ?: continue
            val id = chapterIDRegex.find(anchor.attr("href"))?.groupValues?.get(1)?.toLongOrNull()
                ?: continue
            chapters += Chapter(
                id = id,
                novelID = novelID,
                title = anchor.text().ifBlank { anchor.attr("title") },
                index = chapters.size,
                url = anchor.attr("href"),
                publishedAtMs = parseTimestamp(row),
            )
        }
        return chapters
    }

    /**
     * Result of parsing a chapter page: full text and neighbours for navigation.
     **/
    data class ChapterContent(
        val title: String,
        val paragraphs: List<String>,
        val nextChapterID: Long?,
        val previousChapterID: Long?,
    )

    fun parseChapter(document: Document): ChapterContent {
        val title = document.selectFirst("h1.font-white")?.text()
            ?: document.selectFirst("h1")?.text()
            ?: ""
        return ChapterContent(
            title = title,
            paragraphs = parseParagraphs(document),
            nextChapterID = findNeighborChapter(document, "next"),
            previousChapterID = findNeighborChapter(document, "prev|previous"),
        )
    }

    private fun parseParagraphs(document: Document): List<String> {
        val container = document.selectFirst("div.chapter-inner")
            ?: document.selectFirst("div#chapter-container")
            ?: document.selectFirst("div.rr-composer-blurb")
            ?: return document.select("p").map { it.text() }.filter { it.isNotBlank() }
        val paragraphs = container.select("p").map { it.text() }.filter { it.isNotBlank() }
        if (paragraphs.isNotEmpty()) return paragraphs
        // Unusual markup: fall back to the text of every direct block child.
        return container.children().map { it.text() }.filter { it.isNotBlank() }
    }

    private fun findNeighborChapter(document: Document, keyword: String): Long? {
        val regex = Regex("""(?i).*(?:$keyword).*chapter.*""")
        val anchor = document.select("a[href*='/chapter/']").firstOrNull { a ->
            a.text().matches(regex) || a.attr("title").matches(regex)
        } ?: return null
        return chapterIDRegex.find(anchor.attr("href"))?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun parseImageURL(root: Element): String? {
        val image = root.selectFirst("img.fiction-image, .cover-container img") ?: return null
        val url = image.absUrl("src").ifBlank { image.attr("data-src") }.ifBlank { image.attr("src") }
        return url.ifBlank { null }
    }

    /**
     * Star ratings look like "4.7" inside elements with a star-rating-ish marker.
     **/
    private fun parseRating(root: Element): Float? {
        val candidates = root.select("span.star-rating, [data-name='rating'], li.rating").toMutableList()
        root.select("[title]").forEach { candidates.add(it) }
        for (element in candidates) {
            val text = element.text() + " " + element.attr("title") + " " + element.attr("data-original-title")
            val match = Regex("""(\d(?:\.\d{1,2})?)\s*(?:stars?|average)?""").find(text.take(120))
            val value = match?.groupValues?.get(1)?.toFloatOrNull()
            // Accept even a bare "4.5" from star widgets, reject >5.
            if (value != null && value >= 0.5f && value <= 5f) return value
        }
        return null
    }

    /**
     * RoyalRoad renders <time title="2022-04-09T01:08:58" datetime="...">,
     * sometimes without an explicit zone: interpret missing zone as UTC.
     **/
    fun parseTimestamp(row: Element): Long? {
        val element = row.selectFirst("time") ?: return null
        val raw = listOf(element.attr("title"), element.attr("datetime"), element.text())
            .first { it.isNotBlank() }
        return parseTimestampString(raw)
    }

    fun parseTimestampString(raw: String): Long? {
        val match = dateRegex.find(raw) ?: return null
        val date = match.groupValues[1]
        val time = match.groupValues[2]
        return try {
            Instant.parse("${date}T$time${if (time.length == 5) ":00" else ""}Z").toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(
                    "${date}T${if (time.length == 5) "$time:00" else time}",
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                ).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
