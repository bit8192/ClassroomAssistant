package cn.bincker.classroom.assistant.data.converter

import androidx.room.TypeConverter
import cn.bincker.classroom.assistant.data.entity.FileInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FileInfoListConverter {
    @TypeConverter
    fun toString(data: List<FileInfo>?): String? = if(data == null) "[]" else Gson().toJson(data)

    @TypeConverter
    fun toFileInfoList(str: String?): List<FileInfo> {
        if (str.isNullOrBlank()) return emptyList()
        val typeToken: TypeToken<List<FileInfo>> =
            TypeToken.getParameterized(ArrayList::class.java, FileInfo::class.java) as TypeToken<List<FileInfo>>
        return Gson().fromJson(str, typeToken)
    }
}