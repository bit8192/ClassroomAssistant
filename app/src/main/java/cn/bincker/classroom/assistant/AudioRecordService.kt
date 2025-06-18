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
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists



class AudioRecordService : Service() {
    private var notification: Notification? = null
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var path: Path? = null
    private var fileOutputStream: OutputStream? = null
    companion object {
        private const val BUFFER_SIZE = 1024
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 1
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

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onBind(intent: Intent): IBinder {
        path = Paths.get(intent.getStringExtra("path"))
        Log.d("AudioRecordService.onBind", "on bind: path=$path")
        if (!isRecording) startRecord()
        return AudioRecordBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        audioRecord?.stop()
        isRecording = false
        fileOutputStream?.close()
        fileOutputStream = null
        return super.onUnbind(intent)
    }

    @SuppressLint("ForegroundServiceType")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecord(){
        startForeground(NOTIFICATION_ID, notification)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_DEFAULT,
            AudioFormat.ENCODING_PCM_16BIT,
            BUFFER_SIZE
        )
        path?.let {
            if (it.parent.notExists()){
                Files.createDirectories(it.parent)
            }
            fileOutputStream = FileOutputStream(it.toFile())
        }
        audioRecord?.startRecording()
        isRecording = true
        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(BUFFER_SIZE)
            var len = 0
            while (isRecording) {
                if (audioRecord == null) {
                    isRecording = false
                    break
                }
                len = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                if (len <= 0) continue
                fileOutputStream?.write(buffer, 0, len)
            }
        }
    }

    class AudioRecordBinder : Binder() {

    }
}