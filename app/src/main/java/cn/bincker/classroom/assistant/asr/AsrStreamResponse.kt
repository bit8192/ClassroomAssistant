package cn.bincker.classroom.assistant.asr

data class AsrStreamResponse(
    var sequence: Int = 0,
    var serializationMethod: Int = SERVER_ERROR_RESPONSE.toInt(),
    var protocolVersion: Int = 1,
    var reserved: Int = 0,
    var headerSize: Int = 0,
    var messageCompression: Int = 0,
    var messageType: Int = 0,
    var payloadSize: Int = 0,
    var messageTypeSpecificFlags: Int = 0,
    var payload: AsrResponse = AsrResponse()
)