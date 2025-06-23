package cn.bincker.classroom.assistant.data.dao// CourseDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.bincker.classroom.assistant.data.entity.Course

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY createTime DESC")
    suspend fun getAllCourses(): List<Course>

    @Insert
    suspend fun insertCourse(course: Course)

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Int): Course?

    @Update(entity = Course::class)
    suspend fun update(vararg course: Course)
}