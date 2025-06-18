package cn.bincker.classroom.assistant

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import cn.bincker.classroom.assistant.data.entity.Course

class AudioRecorder(val context: Context) {
    companion object {
        private const val RECORD_BUFFER_SIZE = 4096
    }
    private var audioRecord: AudioRecord? = null
    private var isRecord = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(course: Course) {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_DEFAULT,
            AudioFormat.ENCODING_PCM_16BIT,
            RECORD_BUFFER_SIZE
        )
        audioRecord?.startRecording()
        isRecord = true
        Thread {
            val buffer = ByteArray(RECORD_BUFFER_SIZE)
            while (isRecord){
                val bytesRead = audioRecord?.read(buffer, 0, RECORD_BUFFER_SIZE) ?: 0
                if (bytesRead > 0){
                    //TODO
                }else if (audioRecord == null){
                    isRecord = false
                }
            }
        }.start()
    }
}