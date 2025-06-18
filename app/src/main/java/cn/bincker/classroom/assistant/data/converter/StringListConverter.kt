package cn.bincker.classroom.assistant.data.converter

import androidx.room.TypeConverter
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class StringListConverter {
    companion object {
        @TypeConverter
        fun toString(list: List<String>) =
            list.joinToString(",") { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }

        @TypeConverter
        fun toList(str: String) =
            str.split(",").map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                .toList()
    }
}