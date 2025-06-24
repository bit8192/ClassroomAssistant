package cn.bincker.classroom.assistant.vm

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.bincker.classroom.assistant.ClassroomAssistantApplication
import cn.bincker.classroom.assistant.data.entity.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel() : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val viewPermissionDeniedDialog = mutableStateOf(false)
    val deniedPermissionContent = mutableStateOf("")

    fun loadCourses(context: Context) {
        val applicationContext = context.applicationContext as ClassroomAssistantApplication
        viewModelScope.launch {
            _isLoading.value = true
            _courses.value = applicationContext.db.courseDao().getAllCourses()
            for (i in _courses.value){
                Log.d("CourseViewModel.loadCourses", "id=${i.id}")
            }
            _isLoading.value = false
        }
    }

    fun deleteCourse(context: Context, course: Course) {
        val applicationContext = context.applicationContext as ClassroomAssistantApplication
        viewModelScope.launch {
            applicationContext.db.courseDao().delete(course)
        }
        loadCourses(context)
    }
}