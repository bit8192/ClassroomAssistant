package cn.bincker.classroom.assistant

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        runBlocking {
            val channel = Channel<Unit>()
            launch {
                while (true) {
                    delay(1000)
                    channel.send(Unit)
                }
            }
            println("start")
            val job = launch {
                var i = 0
                while (i++ < 3) {
                    channel.receive()
                    println("receive")
                }
                println("completed")
            }
            job.join()
        }
    }

    @Test
    fun log() {
        Log.d("test", "test")
    }
}