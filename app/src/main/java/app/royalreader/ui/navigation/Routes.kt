package app.royalreader.ui.navigation

object Routes {
    const val LIBRARY = "library"
    const val DISCOVER = "discover"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    const val BOOK_DETAIL = "book/{novelID}"
    const val READER = "reader/{novelID}/{chapterID}"

    fun bookDetail(novelID: Long): String = "book/$novelID"
    fun reader(novelID: Long, chapterID: Long): String = "reader/$novelID/$chapterID"
}