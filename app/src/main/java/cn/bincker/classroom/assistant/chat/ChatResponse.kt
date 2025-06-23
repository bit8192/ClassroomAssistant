package cn.bincker.classroom.assistant.chat

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    var choices: List<Choice>,
    var created: Long? = null,
    var id: String? = null,
    var model: String? = null,
    @SerializedName("service_tier")
    var serviceTier: String? = null,
    var `object`: String? = null,
    var usage: String? = null
){
    data class Choice(
        var delta: Delta? = null,
        var index: Int? = null
    )
    data class Delta(
        var content: String? = null,
        @SerializedName("reasoning_content")
        var reasoningContent: String? = null,
        var role: String? = null
    )
}