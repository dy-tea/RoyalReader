package app.royalreader.data.remote

import app.royalreader.domain.model.FictionDetails
import app.royalreader.domain.model.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Fetches and parses pages from royalroad.com.
 **/
class RoyalRoadClient(
    private val client: OkHttpClient,
    private val base: String = BASE_URL,
) {
    suspend fun popular(): List<Novel> = fetch("$base/fictions/popular") { RoyalRoadParser.parseFictionList(it) }

    suspend fun latest(): List<Novel> = fetch("$base/fictions/updated") { RoyalRoadParser.parseFictionList(it) }

    suspend fun search(query: String): List<Novel> =
        fetch("$base/search?title=${query.replace(' ', '+')}") { RoyalRoadParser.parseFictionList(it) }

    suspend fun getFiction(novelID: Long): FictionDetails =
        fetch("$base/fiction/$novelID") { RoyalRoadParser.parseFiction(it, novelID) }

    suspend fun getChapter(url: String): RoyalRoadParser.ChapterContent =
        fetch(resolve(url)) { RoyalRoadParser.parseChapter(it) }

    suspend fun fetchDocument(url: String): Document = withContext(Dispatchers.IO) {
        val response = client.newCall(Request.Builder().url(resolve(url)).build()).execute()
        response.use { body ->
            if (!body.isSuccessful) throw IOException("HTTP ${body.code} fetching $url")
            Jsoup.parse(body.body!!.byteStream(), Charsets.UTF_8.name(), resolve(url))
        }
    }

    private suspend fun <T> fetch(url: String, parse: (Document) -> T): T = parse(fetchDocument(url))

    private fun resolve(url: String): String =
        if (url.startsWith("http")) url else base + if (url.startsWith("/")) url else "/$url"

    companion object {
        const val BASE_URL = "https://www.royalroad.com"
    }
}
