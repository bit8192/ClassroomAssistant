package cn.bincker.classroom.assistant.vm

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.bincker.classroom.assistant.data.entity.Course
import cn.bincker.classroom.assistant.data.entity.FileInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class RecordActivityViewModel : ViewModel(){
    private val _course = MutableStateFlow(Course(0, "", System.currentTimeMillis(), "新建课程", emptyList()))
    val course = _course.asStateFlow()

    val fileInfos = mutableStateListOf<FileInfoViewModel>()

    fun setTitle(title: String){
        _course.update {
            it.copy(title = title)
        }
    }

    fun addAudioRecord(context: Context){
        fileInfos.add(FileInfoViewModel(FileInfo(
            context.filesDir.toString() + File.separator + "audio" + File.separator + System.currentTimeMillis().toString(36) + ".wav",
            FileInfo.Companion.FileType.AUDIO,
            System.currentTimeMillis(),
            ""
        )))
    }

    fun deleteFile(index: Int){
        val file = fileInfos.removeAt(index)
        file.delete()
    }
}