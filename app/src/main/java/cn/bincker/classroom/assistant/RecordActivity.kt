package cn.bincker.classroom.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.bincker.classroom.assistant.ui.theme.ClassroomAssistantTheme
import cn.bincker.classroom.assistant.vm.RecordActivityViewModel

class RecordActivity : ComponentActivity() {
    private val vm: RecordActivityViewModel by viewModels<RecordActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassroomAssistantTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {TopBar(viewModel = vm)}) { innerPadding ->
                    RecordApp(modifier = Modifier.padding(innerPadding), vm)
                }
            }
        }
    }
}

@Composable
fun TopBar(modifier: Modifier = Modifier, viewModel: RecordActivityViewModel) {
    val course by viewModel.course.collectAsState()
    Box(modifier = modifier.fillMaxWidth().height(65.dp), contentAlignment = Alignment.Center){
        Text(course.title)
    }
}

@Composable
fun STTList(modifier: Modifier = Modifier, content: List<String>) {
    LazyColumn(modifier = modifier.fillMaxWidth().padding(20.dp, 0.dp)) {
        items(content.size) {
            Text(content[it], color = colorResource(R.color.text_log), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun RecordApp(modifier: Modifier = Modifier, viewModel: RecordActivityViewModel) {
    val sttContent = viewModel.textContent
    val blankTitle = remember { mutableStateOf(false) }
    val course = viewModel.course.collectAsState()
    val courseTitle = remember { mutableStateOf("") }
    val recordTime by viewModel.recordTimeText.collectAsState("")
    val isStart by viewModel.isStart
    if (!blankTitle.value && course.value.title.isBlank()){
        AlertDialog(
            onDismissRequest = {},
            title = {Text("课程名称")},
            text = {
                OutlinedTextField(courseTitle.value, {courseTitle.value = it}, placeholder = { Text("输入课程名称") })
            },
            confirmButton = {
                Button({
                    if (course.value.title.isBlank()){
                        blankTitle.value = true
                    }else{
                        viewModel.setTitle(courseTitle.value)
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton({
                    viewModel.setTitle("")
                    blankTitle.value = true
                }) {
                    Text("交由AI生成")
                }
            }
        )
    }
    Column(modifier = modifier.fillMaxSize().padding(top = 54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(recordTime, color = Color.White, fontSize = 54.sp, modifier = Modifier.padding(vertical = 50.dp))
        STTList(modifier = Modifier.weight(1f), content = sttContent)
        IconButton(
            modifier = Modifier.padding(0.dp, 90.dp).size(96.dp).background(colorResource(R.color.record_button), CircleShape),
            onClick = {
                if (isStart) {
                    viewModel.stop()
                }else{
                    viewModel.start()
                }
            }
        ) {
            Text(if(isStart) "暂停" else "开始", color = Color.White)
        }
    }
}

@Preview
@Composable
fun RecordActivityPreview() {
    val vm = RecordActivityViewModel()
    for (i in 0 until 100){
        vm.addContent("-----------------$i")
    }
    RecordApp(viewModel = vm)
}