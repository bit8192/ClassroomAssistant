package cn.bincker.classroom.assistant.upload

import com.google.gson.annotations.SerializedName

data class FileUploadCompleteResponse(
    @SerializedName("file_id")
    var fileId: String = "",
    var time: String = ""
)