package cn.bincker.classroom.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import cn.bincker.classroom.assistant.asr.AsrResponse
import cn.bincker.classroom.assistant.asr.BytedanceAsrClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists


class AudioRecordService : Service() {
    private var notification: Notification? = null
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var path: Path? = null
    private var outputFile: RandomAccessFile? = null
    private var asrClient: BytedanceAsrClient? = null
    private var stopping = false
    companion object {
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 1

        enum class Command{
            Start,
            Stop
        }
    }

    override fun onCreate() {
        super.onCreate()


        val channelId = CHANNEL_ID
        val channelName = "audio record channel"
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        // 可选配置
        channel.enableLights(true)
        channel.lightColor = 0xff0000
        channel.setShowBadge(true)

        val manager = getSystemService<NotificationManager?>(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("麦克风录制")
            .setContentText("正在录制")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onBind(intent: Intent): IBinder {
        return AudioRecordBinder()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val command = intent?.getIntExtra("command", -1)
        when (command){
            Command.Start.ordinal -> {
                path = Paths.get(intent.getStringExtra("path"))
                Log.d("AudioRecordService.onBind", "on bind: path=$path")
                if (!isRecording) startRecord()
            }
            Command.Stop.ordinal -> stopRecording()
            else -> Log.d("AudioRecordService.onStartCommand", "unknown command: $command")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun stopRecording() {
        if (!stopping){//即将停止
            stopping = true
        }else{//停止
            audioRecord?.stop()
            isRecording = false
            outputFile?.let {
                finalizeWavFile(it)
            }
            outputFile = null
        }
    }

    override fun stopService(name: Intent?): Boolean {
        stopRecording()
        return super.stopService(name)
    }

    @SuppressLint("ForegroundServiceType")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecord(){
        startForeground(NOTIFICATION_ID, notification)
        val sampleRate = 16000
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_DEFAULT,
            AudioFormat.ENCODING_PCM_16BIT,
            102400
        ).also {
            asrClient = BytedanceAsrClient(
                BuildConfig.BYTEDANCE_APPPID,
                BuildConfig.BYTEDANCE_ACCESS_TOKEN,
                it.bufferSizeInFrames,
                it.channelCount
            )
        }
        val channelCount = audioRecord?.channelCount?.toShort() ?: 1
        path?.let {
            if (it.parent.notExists()){
                Files.createDirectories(it.parent)
            }
            it.toFile().let { file->
                outputFile = RandomAccessFile(file, "rw")
                if (!file.exists()){
                    outputFile?.write("RIFF".toByteArray())          // ChunkID
                    outputFile?.writeIntLittleEndian(0)              // 临时 ChunkSize (后续修正)
                    outputFile?.write("WAVE".toByteArray())          // Format
                    outputFile?.write("fmt ".toByteArray())          // Subchunk1ID
                    outputFile?.writeIntLittleEndian(16)             // Subchunk1Size
                    outputFile?.writeShortLittleEndian(1)            // AudioFormat (PCM)
                    outputFile?.writeShortLittleEndian(channelCount) // NumChannels
                    outputFile?.writeIntLittleEndian(sampleRate)     // SampleRate
                    outputFile?.writeIntLittleEndian(sampleRate * channelCount * 16 / 8) // ByteRate
                    outputFile?.writeShortLittleEndian((channelCount * 16 / 8).toShort()) // BlockAlign
                    outputFile?.writeShortLittleEndian(16.toShort()) // BitsPerSample
                    outputFile?.write("data".toByteArray())          // Subchunk2ID
                    outputFile?.writeIntLittleEndian(0)              // 临时 Subchunk2Size (后续修正)
                }
            }
        }

        runBlocking {
            asrClient?.waitReady()
        }

        audioRecord?.startRecording()
        isRecording = true
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("audioRecordService.startRecord", "bufferSizeInFrames=${audioRecord!!.bufferSizeInFrames}")
            val buffer = ByteArray(audioRecord!!.bufferSizeInFrames)
            var len = 0
            while (isRecording) {
                if (audioRecord == null) {
                    isRecording = false
                    break
                }
                len = audioRecord?.read(buffer, 0, audioRecord?.bufferSizeInFrames ?: 0) ?: 0
                if (len <= 0) continue
                outputFile?.write(buffer, 0, len)
                asrClient?.sendAudioOnlyRequest(buffer, 0, len, stopping)
                if (stopping) {
                    stopRecording()
                }
            }
        }
    }

    inner class AudioRecordBinder : Binder() {
        val asrResponseStateFlow: MutableStateFlow<AsrResponse>?
            get() = asrClient?.messageFlow
    }

    /**
     * 完成录制后修正 WAV 头部
     */
    fun finalizeWavFile(file: RandomAccessFile) {
        val fileSize = file.length()
        val dataSize = fileSize - 44 // 减去 44 字节头部

        // 更新 ChunkSize (文件总大小 - 8)
        file.seek(4)
        file.writeIntLittleEndian((fileSize - 8).toInt())

        // 更新 Subchunk2Size (PCM 数据大小)
        file.seek(40)
        file.writeIntLittleEndian(dataSize.toInt())

        file.close()
    }

    // 辅助函数：小端序写入 Int/Short
    fun RandomAccessFile.writeIntLittleEndian(value: Int) {
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array().let { write(it) }
    }
    fun RandomAccessFile.writeShortLittleEndian(value: Short) {
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array().let { write(it) }
    }
}