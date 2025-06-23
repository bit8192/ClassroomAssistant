package cn.bincker.classroom.assistant.chat

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    var role: String = "system",
    var content: List<MessageContent> = mutableListOf<MessageContent>()
){
    data class MessageContent(
        var text: String? = null,
        @SerializedName("image_url") var imageUrl: ImageUrl? = null,
        var type: String = "text",
    )
    data class ImageUrl(var url: String = "", var detail: String = "auto")
}