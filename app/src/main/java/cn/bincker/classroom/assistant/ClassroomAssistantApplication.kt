package cn.bincker.classroom.assistant

import android.app.Application
import androidx.room.Room
import cn.bincker.classroom.assistant.data.database.AppDatabase

class ClassroomAssistantApplication: Application() {
    val db: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java, "course-database"
        ).build()
    }
}