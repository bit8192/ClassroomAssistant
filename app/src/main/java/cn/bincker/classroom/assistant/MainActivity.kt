package cn.bincker.classroom.assistant
import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import cn.bincker.classroom.assistant.data.database.AppDatabase
import cn.bincker.classroom.assistant.data.entity.Course
import cn.bincker.classroom.assistant.ui.theme.ClassroomAssistantTheme
import cn.bincker.classroom.assistant.vm.CourseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val permissionCheckChannel = Channel<IntArray>()
    private val viewModel by viewModels<CourseViewModel>()

    private fun addCourse() {
        lifecycleScope.launch {
            requestPermissions().also { deniedPermission ->
                if (deniedPermission.isEmpty()) {
                    startActivity(Intent(applicationContext, RecordActivity::class.java))
                }else{
                    viewModel.viewPermissionDeniedDialog.value = true
                    viewModel.deniedPermissionContent.value = "尚未获得以下权限：\n" +
                            deniedPermission.joinToString("\n") {
                                when (it) {
                                    permission.INTERNET -> "网络"
                                    permission.ACCESS_NETWORK_STATE -> "网络状态"
                                    permission.FOREGROUND_SERVICE -> "后台服务"
                                    permission.RECORD_AUDIO -> "麦克风"
                                    permission.READ_EXTERNAL_STORAGE -> "读取本地文件"
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) permission.FOREGROUND_SERVICE_MICROPHONE else "permission.FOREGROUND_SERVICE_MICROPHONE" -> "后台麦克风"
                                    else -> it
                                }
                            }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassroomAssistantTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CourseApp(modifier = Modifier.padding(innerPadding), onAddCourse = this::addCourse) {
                        CourseListScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            requestPermissions()
        }
    }

    suspend fun requestPermissions(): List<String>{
        val allPermissions = mutableListOf(
            permission.INTERNET,
            permission.ACCESS_NETWORK_STATE,
            permission.FOREGROUND_SERVICE,
            permission.RECORD_AUDIO,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) add(permission.FOREGROUND_SERVICE_MICROPHONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(permission.READ_MEDIA_IMAGES)
                add(permission.READ_MEDIA_AUDIO)
            }else{
                add(permission.READ_EXTERNAL_STORAGE)
            }
        }
        val deniedPermission = allPermissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (deniedPermission.isEmpty()) return deniedPermission
        ActivityCompat.requestPermissions(this, deniedPermission.toTypedArray(), 0)
        val result = permissionCheckChannel.receive()
        if (result.any { it != PackageManager.PERMISSION_GRANTED }){
            Log.e("MainActivity.requestPermissions", "permission denied: ${deniedPermission.filterIndexed { index, _-> result[index] != PackageManager.PERMISSION_GRANTED }.joinToString("、")}")
            return deniedPermission.filterIndexed { index,_-> result[index] != PackageManager.PERMISSION_GRANTED }
        }
        return emptyList()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        Log.d("MainActivity.onRequestPermissionResult", grantResults.joinToString(","))
        lifecycleScope.launch {
            permissionCheckChannel.send(grantResults)
        }
    }
}



@Composable
fun CourseApp(
    modifier: Modifier = Modifier,
    onAddCourse: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCourse,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                content()
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(viewModel: CourseViewModel) {
    val courseState by viewModel.courses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val viewPermissionDeniedDialog by viewModel.viewPermissionDeniedDialog
    val deniedPermissionContent by viewModel.deniedPermissionContent
    val context = LocalContext.current

    if (viewPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = {viewModel.viewPermissionDeniedDialog.value = false},
            confirmButton = {
                Button({
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK // 确保从后台也能跳转
                    }
                    context.startActivity(intent)
                }) { Text("去授权") }
            },
            title = { Text("未获授权") },
            text = { Text(deniedPermissionContent) }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadCourses(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (courseState.isEmpty() && !isLoading) {
            Text(
                text = "暂无课程，点击右下角按钮添加",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.loadCourses(context) }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(courseState, {i->i.id}) { course ->
                        CourseItem(course = course)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun CourseItem(course: Course) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
               context.startActivity(Intent(context, CourseActivity::class.java).apply {
                   putExtra("id", course.id)
               })
            }
    ) {
        Text(
            text = course.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateFormat.format(course.createTime),
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = course.summary,
            fontSize = 14.sp
        )
    }
}

@Preview
@Composable
fun previewAddButton() {
    CourseApp {
        Text("hello world")
    }
}