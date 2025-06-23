package cn.bincker.classroom.assistant.upload

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.time.Duration

const val URL_GOFILE = "https://upload.gofile.io/uploadfile"
const val URL_FILEIO = "https://file.io/"
const val URL_0X0ST = "https://0x0.st"

class UploadApi {
    private val okhttp = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(Duration.ofSeconds(3))
        .callTimeout(Duration.ofMinutes(5))
        .build()

    private fun getFilename(file: String): String{
        val separatorIndex = file.indexOf(File.separator)
        if (separatorIndex == -1) return file
        return file.substring(separatorIndex + 1)
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

    fun uploadFile(context: Context, file: String): String?{
        var request = Request.Builder()
            .url("https://upfile.live/api/file/getUploadLink/")
            .post("vipCode=&file_name=${URLEncoder.encode(getFilename(file), "utf-8")}".toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType()))
            .build()
        val uploadUrlResponse: GetUploadUrlResponse? = okhttp.newCall(request).execute().use { response->
            var str = response.body.string()
            val typeToken: TypeToken<UpFileLiveResponse<GetUploadUrlResponse>> = TypeToken.getParameterized(UpFileLiveResponse::class.java,
                GetUploadUrlResponse::class.java) as TypeToken<UpFileLiveResponse<GetUploadUrlResponse>>
            val result: UpFileLiveResponse<GetUploadUrlResponse> = Gson().fromJson(str, typeToken)
            if (result.status == 1){
                result.data
            }else null
        }
        Log.d("UploadApi.uploadFile", "upload file url content: ${uploadUrlResponse?.toString()}")
        val uploadUrl = uploadUrlResponse?.uploadUrl
        if (uploadUrl?.isNotBlank() != true) {
            Log.d("FileApi.uploadFile", "upload url is empty")
            return null
        }
        val fileBody = getFileRequestBody(context, file)
//        val fileBody = "hello world".toRequestBody()
        request = Request.Builder()
            .put(fileBody)
            .url(uploadUrl)
            .addHeader("Accept", "application/json")
            .build()
        Log.d("BytedanceAsrFileApi.uploadFile", "start upload")
        val uploadDataCompleted = okhttp.newCall(request).execute().use { response: Response->
            response.isSuccessful
        }
        if (!uploadDataCompleted) {
            Log.e("UploadApi.uploadFile", "upload content failed")
            return null
        }
        request = Request.Builder()
            .url("https://upfile.live/api/file/upload/")
            .post("file_size=${fileBody.contentLength()}&file_name=${URLEncoder.encode(uploadUrlResponse.fileName, "utf-8")}&file_key=${URLEncoder.encode(uploadUrlResponse.fileKey, "utf-8")}".toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType()))
            .build()
        val url: String? = okhttp.newCall(request).execute().use { response: Response->
            if (response.isSuccessful){
                val str = response.body.string()
                Log.d("UploadApi.uploadFile", "upload completed: $str")
                val result: UpFileLiveResponse<FileUploadCompleteResponse> = Gson().fromJson<UpFileLiveResponse<FileUploadCompleteResponse>>(str, TypeToken.getParameterized(
                    UpFileLiveResponse::class.java, FileUploadCompleteResponse::class.java) as TypeToken<UpFileLiveResponse<FileUploadCompleteResponse>>)
                Log.d("UploadApi.uploadFile", "upload completed: https://upfile.live/download/" + result.data?.fileId)
                "https://upfile.live/download/" + result.data?.fileId
            }else{
                Log.d("UploadApi.uploadFile", "send upload completed failed.")
                null
            }
        }
        if (url == null) return null
        request = Request.Builder().url(url).get().build()
        return okhttp.newCall(request).execute().use { response: Response ->
            response.header("location")
        }
    }
}