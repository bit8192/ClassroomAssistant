package cn.bincker.classroom.assistant.vm

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.bincker.classroom.assistant.ClassroomAssistantApplication
import cn.bincker.classroom.assistant.chat.BytedanceChatApi
import cn.bincker.classroom.assistant.chat.ChatMessage
import cn.bincker.classroom.assistant.data.entity.Course
import cn.bincker.classroom.assistant.data.entity.FileInfo
import cn.bincker.classroom.assistant.upload.UploadApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class CourseActivityViewModel: ViewModel() {
    private val _course = MutableStateFlow<Course?>(null)
    val course = _course.asStateFlow()
    val loading = mutableStateOf(false)
    private val _reasoningContent = MutableStateFlow("")
    val reasoningContent = _reasoningContent.asStateFlow()

    fun loadCourse(context: Context, id: Int) {
        val applicationContext = context.applicationContext as ClassroomAssistantApplication
        viewModelScope.launch(Dispatchers.IO) {
            loading.value = true
            try {
                _course.value = applicationContext.db.courseDao().getCourseById(id)
                _course.value?.let { c->
                    if (c.content.isBlank() == true) {
                        Log.d("CourseActivityViewModel.loadCourse", "start stream chat")
                        val uploadApi = UploadApi()
                        val chatApi = BytedanceChatApi()
                        val imageUrls = c.files.filter { it.type == FileInfo.Companion.FileType.IMAGE }.map { uploadApi.uploadFile(context, it.path) }
                        val messages = listOf<ChatMessage>(
                            ChatMessage(content = listOf(ChatMessage.MessageContent(text = "你是一个智能课堂AI助手，可根据用户提供的录音转文本内容、图片内容进行总结、分析、归纳、补充，你会考虑用户提供的文本可能有音译错误从而自行进行更正"))),
                            ChatMessage(
                                role = "user",
                                content =
                                    imageUrls.filterNotNull().map {
                                        ChatMessage.MessageContent(type = "image", imageUrl = ChatMessage.ImageUrl(it))
                                    } +
                                    c.files.filter { it.type == FileInfo.Companion.FileType.AUDIO }.mapIndexed { index,file->
                                        ChatMessage.MessageContent(text = "录音${index + 1} 时间：${DateFormat.getDateFormat(context).format(Date(file.time))}: \n" + file.description)
                                    }
                            )
                        )
                        chatApi.streamChat(messages) { response->
                            _reasoningContent.update { it + response.choices.joinToString { it.delta?.reasoningContent ?: "" } }
                            _course.update {
                                it?.content += response.choices.joinToString { it.delta?.content ?: "" }
                                it
                            }
                        }
                        applicationContext.db.courseDao().update(_course.value!!)
                    }
                }
            }finally {
                loading.value = false
            }
        }
    }
}