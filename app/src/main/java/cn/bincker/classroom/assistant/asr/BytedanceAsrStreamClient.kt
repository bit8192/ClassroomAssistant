package cn.bincker.classroom.assistant.asr

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


const val PROTOCOL_VERSION: Byte = 1
const val DEFAULT_HEADER_SIZE: Byte = 1
const val FULL_CLIENT_REQUEST: Byte = 1
const val AUDIO_ONLY_REQUEST: Byte = 2
const val FULL_SERVER_RESPONSE: Byte = 9
const val SERVER_ACK: Byte = 11
const val SERVER_ERROR_RESPONSE: Byte = 15
const val NO_SEQUENCE: Byte = 0 // no check sequence
const val POS_SEQUENCE: Byte = 1
const val NEG_SEQUENCE: Byte = 2
const val NEG_WITH_SEQUENCE: Byte = 3
const val NEG_SEQUENCE_1: Byte = 3
const val NO_SERIALIZATION: Byte = 0
const val JSON: Byte = 1

const val NO_COMPRESSION: Byte = 0
const val GZIP: Byte = 1

class BytedanceAsrClient: WebSocketListener {

    private var seq: Int = 0
    private val sampleRate: Int
    private val channels: Int
    private val okHttpClient: OkHttpClient
    private val webSocket: WebSocket
    private var _ready = false
    val ready: Boolean
        get() = _ready
    private val readyChannel = Channel<Boolean>()
    private val gson = Gson()
    val messageFlow = MutableStateFlow(AsrStreamResponse())

    constructor(appId: String, token: String, sampleRate: Int, channels: Int) : super() {
        this.sampleRate = sampleRate
        this.channels = channels

        val request: Request = Request.Builder()
            .url("wss://openspeech.bytedance.com/api/v3/sauc/bigmodel")
            .header("X-Api-App-Key", appId)
            .header("X-Api-Access-Key", token)
            .header("X-Api-Resource-Id", "volc.bigasr.sauc.duration")
            .header("X-Api-Connect-Id", UUID.randomUUID().toString())
            .build()

        okHttpClient = OkHttpClient.Builder().pingInterval(50, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(100, TimeUnit.SECONDS)
            .build()
        webSocket = okHttpClient.newWebSocket(request, this)
    }

    suspend fun waitReady(){
        while (readyChannel.receive() == false){
            delay(100)
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        val logId = response.header("X-Tt-Logid")
        Log.d("BytedanceAsrClient.onOpen", "===> onOpen,X-Tt-Logid:$logId")

        // send full client request
        // step 1: append payload json string
        val request = AsrRequest()
        request.audio.format = "pcm"
        request.audio.rate = sampleRate
        request.audio.channel = channels
        request.request.enableDDC = true
        request.request.resultType = "single"

        val payloadStr = gson.toJson(request)
        Log.d("BytedanceAsrClient", payloadStr)
        // step2: 压缩 payload 字段。
        val payloadBytes = payloadStr.toByteArray().let { gzipCompress(it, 0, it.size) }
        // step3:组装 fullClientRequest；fullClientRequest= header+ sequence + payload
        val header = getHeader(FULL_CLIENT_REQUEST, POS_SEQUENCE, JSON, GZIP, 0.toByte())
        val payloadSize = intToBytes(payloadBytes.size)
        seq = 1
        val seqBytes = generateBeforPayload(seq)
        val fullClientRequest = ByteArray(
            (header.size + seqBytes.size + payloadSize.size
                    + payloadBytes.size)
        )
        var destPos = 0
        System.arraycopy(header, 0, fullClientRequest, destPos, header.size)
        destPos += header.size
        System.arraycopy(seqBytes, 0, fullClientRequest, destPos, seqBytes.size)
        destPos += seqBytes.size
        System.arraycopy(payloadSize, 0, fullClientRequest, destPos, payloadSize.size)
        destPos += payloadSize.size
        System.arraycopy(payloadBytes, 0, fullClientRequest, destPos, payloadBytes.size)
        val suc: Boolean = webSocket.send(ByteString.of(*fullClientRequest))
        if (!suc) {
            Log.e("BytedanceAsrClient.onOpen", "send fullClientRequest failed")
        }else{
            Log.d("BytedanceAsrClient.onOpen", "send fullClientRequest success")
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        Log.d("BytedanceAsrClient", "===> onMessage： text:$text")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        val res: ByteArray = bytes.toByteArray()
        messageFlow.value = parserResponse(res)
        if (!_ready) {
            _ready = true
            runBlocking {
                readyChannel.send(_ready)
            }
        }
    }


    // audio_only_request= header + sequence + payload size+ payload
    fun sendAudioOnlyRequest(
        buffer: ByteArray,
        off: Int,
        len: Int,
        isLast: Boolean
    ): Boolean {
        seq++
        if (isLast) {
            seq = -seq
        }
        val messageTypeSpecificFlags = if (isLast) NEG_WITH_SEQUENCE else POS_SEQUENCE
        // header
        val header =
            getHeader(AUDIO_ONLY_REQUEST, messageTypeSpecificFlags, JSON, GZIP, 0.toByte())
        // sequence
        val sequenceBytes = generateBeforPayload(seq)
        // payload size
        val payloadBytes = gzipCompress(buffer, off, len)
        // payload
        val payloadSize = intToBytes(payloadBytes.size)
        val audioOnlyRequest = ByteArray(
            (header.size + sequenceBytes.size + payloadSize.size
                    + payloadBytes.size)
        )
        var destPos = 0
        System.arraycopy(header, 0, audioOnlyRequest, destPos, header.size)
        destPos += header.size
        System.arraycopy(sequenceBytes, 0, audioOnlyRequest, destPos, sequenceBytes.size)
        destPos += sequenceBytes.size
        System.arraycopy(payloadSize, 0, audioOnlyRequest, destPos, payloadSize.size)
        destPos += payloadSize.size
        System.arraycopy(payloadBytes, 0, audioOnlyRequest, destPos, payloadBytes.size)
        return webSocket.send(ByteString.of(*audioOnlyRequest))
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        Log.d("BytedanceAsrClient", "===> onClosing： code:$code reason:$reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        Log.d("BytedanceAsrClient", "===> onClosed： code:$code reason:$reason")
    }

    override fun onFailure(
        webSocket: WebSocket,
        t: Throwable,
        response: Response?
    ) {
        super.onFailure(webSocket, t, response)
        Log.d("BytedanceAsrClient.onFailure", "===> onFailure： Throwable:" + t.message + " Response:" + response?.toString())
        _ready = false
        runBlocking {
            readyChannel.send(_ready)
        }
    }

    fun getHeader(
        messageType: Byte,
        messageTypeSpecificFlags: Byte,
        serialMethod: Byte,
        compressionType: Byte,
        reservedData: Byte
    ): ByteArray {
        val header = ByteArray(4)
        header[0] =
            ((PROTOCOL_VERSION.toInt() shl 4) or DEFAULT_HEADER_SIZE.toInt()).toByte() // Protocol version|header size
        header[1] =
            ((messageType.toInt() shl 4) or messageTypeSpecificFlags.toInt()).toByte() // message type | messageTypeSpecificFlags
        header[2] = ((serialMethod.toInt() shl 4) or compressionType.toInt()).toByte()
        header[3] = reservedData
        return header
    }

    fun intToBytes(a: Int): ByteArray {
        return byteArrayOf(
            ((a shr 24) and 0xFF).toByte(),
            ((a shr 16) and 0xFF).toByte(),
            ((a shr 8) and 0xFF).toByte(),
            (a and 0xFF).toByte()

        )
    }

    fun bytesToInt(src: ByteArray): Int {
        require(src.size == 4) { "" }
        return (((src[0].toInt() and 0xFF) shl 24)
                or ((src[1].toInt() and 0xff) shl 16)
                or ((src[2].toInt() and 0xff) shl 8)
                or ((src[3].toInt() and 0xff)))
    }

    fun generateBeforPayload(seq: Int): ByteArray {
        return intToBytes(seq)
    }

    fun gzipCompress(src: ByteArray?, off: Int, len: Int): ByteArray {
        if (src == null || len == 0) {
            return ByteArray(0)
        }
        val out = ByteArrayOutputStream()
        var gzip: GZIPOutputStream? = null
        try {
            gzip = GZIPOutputStream(out)
            gzip.write(src, off, len)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (gzip != null) {
                try {
                    gzip.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return out.toByteArray()
    }

    fun gzipDecompress(src: ByteArray): ByteArray {
        if (src.isEmpty()) {
            return ByteArray(0)
        }
        val out = ByteArrayOutputStream()
        val ins = ByteArrayInputStream(src)
        var gzip: GZIPInputStream? = null
        try {
            gzip = GZIPInputStream(ins)
            val buffer = ByteArray(ins.available())
            var len = 0
            while ((gzip.read(buffer).also { len = it }) > 0) {
                out.write(buffer, 0, len)
            }
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (gzip != null) {
                try {
                    gzip.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        return out.toByteArray()
    }


    fun parserResponse(res: ByteArray): AsrStreamResponse {
        if (res.isEmpty()) return AsrStreamResponse()
        // 当符号位为1时进行 >> 运算后高位补1（预期是补0），导致结果错误，所以增加个数再与其& 运算，目的是确保高位是补0.
        val num: Byte = 15
        // header 32 bit=4 byte
        val protocolVersion = (res[0].toInt() shr 4) and num.toInt()
        val headerSize = res[0].toInt() and 0x0f

        val messageType = (res[1].toInt() shr 4) and num.toInt()
        val messageTypeSpecificFlags = res[1].toInt() and 0x0f
        val serializationMethod = res[2].toInt() shr num.toInt()
        val messageCompression = res[2].toInt() and 0x0f
        val reserved = res[3].toInt()

        // sequence 4 byte
        val temp = ByteArray(4)
        System.arraycopy(res, 4, temp, 0, temp.size)
        val sequence = bytesToInt(temp) // sequence 4 byte

        // payload size 4 byte
        var payloadStr: String? = null
        System.arraycopy(res, 8, temp, 0, temp.size)
        val payloadSize = bytesToInt(temp)
        val payloadData = ByteArray(res.size - 12)
        System.arraycopy(res, 12, payloadData, 0, payloadData.size)
        payloadStr = if (messageCompression == GZIP.toInt()) {
            String(gzipDecompress(payloadData))
        } else {
            String(payloadData)
        }
        Log.d("BytedanceAsrClient.parseResponse", payloadStr)
        return AsrStreamResponse(
            sequence,
            serializationMethod,
            protocolVersion,
            reserved,
            headerSize,
            messageCompression,
            messageType,
            payloadSize,
            messageTypeSpecificFlags,
            gson.fromJson<AsrResponse>(payloadStr, AsrResponse::class.java)
        )
    }


}