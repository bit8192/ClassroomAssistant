package cn.bincker.classroom.assistant.vm

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.bincker.classroom.assistant.data.dao.CourseDao
import cn.bincker.classroom.assistant.data.entity.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel(private val courseDao: CourseDao) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val viewPermissionDeniedDialog = mutableStateOf(false)
    val deniedPermissionContent = mutableStateOf("")

    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            _courses.value = courseDao.getAllCourses()
            _isLoading.value = false
        }
    }
}