package cn.bincker.classroom.assistant.upload

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadApiTest {
    @Test
    fun uploadFile() {
        val api = UploadApi()
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        api.uploadFile(appContext, "test.txt")
    }

}