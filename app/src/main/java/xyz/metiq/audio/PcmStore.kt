package xyz.metiq.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32

internal data class Pcm(
    val data: ShortArray,
    val sampleRate: Int,
    val channelMask: Int,
    val channelCount: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
object PcmStore {
    // Raw little-endian header: magic, asset crc32, sample rate, channel count,
    // sample count — then the samples themselves.
    private const val CACHE_MAGIC = 0x4D504331 // "MPC1"
    private const val CACHE_HEADER_BYTES = 5 * 4
    val NOISE_ASSETS = mapOf(
        "pink" to "audio/noise/pink.ogg",
        "brown" to "audio/noise/brown.ogg",
        "white" to "audio/noise/white.ogg",
        "grey" to "audio/noise/grey.ogg",
    )

    val AMBIENT_ASSETS = mapOf(
        "seawaves" to "audio/ambient/seawaves.ogg",
        "rain" to "audio/ambient/rain.ogg",
        "fire" to "audio/ambient/fire.ogg",
        "birds" to "audio/ambient/birds.ogg",
        "cafe" to "audio/ambient/cafe.ogg",
        "wind" to "audio/ambient/wind.ogg",
        "crickets" to "audio/ambient/crickets.ogg",
        "stream" to "audio/ambient/stream.ogg",
    )

    private val cache = ConcurrentHashMap<String, Deferred<Pcm>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val decodeDispatcher = Dispatchers.IO.limitedParallelism(2)

    @Volatile
    var noiseReady = false
        private set

    private fun pcmAsync(context: Context, id: String, assetPath: String): Deferred<Pcm> {
        val appContext = context.applicationContext
        return cache.computeIfAbsent(id) {
            scope.async(decodeDispatcher) { loadPcm(appContext, id, assetPath) }
        }
    }

    private fun loadPcm(context: Context, id: String, assetPath: String): Pcm {
        val crc = assetCrc32(context, assetPath)
        val file = File(File(context.filesDir, "pcm"), "$id.pcm")
        readCachedPcm(file, crc)?.let { return it }
        val pcm = decodeAssetToPcm(context, assetPath)
        runCatching { writeCachedPcm(file, crc, pcm) }
        return pcm
    }

    private fun assetCrc32(context: Context, assetPath: String): Int {
        val crc = CRC32()
        context.assets.open(assetPath).use { ins ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                crc.update(buf, 0, n)
            }
        }
        return crc.value.toInt()
    }

    private fun readCachedPcm(file: File, crc: Int): Pcm? = runCatching {
        if (!file.isFile) return null
        val buf = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        if (buf.remaining() < CACHE_HEADER_BYTES) return null
        if (buf.int != CACHE_MAGIC || buf.int != crc) return null
        val sampleRate = buf.int
        val channelCount = buf.int
        val sampleCount = buf.int
        if (sampleCount < 0 || buf.remaining() != sampleCount * 2) return null
        val shorts = ShortArray(sampleCount)
        buf.asShortBuffer().get(shorts)
        Pcm(
            data = shorts,
            sampleRate = sampleRate,
            channelMask = channelMaskFor(channelCount),
            channelCount = channelCount,
        )
    }.getOrNull()

    private fun writeCachedPcm(file: File, crc: Int, pcm: Pcm) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        val bytes = ByteBuffer.allocate(CACHE_HEADER_BYTES + pcm.data.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        bytes.putInt(CACHE_MAGIC).putInt(crc)
            .putInt(pcm.sampleRate).putInt(pcm.channelCount).putInt(pcm.data.size)
        bytes.asShortBuffer().put(pcm.data)
        tmp.writeBytes(bytes.array())
        if (!tmp.renameTo(file)) {
            tmp.delete()
            error("Could not move ${tmp.name} into place")
        }
    }

    internal suspend fun awaitPcm(context: Context, id: String, assetPath: String): Pcm {
        val deferred = pcmAsync(context, id, assetPath)
        return try {
            deferred.await()
        } catch (e: Throwable) {
            if (deferred.isCompleted) cache.remove(id, deferred)
            throw e
        }
    }

    private suspend fun preload(context: Context, id: String, assetPath: String) {
        try {
            awaitPcm(context, id, assetPath)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
        }
    }

    fun preloadAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            for ((id, path) in NOISE_ASSETS) preload(appContext, id, path)
            noiseReady = true
            for ((id, path) in AMBIENT_ASSETS) preload(appContext, id, path)
            sweepStaleCacheFiles(appContext)
        }
    }

    // Deletes cache files that no current sound owns — renamed/removed ids
    // and half-written .tmp files from crashes.
    private fun sweepStaleCacheFiles(context: Context) {
        val valid = (NOISE_ASSETS.keys + AMBIENT_ASSETS.keys).map { "$it.pcm" }.toSet()
        File(context.filesDir, "pcm").listFiles()?.forEach { f ->
            if (f.name !in valid) runCatching { f.delete() }
        }
    }

    private fun decodeAssetToPcm(context: Context, assetPath: String): Pcm {
        val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
        val extractor = MediaExtractor()
        extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()

        val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        } ?: error("No audio track in $assetPath")
        extractor.selectTrack(trackIndex)
        val inputFormat = extractor.getTrackFormat(trackIndex)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(inputFormat, null, null, 0)
        codec.start()

        val info = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        val pcmBytes = java.io.ByteArrayOutputStream()

        while (!sawOutputEos) {
            if (!sawInputEos) {
                val inIx = codec.dequeueInputBuffer(10_000)
                if (inIx >= 0) {
                    val inBuf: ByteBuffer = codec.getInputBuffer(inIx)!!
                    val read = extractor.readSampleData(inBuf, 0)
                    if (read < 0) {
                        codec.queueInputBuffer(inIx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEos = true
                    } else {
                        codec.queueInputBuffer(inIx, 0, read, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIx = codec.dequeueOutputBuffer(info, 10_000)
            if (outIx >= 0) {
                val outBuf: ByteBuffer = codec.getOutputBuffer(outIx)!!
                outBuf.position(info.offset)
                outBuf.limit(info.offset + info.size)
                val chunk = ByteArray(info.size)
                outBuf.get(chunk)
                pcmBytes.write(chunk)
                codec.releaseOutputBuffer(outIx, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEos = true
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val bytes = pcmBytes.toByteArray()
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

        return Pcm(
            data = shorts,
            sampleRate = sampleRate,
            channelMask = channelMaskFor(channelCount),
            channelCount = channelCount,
        )
    }

    private fun channelMaskFor(channelCount: Int): Int = when (channelCount) {
        1 -> AudioFormat.CHANNEL_OUT_MONO
        2 -> AudioFormat.CHANNEL_OUT_STEREO
        else -> error("Unsupported channel count: $channelCount")
    }
}
