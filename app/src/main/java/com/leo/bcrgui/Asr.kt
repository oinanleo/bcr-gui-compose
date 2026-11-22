package com.leo.bcrgui

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 端侧转写：sherpa-onnx + SenseVoice-Small（int8）。
 * 模型不进 APK，第一次转写时下载到 filesDir/sensevoice（先 HF，失败再走 hf-mirror）。
 */
object Asr {
    private const val MODEL_FILE = "model.int8.onnx"
    private const val TOKENS_FILE = "tokens.txt"
    private const val MODEL_SIZE = 239233841L
    private const val TOKENS_SIZE = 315894L
    private const val REPO = "csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17"

    private fun dir(ctx: Context) = File(ctx.filesDir, "sensevoice")

    fun isReady(ctx: Context): Boolean {
        val d = dir(ctx)
        return File(d, MODEL_FILE).length() == MODEL_SIZE && File(d, TOKENS_FILE).length() == TOKENS_SIZE
    }

    suspend fun transcribe(
        ctx: Context,
        uri: Uri,
        onStatus: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        ensureModel(ctx, onStatus)
        onStatus("解码音频…")
        val samples = decode(ctx, uri)
        require(samples.size > 1600) { "这段录音解出来是空的" }
        onStatus("识别中…（${samples.size / 16000} 秒）")

        val d = dir(ctx)
        val config = OfflineRecognizerConfig(
            modelConfig = OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = File(d, MODEL_FILE).absolutePath,
                    language = "auto",
                    useInverseTextNormalization = true,
                ),
                tokens = File(d, TOKENS_FILE).absolutePath,
                numThreads = 2,
                debug = false,
                provider = "cpu",
            ),
        )
        val rec = OfflineRecognizer(config = config)
        try {
            val chunk = 16000 * 30
            val sb = StringBuilder()
            var off = 0
            while (off < samples.size) {
                val end = minOf(off + chunk, samples.size)
                val piece = samples.copyOfRange(off, end)
                val stream = rec.createStream()
                stream.acceptWaveform(piece, 16000)
                rec.decode(stream)
                val text = rec.getResult(stream).text.trim()
                stream.release()
                if (text.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(text)
                }
                off = end
            }
            sb.toString().ifBlank { "（没识别出内容）" }
        } finally {
            rec.release()
        }
    }

    private fun ensureModel(ctx: Context, onStatus: (String) -> Unit) {
        val d = dir(ctx)
        d.mkdirs()
        val files = listOf(MODEL_FILE to MODEL_SIZE, TOKENS_FILE to TOKENS_SIZE)
        for ((name, size) in files) {
            val f = File(d, name)
            if (f.length() == size) continue
            onStatus("下载识别模型 $name")
            download(name, f) { p -> onStatus("下载识别模型 $name  ${(p * 100).toInt()}%") }
        }
    }

    private fun download(name: String, target: File, onProgress: (Float) -> Unit) {
        val tmp = File(target.parentFile, "$name.part")
        var last = System.currentTimeMillis()
        val sources = listOf(
            "https://huggingface.co/$REPO/resolve/main/$name",
            "https://hf-mirror.com/$REPO/resolve/main/$name",
        )
        for (url in sources) {
            val ok = runCatching {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 20000
                conn.readTimeout = 60000
                conn.setRequestProperty("User-Agent", "bcrgui")
                if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                val total = conn.contentLengthLong
                conn.inputStream.use { ins ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(256 * 1024)
                        var done = 0L
                        while (true) {
                            val read = ins.read(buf)
                            if (read <= 0) break
                            out.write(buf, 0, read)
                            done += read
                            if (total > 0) {
                                val now = System.currentTimeMillis()
                                if (now - last > 300) {
                                    last = now
                                    onProgress(done.toFloat() / total)
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
                tmp.renameTo(target)
            }.isSuccess
            if (ok) return
        }
        error("识别模型下载失败，检查网络后重试")
    }

    private fun decode(ctx: Context, uri: Uri): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(ctx, uri, null)
        var track = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                track = i
                format = f
                break
            }
        }
        require(track >= 0 && format != null) { "这条录音里没有音频轨" }
        extractor.selectTrack(track)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val srcRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val srcCh = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val baos = ByteArrayOutputStream(1 shl 22)
        val info = MediaCodec.BufferInfo()
        var sawEos = false
        while (!sawEos) {
            val inIdx = codec.dequeueInputBuffer(10_000)
            if (inIdx >= 0) {
                val buf = codec.getInputBuffer(inIdx)!!
                val sz = extractor.readSampleData(buf, 0)
                if (sz < 0) {
                    codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    sawEos = true
                } else {
                    codec.queueInputBuffer(inIdx, 0, sz, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
            var outIdx = codec.dequeueOutputBuffer(info, 10_000)
            while (outIdx >= 0) {
                if (info.size > 0) {
                    val out = codec.getOutputBuffer(outIdx)!!
                    val chunk = ByteArray(info.size)
                    out.position(info.offset)
                    out.get(chunk)
                    baos.write(chunk)
                }
                codec.releaseOutputBuffer(outIdx, false)
                outIdx = codec.dequeueOutputBuffer(info, 0)
            }
        }
        codec.stop()
        codec.release()
        extractor.release()

        val bytes = baos.toByteArray()
        val frames = bytes.size / (2 * srcCh)
        require(frames > 0) { "解码结果为空" }
        val mono = FloatArray(frames)
        for (i in 0 until frames) {
            var acc = 0f
            for (c in 0 until srcCh) {
                val idx = (i * srcCh + c) * 2
                val lo = bytes[idx].toInt() and 0xFF
                val hi = bytes[idx + 1].toInt()
                acc += ((hi shl 8) or lo).toShort() / 32768f
            }
            mono[i] = acc / srcCh
        }
        if (srcRate == 16000) return mono
        val outN = (frames.toLong() * 16000 / srcRate).toInt()
        val out = FloatArray(outN)
        for (i in 0 until outN) {
            val pos = i.toFloat() * srcRate / 16000f
            val i0 = pos.toInt().coerceIn(0, frames - 1)
            val i1 = (i0 + 1).coerceAtMost(frames - 1)
            val fr = pos - i0
            out[i] = mono[i0] * (1 - fr) + mono[i1] * fr
        }
        return out
    }
}
