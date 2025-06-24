package cn.bincker.classroom.assistant.vm

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.bincker.classroom.assistant.ClassroomAssistantApplication
import cn.bincker.classroom.assistant.CourseActivity
import cn.bincker.classroom.assistant.RecordActivity
import cn.bincker.classroom.assistant.data.entity.Course
import cn.bincker.classroom.assistant.data.entity.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RecordActivityViewModel : ViewModel(){
    private val _course = MutableStateFlow(Course(null, "", System.currentTimeMillis(), "新建课程", emptyList(), ""))
    val course = _course.asStateFlow()

    val fileInfos = mutableStateListOf<FileInfoViewModel>()

    val pagerState = PagerState { fileInfos.size }

    val generateTitle = mutableStateOf(false)

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

    fun addImage(imageUri: Uri) {
        fileInfos.add(FileInfoViewModel(FileInfo(
            imageUri.toString(),
            FileInfo.Companion.FileType.IMAGE,
            System.currentTimeMillis(),
            ""
        )))
    }

    fun addAudio(context: Context, audioUri: Uri) {
        viewModelScope.launch {
            val model = FileInfoViewModel(FileInfo(
                audioUri.toString(),
                FileInfo.Companion.FileType.AUDIO,
                System.currentTimeMillis(),
                ""
            ))
            fileInfos.add(model)
            model.asr(context, audioUri)
        }
    }

    fun complete(context: Context) {
        val applicationContext = context.applicationContext as ClassroomAssistantApplication
        viewModelScope.launch(Dispatchers.IO) {
            _course.value.files = fileInfos.map { it.getFileInfo() }
            val id = applicationContext.db.courseDao().insertCourse(_course.value)
            _course.update { course->
                course.id = id.toInt()
                course
            }
            withContext(Dispatchers.Main){
                (context as RecordActivity).also {
                    it.startActivity(
                        Intent(context, CourseActivity::class.java)
                            .also { intent ->
                                intent.putExtra("id", course.value.id)
                                intent.putExtra("generateTitle", generateTitle.value || course.value.title.isBlank())
                            }
                    )
                    it.finish()
                }
            }
        }
    }
}