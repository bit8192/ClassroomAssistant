package cn.bincker.classroom.assistant

import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.bincker.classroom.assistant.RecordActivity.RecordServiceConnection
import cn.bincker.classroom.assistant.asr.FULL_SERVER_RESPONSE
import cn.bincker.classroom.assistant.data.entity.FileInfo
import cn.bincker.classroom.assistant.ui.theme.ClassroomAssistantTheme
import cn.bincker.classroom.assistant.vm.FileInfoViewModel
import cn.bincker.classroom.assistant.vm.RecordActivityViewModel
import kotlinx.coroutines.launch
import androidx.core.net.toUri


class RecordActivity : ComponentActivity() {
    private val vm: RecordActivityViewModel by viewModels<RecordActivityViewModel>()
    private val serviceConnection = RecordServiceConnection().apply {
        onServiceConnectionListener = {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    audioRecordBinder?.asrStreamResponseStateFlow?.collect { response->
                        if (response.messageType != FULL_SERVER_RESPONSE.toInt()) return@collect
                        val currentRecorder = vm.fileInfos[vm.pagerState.currentPage]
                        currentRecorder.onMessage(response)
                    }
                }
            }
        }
    }

    class RecordServiceConnection: ServiceConnection{
        private var _audioRecordBinder: AudioRecordService.AudioRecordBinder? = null
        val audioRecordBinder: AudioRecordService.AudioRecordBinder?
            get() = _audioRecordBinder
        var isBind = false
        var onServiceConnectionListener: (()->Unit)? = null
        override fun onServiceConnected(
            name: ComponentName,
            binder: IBinder
        ) {
            _audioRecordBinder = binder as AudioRecordService.AudioRecordBinder
            isBind = true
            onServiceConnectionListener?.invoke()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBind = false
            _audioRecordBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassroomAssistantTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {TopBar(viewModel = vm)}) { innerPadding ->
                    RecordApp(modifier = Modifier.padding(innerPadding), vm, serviceConnection)
                }
            }
        }
    }
}

@Composable
fun TopBar(modifier: Modifier = Modifier, viewModel: RecordActivityViewModel) {
    val course by viewModel.course.collectAsState()
    Box(modifier = modifier
        .fillMaxWidth()
        .height(65.dp), contentAlignment = Alignment.Center){
        Text(course.title)
    }
}

@Composable
fun STTList(modifier: Modifier = Modifier, content: List<String>) {
    LazyColumn(modifier = modifier
        .fillMaxWidth()
        .padding(20.dp, 0.dp)) {
        items(content.size) {
            Text(content[it], color = colorResource(R.color.text_log), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun RecordApp(modifier: Modifier = Modifier, viewModel: RecordActivityViewModel, serviceConnection: RecordServiceConnection) {
    val blankTitle = remember { mutableStateOf(false) }
    val course = viewModel.course.collectAsState()
    val courseTitle = remember { mutableStateOf("") }
    val pagerState = viewModel.pagerState
    val showAddContentChooseDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.addImage(uri) }
        }
    )
    val selectAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.addAudio(context, uri) }
        }
    )
    val tabScrollState = rememberScrollState()
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
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background), horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton({
                    showAddContentChooseDialog.value = false
                    viewModel.addAudioRecord(context)
                }, modifier = Modifier.fillMaxWidth()) { Text("录制音频", modifier = Modifier.padding(85.dp, 15.dp)) }
                TextButton({
                    showAddContentChooseDialog.value = false
                    selectAudioLauncher.launch("audio/*")
                }, modifier = Modifier.fillMaxWidth()){ Text("本地录音/音频", modifier = Modifier.padding(85.dp, 15.dp)) }
                TextButton({
                    showAddContentChooseDialog.value = false
                    selectImageLauncher.launch("image/*")
                }, modifier = Modifier.fillMaxWidth()){ Text("图片/照片", modifier = Modifier.padding(85.dp, 15.dp)) }
                TextButton({
                    showAddContentChooseDialog.value = false
                    if (viewModel.fileInfos.isEmpty()){
                        Toast.makeText(context, "请至少添加一个素材", Toast.LENGTH_SHORT).show()
                    }else {
                        viewModel.save(context)
                        (context as RecordActivity).also {
                            it.startActivity(
                                Intent(context, CourseActivity::class.java)
                                    .also { intent ->
                                        intent.putExtra("id", course.value.id)
                                    }
                            )
                            it.finish()
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()){ Text("完成/AI总结", modifier = Modifier.padding(85.dp, 15.dp)) }
            }
        }
    }
    Column(modifier = modifier
        .fillMaxSize()
        .padding(top = 54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (viewModel.fileInfos.isEmpty()){
            Column(modifier = Modifier
                .clickable { showAddContentChooseDialog.value = true }
                .weight(1f)
                .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
                Text("点击添加内容", color = colorResource(R.color.text_log))
            }
        }else {
            HorizontalPager(pagerState, modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) { page ->
                val fileInfo = viewModel.fileInfos[page]
                if (fileInfo.type.value == FileInfo.Companion.FileType.AUDIO) {
                    RecordView(vm = fileInfo, serviceConnection = serviceConnection, onDelete = {
                        viewModel.deleteFile(page)
                    })
                } else {
                    ImageFileView(vm = fileInfo, onDelete = {
                        viewModel.deleteFile(page)
                    })
                }
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(tabScrollState), horizontalArrangement = Arrangement.Start) {
            repeat(viewModel.fileInfos.size) { index->
                Box(modifier = Modifier
                    .size(60.dp)
                    .background(if (pagerState.currentPage == index) colorResource(R.color.purple_500) else Color.Gray), contentAlignment = Alignment.Center) {
                    Text(viewModel.fileInfos[index].fileName, color = if (pagerState.currentPage == index) Color.White else LocalContentColor.current)
                }
            }
            IconButton({showAddContentChooseDialog.value = true}, modifier = Modifier.size(60.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    }
}

@Composable
fun RecordView(modifier: Modifier = Modifier, vm: FileInfoViewModel, serviceConnection: RecordServiceConnection, onDelete: ()->Unit = {}){
    val recordTimeText by vm.recordTimeText.collectAsState("")
    val recordTime by vm.recordedTime
    val isStart by vm.isStart
    val context = LocalContext.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(recordTimeText, fontSize = 54.sp, modifier = Modifier.padding(vertical = 50.dp))
        STTList(modifier = Modifier.weight(1f), content = vm.description)
        IconButton(
            modifier = Modifier
                .padding(0.dp, 30.dp)
                .size(96.dp)
                .background(if (vm.writable.value) colorResource(R.color.record_button) else colorResource(R.color.record_button_disabled), CircleShape),
            onClick = {
                if (isStart) {
                    vm.stop()
                    val intent = Intent(context, AudioRecordService::class.java)
                    intent.putExtra("command", AudioRecordService.Companion.Command.Stop.ordinal)
                    context.startService(intent)
                    context.unbindService(serviceConnection)
                    serviceConnection.isBind = false
                }else{
                    if (serviceConnection.isBind) {
                        Toast.makeText(context, "正在录制中，无法重复录制", Toast.LENGTH_SHORT).show()
                    }else{
                        vm.start()
                        context.bindService(Intent(context, AudioRecordService::class.java), serviceConnection, BIND_AUTO_CREATE)
                        val intent = Intent(context, AudioRecordService::class.java)
                        intent.putExtra("path", vm.path.value)
                        intent.putExtra("command", AudioRecordService.Companion.Command.Start.ordinal)
                        context.startService(intent)
                    }
                }
            },
            enabled = vm.writable.value
        ) {
            if (vm.loading.value){
                CircularProgressIndicator()
            }else{
                if (vm.writable.value) {
                    Text(
                        if (isStart) "停止" else if (recordTime == 0L) "开始" else "继续",
                        color = Color.White
                    )
                }else{
                    Text("不可写", color = colorResource(R.color.text_disable))
                }
            }
        }
        IconButton(onDelete, modifier = Modifier.size(60.dp, 40.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "delete")
        }
    }
}

@Composable
fun ImageFileView(modifier: Modifier = Modifier, vm: FileInfoViewModel, onDelete: () -> Unit){
    val context = LocalContext.current
    val bitmap = remember(vm.path.value) {
        mutableStateOf<Bitmap?>(null)
    }
    LaunchedEffect(vm.path.value) {
        if (vm.path.value.startsWith("content://")){
            bitmap.value = BitmapFactory.decodeStream(context.contentResolver.openInputStream(vm.path.value.toUri()))
        }else{
            bitmap.value = BitmapFactory.decodeFile(vm.path.value)
        }
    }
    Column(modifier = modifier) {
        bitmap.value?.let {
            Image(bitmap = it.asImageBitmap(), modifier = Modifier.fillMaxSize(), contentDescription = "Image")
        }
        IconButton(onDelete, modifier = Modifier.size(60.dp, 40.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "delete")
        }
    }
}

@Preview
@Composable
fun RecordActivityPreview() {
    val vm = RecordActivityViewModel()
    vm.setTitle("test")
    vm.addAudioRecord(LocalContext.current)
    val serviceConnection = RecordServiceConnection()
    RecordApp(viewModel = vm, serviceConnection = serviceConnection)
}