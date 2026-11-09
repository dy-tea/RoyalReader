package app.royalreader.data.local.db

import androidx.room.TypeConverter

private const val TAG_SEPARATOR = "\u001F"

class TagListConverter {
    @TypeConverter
    fun fromTags(tags: List<String>): String = tags.joinToString(TAG_SEPARATOR)

    @TypeConverter
    fun toTags(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(TAG_SEPARATOR)
}
