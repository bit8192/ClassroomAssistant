package cn.bincker.classroom.assistant.asr

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration

const val URL_GOFILE = "https://upload.gofile.io/uploadfile"
const val URL_FILEIO = "https://file.io/"
const val URL_0X0ST = "https://0x0.st"
const val URL_BASE = "https://openspeech.bytedance.com/api/v3/auc/bigmodel"
const val URL_SUBMIT = "$URL_BASE/submit"
const val URL_QUERY = "$URL_BASE/query"
val jsonMediaType = "application/json; charset=UTF-8".toMediaType()
class BytedanceAsrFileApi(val appId: String, val accessToken: String) {
    private val okhttp = OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(3)).build()

    private fun getFilename(file: String): String{
        val separatorIndex = file.indexOf(File.separator)
        if (separatorIndex == -1) return file
        return file.substring(separatorIndex + 1)
    }

    fun uploadFile(context: Context, file: String){
        val body = MultipartBody.Builder()
            .addFormDataPart("file", getFilename(file), getFileRequestBody(context, file))
            .addFormDataPart("expires", (24 * 3600).toString())
            .build()
        var request = Request.Builder()
            .post(body)
            .url(URL_0X0ST)
            .addHeader("Accept", "application/json")
            .build()
        Log.d("BytedanceAsrFileApi.uploadFile", "start upload")
        okhttp.newCall(request).execute().use { response->
            Log.d("BytedanceAsrFileApi.uploadFile", response.body.string())
        }
    }

    private fun getFileRequestBody(context: Context, file: String): RequestBody {
        return if (file.startsWith("content://")){
            context.contentResolver.openInputStream(file.toUri()).use { fileIn->
                ByteArrayOutputStream().use { out->
                    val buffer = ByteArray(8192)
                    var len = 0
                    len = fileIn?.read(buffer) ?: 0
                    while (len > 0){
                        out.write(buffer, 0, len)
                        len = fileIn?.read(buffer) ?: 0
                    }
                    out.toByteArray().toRequestBody()
                }
            }
        }else{
            File(file).asRequestBody()
        }
    }

    fun submit(file: String, asrRequest: AsrRequest) {
//        val request = Request.Builder().post(Gson().toJson(asrRequest).toRequestBody(jsonMediaType))
    }
}