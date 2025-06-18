package cn.bincker.classroom.assistant

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class AudioRecordService : Service() {
    private var notification: Notification? = null
    private var isRecorded = false

    override fun onBind(intent: Intent): IBinder {
        Log.d("AudioRecordService.onBind", "on bind")
        if (!isRecorded) startRecord()
        return AudioRecordBinder()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startRecord(){
        notification = NotificationCompat.Builder(this, "recording_channel")
            .setContentTitle("录制中")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    class AudioRecordBinder : Binder() {

    }
}