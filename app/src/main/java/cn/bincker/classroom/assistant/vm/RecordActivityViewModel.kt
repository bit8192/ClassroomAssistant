package cn.bincker.classroom.assistant.vm

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.bincker.classroom.assistant.data.entity.Course
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecordActivityViewModel : ViewModel(){
    private val _course = MutableStateFlow<Course>(Course(0, "", System.currentTimeMillis(), "新建课程"))
    val course = _course.asStateFlow()

    val textContent = mutableStateListOf<String>()

    private var recordedTime = 0L
    var isStart = mutableStateOf(false)
    private var startTime = 0L
    private val _recordTime = MutableStateFlow<Long>(0)
    val recordTimeText = _recordTime.asStateFlow().map {
        val time = it + recordedTime;
        val h = time / 3600_000L
        val m = time / 60_000 % 60
        val s = time / 1000 % 60
        var stringBuilder = StringBuilder()
        if (h < 10) stringBuilder.append("0")
        stringBuilder.append(h).append(":")
        if (m < 10) stringBuilder.append("0")
        stringBuilder.append(m).append(":")
        if (s < 10) stringBuilder.append("0")
        stringBuilder.append(s)
        stringBuilder.toString()
    }

    fun addContent(line: String){
        viewModelScope.launch {
            textContent.add(line)
        }
    }

    fun start(){
        isStart.value = true
        viewModelScope.launch {
            startTime = System.currentTimeMillis()
            while (isStart.value) {
                _recordTime.value = System.currentTimeMillis() - startTime
                delay(1000)
            }
        }
    }

    fun stop() {
        isStart.value = false
        viewModelScope.launch {
            recordedTime += System.currentTimeMillis() - startTime
            _recordTime.value = 0
        }
    }

    fun setTitle(title: String){
        _course.update {
            it.copy(title = title)
        }
    }
}