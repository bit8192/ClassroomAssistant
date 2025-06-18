package cn.bincker.classroom.assistant.data.entity

data class FileInfo(
    val path: String,
    val type: FileType,
    val time: Long,
    val description: String
){
    companion object{
        enum class FileType{
            AUDIO,
            IMAGE
        }
    }
}