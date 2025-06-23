package cn.bincker.classroom.assistant.asr

import com.google.gson.annotations.SerializedName
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
        var codec: String? = "raw",
        var rate: Int? = 16000,
        var bits: Int? = 16,
        var channel: Int? = 1,
    )
    data class Request(
        @SerializedName("model_name")
        var modelName: String = "bigmodel",

        @SerializedName("enable_itn")
        var enableItn: Boolean? = null,

        @SerializedName("enable_punc")
        var enablePUNC: Boolean? = null,

        var enableDDC: Boolean? = null,
        @SerializedName("enable_speaker_info")
        var enableSpeakerInfo: Boolean? = null,

        @SerializedName("enable_channel_split")
        var enableChannelSplit: Boolean? = null,

        @SerializedName("show_utterances")
        var showUtterances: Boolean? = null,

        @SerializedName("result_type")
        var resultType: String? = null,

        @SerializedName("vad_segment")
        var vadSegment: Boolean? = null,

        @SerializedName("end_window_size")
        var endWindowSize: Int? = null,

        @SerializedName("sensitive_words_filter")
        var sensitiveWordsFilter: WordFilter? = null,

        var corpus: String? = null,
        @SerializedName("boosting_table_name")
        var boostingTableName: String? = null,

        var context: String? = null,
        var callback: String? = null,
        @SerializedName("callback_data")
        var callbackData: String? = null,

    )
    data class WordFilter(
        @SerializedName("system_reserved_filter")
        var systemReservedFilter: Boolean = true,
        @SerializedName("filter_with_empty")
        var filterWithEmpty: List<String> = mutableListOf<String>(),
        @SerializedName("filter_with_signed")
        var filterWithSigned: List<String> = mutableListOf<String>()
    )
}