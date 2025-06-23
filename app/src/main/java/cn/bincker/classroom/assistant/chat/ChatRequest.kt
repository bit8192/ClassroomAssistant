package cn.bincker.classroom.assistant.chat

data class ChatRequest(
    var model: String = "ep-20250622185033-sctfb",
    var messages: List<ChatMessage> = mutableListOf<ChatMessage>(),
    var stream: Boolean = true
)