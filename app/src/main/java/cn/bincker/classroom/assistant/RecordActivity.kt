package cn.bincker.classroom.assistant

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.bincker.classroom.assistant.data.entity.FileInfo
import cn.bincker.classroom.assistant.ui.theme.ClassroomAssistantTheme
import cn.bincker.classroom.assistant.vm.FileInfoViewModel
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
    val blankTitle = remember { mutableStateOf(false) }
    val course = viewModel.course.collectAsState()
    val courseTitle = remember { mutableStateOf("") }
    val pagerState = rememberPagerState { viewModel.fileInfos.size }
    val showAddContentChooseDialog = remember { mutableStateOf(false) }
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
    if (showAddContentChooseDialog.value){
        Dialog({showAddContentChooseDialog.value = false}) {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                Text("录制音频", modifier = Modifier.padding(85.dp, 15.dp).clickable {
                    showAddContentChooseDialog.value = false
                    viewModel.addAudioRecord()
                })
                Text("图片/照片", modifier = Modifier.padding(85.dp, 15.dp).clickable {
                    showAddContentChooseDialog.value = false
                    //TODO
                })
            }
        }
    }
    Column(modifier = modifier.fillMaxSize().padding(top = 54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (viewModel.fileInfos.size == 0){
            Column(modifier = Modifier.clickable {  }.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
                Text("点击添加内容", color = colorResource(R.color.text_log))
            }
        }else {
            HorizontalPager(pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                val fileInfo = viewModel.fileInfos[page]
                if (fileInfo.type.value == FileInfo.Companion.FileType.AUDIO) {
                    RecordView(vm = fileInfo)
                } else {
                    ImageFileView(vm = fileInfo)
                }
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            repeat(5) { index->
                Box(modifier = Modifier.size(60.dp).background(Color.Gray), contentAlignment = Alignment.Center) {
                    Text(index.toString())
                }
            }
            IconButton({showAddContentChooseDialog.value = true}, modifier = Modifier.size(60.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    }
}

@Composable
fun RecordView(modifier: Modifier = Modifier, vm: FileInfoViewModel){
    val recordTime by vm.recordTimeText.collectAsState("")
    val isStart by vm.isStart
    Column(modifier = modifier) {
        Text(recordTime, fontSize = 54.sp, modifier = Modifier.padding(vertical = 50.dp))
        STTList(modifier = Modifier.weight(1f), content = vm.description)
        IconButton(
            modifier = Modifier.padding(0.dp, 90.dp).size(96.dp).background(colorResource(R.color.record_button), CircleShape),
            onClick = {
                if (isStart) {
                    vm.stop()
                }else{
                    vm.start()
                }
            }
        ) {
            Text(if(isStart) "暂停" else "开始", color = Color.White)
        }
    }
}

@Composable
fun ImageFileView(modifier: Modifier = Modifier, vm: FileInfoViewModel){
    val bitmap = remember(vm.path.value) {
        mutableStateOf<Bitmap?>(null)
    }
    LaunchedEffect(vm.path.value) {
        bitmap.value = BitmapFactory.decodeFile(vm.path.value)
    }
    Column(modifier = modifier) {
        bitmap.value?.let {
            Image(bitmap = it.asImageBitmap(), modifier = Modifier.fillMaxSize(), contentDescription = "Image")
        }
    }
}

@Preview
@Composable
fun RecordActivityPreview() {
    val vm = RecordActivityViewModel()
    vm.setTitle("test")
    RecordApp(viewModel = vm)
}