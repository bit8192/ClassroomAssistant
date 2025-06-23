package cn.bincker.classroom.assistant.asr

data class AsrResponse(
    var audioInfo: AudioInfo = AudioInfo(0),
    var result: Result = Result(),
){
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