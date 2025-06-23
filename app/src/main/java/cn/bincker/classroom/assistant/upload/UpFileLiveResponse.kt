package cn.bincker.classroom.assistant.upload

data class UpFileLiveResponse<T>(
    var status: Int = 0,
    var data: T? = null
)