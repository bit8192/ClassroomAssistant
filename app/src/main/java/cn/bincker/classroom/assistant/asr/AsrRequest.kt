package cn.bincker.classroom.assistant.asr

import java.util.UUID

data class AsrRequest(
    var user: User = User(),
    var audio: Audio = Audio(),
    var request: Request = Request(),
){
    data class User(var uid: String = UUID.randomUUID().toString().replace("-", ""))
    data class Audio(
        var url: String? = null,
        var format: String = "raw",
        var codec: String = "raw",
        var rate: Int = 16000,
        var bits: Int = 16,
        var channel: Int = 1,
    )
    data class Request(
        var modelName: String = "bigmodel",
        var enableItn: Boolean = true,
        var enablePUNC: Boolean = false,
        var enableDDC: Boolean = false,
        var enableSpeakerInfo: Boolean = false,
        var enableChannelSplit: Boolean? = null,
        var showUtterances: Boolean? = null,
        var resultType: String? = null,
        var vadSegment: Boolean = false,
        var endWindowSize: Int? = null,
        var sensitiveWordsFilter: WordFilter? = null,
        var corpus: String? = null,
        var boostingTableName: String? = null,
        var context: String? = null,
        var callback: String? = null,
        var callbackData: String? = null,
    )
    data class WordFilter(
        var systemReservedFilter: Boolean = true,
        var filterWithEmpty: List<String> = mutableListOf<String>(),
        var filterWithSigned: List<String> = mutableListOf<String>()
    )
}