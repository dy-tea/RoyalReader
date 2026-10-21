package app.royalreader.domain.model

/**
 * A chapter of a [Novel].
 * - [id] is the chapter id from (.../chapter/{id}/{slug})
 * - [url] is a relative path
 * - [index] is the position in the table of contents
 **/
data class Chapter(
    val id: Long,
    val novelID: Long,
    val title: String,
    val index: Int,
    val url: String,
    val publishedAtMs: Long? = null,
    val downloaded: Boolean = false,
    val read: Boolean = false
)