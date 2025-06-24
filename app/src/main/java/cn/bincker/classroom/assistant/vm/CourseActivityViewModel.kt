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
import kotlinx.coroutines.delay
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

    fun loadCourse(context: Context, id: Int, generatedTitle: Boolean) {
        val applicationContext = context.applicationContext as ClassroomAssistantApplication
        viewModelScope.launch(Dispatchers.IO) {
            loading.value = true
            try {
                var times = 0
                while (_course.value == null && times++ < 3){
                    Log.d("CourseActivityViewModel.loadCourse", "load course id: $id")
                    _course.value = applicationContext.db.courseDao().getCourseById(id)
                    delay(1000)
                }
                _course.value?.let { course->
                    Log.d("CourseActivityViewModel.loadCourse", "file.size=${course.files.size}")
                    if (course.content.isBlank()) {
                        val uploadApi = UploadApi()
                        val chatApi = BytedanceChatApi()
                        val imageUrls = course.files.filter { it.type == FileInfo.Companion.FileType.IMAGE }.map { uploadApi.uploadFile(context, it.path) }
                        val messageContents = imageUrls.filterNotNull().map {
                            ChatMessage.MessageContent(
                                type = "image",
                                imageUrl = ChatMessage.ImageUrl(it)
                            )
                        }.toMutableList()
                        messageContents += course.files.filter { it.type == FileInfo.Companion.FileType.AUDIO }
                            .mapIndexed { index, file ->
                                ChatMessage.MessageContent(
                                    text = "录音${index + 1} 时间：${
                                        DateFormat.getDateFormat(
                                            context
                                        ).format(Date(file.time))
                                    }: \n" + file.description
                                )
                            }
                        if (messageContents.isEmpty()){
                            Log.e("CourseActivityViewModel.loadCourse", "file content is empty. file.size=${course.files.size}")
                            return@let
                        }
                        val messages = listOf(
                            ChatMessage(content = listOf(ChatMessage.MessageContent(
                                text = "你是一个智能课堂AI助手，可根据用户提供的录音转文本内容、图片内容进行总结、分析、归纳、补充，你会考虑用户提供的文本可能有音译错误从而自行进行更正，结合图片内容进行分析（若用户提供的话）。" +
                                        "你总是输出中文内容，即便用户输入内容为其他语言。" +
                                        "你的输出为Markdown，但是有两点要求：1.第一行总是1级标题 2.第二行开始你会用引用（即'> '）进行1-2句话对总体的概括，其余可任意发挥。"
                            ))),
                            ChatMessage(
                                role = "user",
                                content = messageContents
                            )
                        )
                        chatApi.streamChat(messages) { response->
                            _reasoningContent.update { content-> content + response.choices.joinToString { it.delta?.reasoningContent ?: "" } }
                            _course.update { c->
                                c?.content += response.choices.joinToString { it.delta?.content ?: "" }
                                c
                            }
                        }
                        if (generatedTitle){
                            _course.update {
                                it?.title = it!!.content.lines().first().replace("# ", "")
                                it
                            }
                        }
                        _course.value?.summary =
                            _course.value?.content?.lines()?.filter { it.isBlank() }?.filterIndexed { index, s ->
                                if (index == 0 || index > 3) return@filterIndexed false
                                return@filterIndexed s.startsWith("> ")
                            }
                                ?.joinToString("") {
                                    it.replace("> ", "")
                                } ?: ""
                        applicationContext.db.courseDao().update(_course.value!!)
                    }
                }
            }finally {
                loading.value = false
            }
        }
    }

    fun setCourse(c: Course){
        _course.update { c }
    }

    fun setReasoningContent(content: String){
        _reasoningContent.value = content
    }
}