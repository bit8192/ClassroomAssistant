package cn.bincker.classroom.assistant.vm

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.bincker.classroom.assistant.asr.AsrResponse
import cn.bincker.classroom.assistant.data.entity.FileInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

class FileInfoViewModel(fileInfo: FileInfo): ViewModel() {
    val path = mutableStateOf(fileInfo.path)
    val fileName: String
        get() {
            val index = path.value.lastIndexOf(File.separator)
            return if (index == -1) return path.value else path.value.substring(index + 1)
        }
    val type = mutableStateOf(fileInfo.type)
    val time = mutableLongStateOf(fileInfo.time)
    val description = mutableStateListOf(*fileInfo.description.split("\n").toTypedArray())

    fun getFileInfo() = FileInfo(path.value, type.value, time.longValue, description.joinToString("\n"))

    var recordedTime = mutableLongStateOf(0L)
    var isStart = mutableStateOf(false)
    private var startTime = 0L
    val recordTime = MutableStateFlow<Long>(0)
    val recordTimeText = recordTime.asStateFlow().map {
        val time = it + recordedTime.longValue
        val h = time / 3600_000L
        val m = time / 60_000 % 60
        val s = time / 1000 % 60
        val stringBuilder = StringBuilder()
        if (h < 10) stringBuilder.append("0")
        stringBuilder.append(h).append(":")
        if (m < 10) stringBuilder.append("0")
        stringBuilder.append(m).append(":")
        if (s < 10) stringBuilder.append("0")
        stringBuilder.append(s)
        stringBuilder.toString()
    }

    val writable = mutableStateOf(false)
    val loading = mutableStateOf(false)

    init {
        viewModelScope.launch {
            if (type.value == FileInfo.Companion.FileType.AUDIO){
                if (path.value.startsWith("content://")){
                    writable.value = false
                    loading.value = true
                }else if(Paths.get(path.value).exists()){
                    loading.value = true
                    recordedTime.longValue = getWavDuration(path.value).toLong()
                }else{
                    writable.value = true
                }
            }
        }
    }

    fun start(){
        isStart.value = true
        viewModelScope.launch {
            startTime = System.currentTimeMillis()
            while (isStart.value) {
                recordTime.value = System.currentTimeMillis() - startTime
                delay(1000)
            }
        }
    }

    fun stop() {
        isStart.value = false
        viewModelScope.launch {
            recordedTime.longValue += System.currentTimeMillis() - startTime
            recordTime.value = 0
        }
    }

    fun delete() {
        isStart.value = false
        Paths.get(path.value).deleteIfExists()
    }

    private var newLine = true
    fun onMessage(response: AsrResponse) {
        response.payload.result.let { result->
            if (result.text.isEmpty()) return@let
            if (newLine){
                description.add(result.text)
                newLine = false
            }else{
                description[description.lastIndex] = result.text
                if (result.utterances.first().definite == true) {
                    newLine = true
                }
            }
        }
    }

    fun getWavDuration(filePath: String): Long {
        RandomAccessFile(filePath, "r").use { file ->
            // 1. 读取必要的头部字段
            file.seek(24) // 跳到 SampleRate 的位置
            val sampleRate = file.readIntLittleEndian()
            val numChannels = file.readShortLittleEndian()
            val bitsPerSample = file.readShortLittleEndian()
            file.seek(40) // 跳到 Subchunk2Size 的位置
            val dataSize = file.readIntLittleEndian()

            // 2. 计算 ByteRate 和时长
            val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
            return (dataSize.toDouble() / byteRate * 1000).toLong()
        }
    }

    // 辅助函数：读取小端序的 Int 和 Short
    fun RandomAccessFile.readIntLittleEndian(): Int {
        val bytes = ByteArray(4)
        read(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }

    fun RandomAccessFile.readShortLittleEndian(): Short {
        val bytes = ByteArray(2)
        read(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short
    }
}