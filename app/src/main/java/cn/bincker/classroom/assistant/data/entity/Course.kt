package cn.bincker.classroom.assistant.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String,
    var createTime: Long,
    var summary: String,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    var files: List<FileInfo>,
    var content: String
)