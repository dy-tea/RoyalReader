package app.royalreader.data.local.files

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores chapter paragraphs gzip-compressed on diesk.
 * One file per chapter: files/chapters/{novelID}/{chapterID}.txt.gz
 **/
@Singleton
class ChapterStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val chaptersDir: File get() = File(context.filesDir, "chapters")

    fun save(novelID: Long, chapterID: Long, paragraphs: List<String>) {
        val file = fileFor(novelID, chapterID)
        try {
            file.parentFile?.mkdirs()
            GZIPOutputStream(file.outputStream()).use { out ->
                writerTo(paragraphs, out)
            }
        } catch (ignored: IOException) {
            // reading can retry over the network.
        }
    }

    fun load(novelID: Long, chapterID: Long): List<String>? {
        val file = fileFor(novelID, chapterID)
        if (!file.exists()) return null
        return try {
            GZIPInputStream(file.inputStream()).use { input ->
                readerFrom(input)
            }
        } catch (ignored: IOException) {
            null
        }
    }

    fun deleteNovel(novelID: Long) {
        File(chaptersDir, novelID.toString()).deleteRecursively()
    }

    fun deleteAll() {
        chaptersDir.deleteRecursively()
    }

    private fun fileFor(novelID: Long, chapterID: Long) =
        File(chaptersDir, "$novelID/$chapterID.txt.gz")
}

internal fun writerTo(paragraphs: List<String>, out: java.io.OutputStream) {
    out.write(paragraphs.joinToString(SEPARATOR).toByteArray(Charsets.UTF_8))
}

internal fun readerFrom(input: java.io.InputStream): List<String> =
    input.readBytes().toString(Charsets.UTF_8).split(SEPARATOR).filter { it.isNotBlank() }

private const val SEPARATOR = "\n"
