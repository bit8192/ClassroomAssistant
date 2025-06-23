package cn.bincker.classroom.assistant.upload

import com.google.gson.annotations.SerializedName

data class GetUploadUrlResponse(
    @SerializedName("file_name")
    var fileName: String = "",
    @SerializedName("upload_url")
    var uploadUrl: String = "",
    @SerializedName("file_key")
    var fileKey: String = "",
    var time: String = ""
)