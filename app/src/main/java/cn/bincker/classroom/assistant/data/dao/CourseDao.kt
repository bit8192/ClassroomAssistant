package cn.bincker.classroom.assistant.data.dao// CourseDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.bincker.classroom.assistant.data.entity.Course

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY createTime DESC")
    suspend fun getAllCourses(): List<Course>
    
    @Insert
    suspend fun insertCourse(course: Course)
}