package cn.bincker.classroom.assistant.asr

data class AsrResponse(
    var sequence: Int = 0,
    var serializationMethod: Int = SERVER_ERROR_RESPONSE.toInt(),
    var protocolVersion: Int = 1,
    var reserved: Int = 0,
    var headerSize: Int = 0,
    var messageCompression: Int = 0,
    var messageType: Int = 0,
    var payloadSize: Int = 0,
    var messageTypeSpecificFlags: Int = 0,
    var payload: Payload = Payload()
) {
    companion object{
        /**
         * {
         * "audio_info":{"duration":9600},
         * "result":{
         *   "additions":{"log_id":"20250622163644B5501DFE8F745CFA3C9B"},
         *   "text":"你现在过得怎么样呀？在那边过得还好吗",
         *   "utterances":[
         *     {
         *       "definite":false,
         *       "end_time":8172,
         *       "start_time":1642,
         *       "text":"你现在过得怎么样呀？在那边过得还好吗",
         *       "words":[
         *         {"end_time":1722,"start_time":1642,"text":"你"},
         *         {"end_time":1962,"start_time":1882,"text":"现在"},
         *         {"end_time":2202,"start_time":2122,"text":"过得"},
         *         {"end_time":2362,"start_time":2282,"text":"怎么样"},
         *         {"end_time":2742,"start_time":2662,"text":"呀"},
         *         {"end_time":5372,"start_time":5292,"text":"在"},
         *         {"end_time":5832,"start_time":5752,"text":"那边"},
         *         {"end_time":7712,"start_time":7632,"text":"过得"},
         *         {"end_time":7952,"start_time":7872,"text":"还好"},
         *         {"end_time":8172,"start_time":8092,"text":"吗"}
         *        ]
         *      }
         *   ]
         * }
         * }
         */
        data class Payload(
            var audioInfo: AudioInfo = AudioInfo(0),
            var result: Result = Result(),
        )
        data class AudioInfo(
            var duration: Long
        )
        data class Result(
            var additions: Additions = Additions(),
            var text: String = "",
            var utterances: List<Utterance> = mutableListOf<Utterance>()
        )
        data class Additions(var logId: String = "")
        data class Utterance(
            var definite: Boolean = false,
            var startTime: Long = 0,
            var endTime: Long = 0,
            var text: String = "",
            var words: List<Word> = mutableListOf<Word>()
        )
        data class Word(var startTime: Long = 0, var endTime: Long = 0, var text: String = "")
    }
}