package cn.bincker.classroom.assistant.asr

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import cn.bincker.classroom.assistant.upload.UploadApi
import com.google.gson.Gson
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration
import java.util.UUID

const val URL_BASE = "https://openspeech.bytedance.com/api/v3/auc/bigmodel"
const val URL_SUBMIT = "$URL_BASE/submit"
const val URL_QUERY = "$URL_BASE/query"
val jsonMediaType = "application/json; charset=UTF-8".toMediaType()

class BytedanceAsrFileApi(val appId: String, val accessToken: String) {
    private val client = OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(3)).build()

    fun submit(context: Context, file: String, asrRequest: AsrRequest): String {
        val audioUrl = UploadApi().uploadFile(context, file) ?: throw Exception("上传音频文件失败")
//        val audioUrl = "https://upfile.live/download/ed532523"
//        val audioUrl = "https://upfile.live/download/9502d1ae"
//        val audioUrl = "https://file.upfile.live/uploads/20250623/aa31c18d11116a5683ba50d47382a190.mp3"
        Log.d("BytedanceAsrFileApi.submit", "audioUrl=$audioUrl")
        asrRequest.audio.url = audioUrl
        var format = context.contentResolver.getType(file.toUri()) ?: "wav"
        if (format.startsWith("audio/")) format = format.substring(6)
        if (format == "mpeg") format = "mp3"
        if (format == "x-wav") format = "wav"
        asrRequest.audio.format = format
        asrRequest.audio.rate = null
        asrRequest.audio.codec = null
        asrRequest.audio.bits = null
        asrRequest.audio.channel = null
        Log.d("BytedanceAsrFileApi.submit", "format: ${asrRequest.audio.format}")
        Log.d("BytedanceAsrFileApi.submit", Gson().toJson(asrRequest))
        val requestId = UUID.randomUUID().toString()
        val request: Request = Request.Builder()
            .url(URL_SUBMIT)
            .header("X-Api-App-Key", appId)
            .header("X-Api-Access-Key", accessToken)
            .header("X-Api-Resource-Id", "volc.bigasr.auc")
            .header("X-Api-Request-Id", requestId)
            .header("X-Api-Sequence", "-1")
            .post(Gson().toJson(asrRequest).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().use { response->
            if(response.isSuccessful && response.header("X-Api-Status-Code") == "20000000"){
                return requestId
            }
            throw Exception("创建Asr任务失败")
        }
    }

    suspend fun query(requestId: String): AsrResponse{
        val request: Request = Request.Builder()
            .url(URL_QUERY)
            .header("X-Api-App-Key", appId)
            .header("X-Api-Access-Key", accessToken)
            .header("X-Api-Resource-Id", "volc.bigasr.auc")
            .header("X-Api-Request-Id", requestId)
            .post("{}".toRequestBody(jsonMediaType))
            .build()
        var result: AsrResponse? = null
        do {
            result = client.newCall(request).execute().use { response->
                if(response.isSuccessful){
                    val status = response.header("X-Api-Status-Code")
                    when(status) {
                        "20000000" -> {
                            val str = response.body.string()
                            Gson().fromJson<AsrResponse>(str, AsrResponse::class.java)
                        }
                        "20000001" -> {delay(1000); null}
                        else -> {
                            throw Exception("译音错误：status=$status\tmsg=${response.body.string()}")
                        }
                    }
                }else{
                    throw Exception("查询译音结果失败")
                }
            }
        }while (result == null)
        return result
    }
}