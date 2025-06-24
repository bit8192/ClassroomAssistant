package cn.bincker.classroom.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import cn.bincker.classroom.assistant.data.entity.Course
import cn.bincker.classroom.assistant.ui.theme.ClassroomAssistantTheme
import cn.bincker.classroom.assistant.vm.CourseActivityViewModel

class CourseActivity : ComponentActivity() {
    private val vm by viewModels<CourseActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassroomAssistantTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding->
                    CourseApp(modifier = Modifier.padding(innerPadding), vm)
                }
            }
        }
        vm.loadCourse(
            this,
            intent.getIntExtra("id", -1),
            intent.getBooleanExtra("generatedTitle", false)
        )
    }
}

@Composable
fun CourseApp(modifier: Modifier = Modifier, vm: CourseActivityViewModel){
    val scrollState = rememberScrollState()
    val reasoningContent by vm.reasoningContent.collectAsState()
    val course by vm.course.collectAsState()
    Column(modifier = modifier.fillMaxSize().verticalScroll(scrollState)) {
        if (vm.loading.value) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(reasoningContent, color = colorResource(R.color.text_log))
        Text(course?.content ?: "")
    }
}

@Preview
@Composable
fun Preview(){
    val vm = CourseActivityViewModel()
    vm.setCourse(Course(0, "title", System.currentTimeMillis(), "summary", emptyList(), "content"))
    vm.setReasoningContent("ReasoningContent")
    CourseApp(vm = vm)
}