package cn.bincker.classroom.assistant.chat

import android.util.Log
import cn.bincker.classroom.assistant.BuildConfig
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration

const val URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
class BytedanceChatApi {
    private val client = OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(3)).build()

    fun streamChat(messages: List<ChatMessage>, callback: (line: ChatResponse)->Unit){
        val bodyContent = Gson().toJson(ChatRequest(messages = messages))
        Log.d("BytedanceChatApi.streamChat", "bodyContent=$bodyContent")
        val requestBody = bodyContent.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer " + BuildConfig.BYTEDANCE_APIKEY)
            .post(requestBody)
            .build()
        client.newCall(request).execute().use { response->
            val gson = Gson()
            response.body.charStream().forEachLine {l->
                if (l.isBlank()) return@forEachLine
                val line = if (l.startsWith("data: ")) l.substring(6) else l
                try {
                    val chatResponse: ChatResponse? = gson.fromJson<ChatResponse>(line, ChatResponse::class.java)
                    if (chatResponse == null) return@forEachLine
                    callback.invoke(chatResponse)
                }catch (e: Exception){
                    Log.e("BytedanceChatApi.streamChat", "parse chat response failed: line=$line", e)
                }
            }
        }
    }
}