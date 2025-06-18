package cn.bincker.classroom.assistant.data.database// AppDatabase.kt
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cn.bincker.classroom.assistant.data.converter.FileInfoListConverter
import cn.bincker.classroom.assistant.data.converter.StringListConverter
import cn.bincker.classroom.assistant.data.dao.CourseDao
import cn.bincker.classroom.assistant.data.entity.Course

@Database(entities = [Course::class], version = 1)
@TypeConverters(FileInfoListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
}