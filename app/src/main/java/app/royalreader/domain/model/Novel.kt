package app.royalreader.domain.model

/**
 * A RoyalRoad fiction.
 * - [id] is the site's fiction id (/fiction/{id}/{slug})
 * - [inLibrary] is true when the user has added the fiction to their local library
 **/
data class Novel(
    val id: Long,
    val title: String,
    val author: String = "",
    val coverURL: String? = null,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val rating: Float? = null,
    val inLibrary: Boolean = false
)

/**
 * Result of parsing a fiction page.
 **/
data class FictionDetails(
    val novel: Novel,
    val chapters: List<Chapter>,
)