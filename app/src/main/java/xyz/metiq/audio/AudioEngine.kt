package xyz.metiq.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val FADE_STEP_MS = 10L
private const val MAX_FADE_MILLIS = 30_000L
private const val NOISE_BLOCK_FRAMES = 1024
private const val WARMTH_GLIDE = 0.2f
private const val SYNTH_SAMPLE_RATE = 48000
private const val SYNTH_BLOCK_FRAMES = 1024
private const val SYNTH_GLIDE = 0.08
private const val SYNTH_AMPLITUDE = 0.5

const val BINAURAL_ID = "binaural"

@OptIn(ExperimentalCoroutinesApi::class)
class AudioEngine(private val context: Context) {
    private data class Layer(
        val track: AudioTrack,
        var volume: Float,
        val warmthEligible: Boolean,
        val feeder: Job? = null,
    ) {
        @Volatile
        var fadeFactor: Float = 1f
        var fadeJob: Job? = null
    }

    private val layers = ConcurrentHashMap<String, Layer>()
    private val startJobs = ConcurrentHashMap<String, Job>()
    private val feederScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var masterVolume: Float = 1f
    private val fadeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var fadeJob: Job? = null

    @Volatile
    private var warmth: Float = 0f

    @Volatile
    private var carrierHz: Float = 216f

    @Volatile
    private var beatHz: Float = 10f

    @Volatile
    private var fadeMillis: Long = 0L

    @Volatile
    private var timerFadeMillis: Long = 0L

    private fun fadeSteps(ms: Long = fadeMillis): Int = (ms / FADE_STEP_MS).toInt()


    private fun fadeInGain(progress: Float): Float = sqrt(progress.coerceIn(0f, 1f))

    private fun fadeOutGain(progress: Float): Float =
        if (progress >= 1f) 0f else 10f.pow(-3f * progress.coerceAtLeast(0f))

    private fun appliedVolume(l: Layer): Float =
        (l.volume * masterVolume * l.fadeFactor).coerceIn(0f, 1f)

    fun startLayer(
        id: String,
        assetPath: String,
        volume: Float = 1f,
        warmthEligible: Boolean = false,
    ) {
        if (layers.containsKey(id) || startJobs.containsKey(id)) return
        val job = feederScope.launch {
            val pcm = try {
                PcmStore.awaitPcm(context, id, assetPath)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                return@launch
            }
            val track = withContext(Dispatchers.IO) { buildStreamTrack(pcm, 0f) }
            if (!isActive) {
                runCatching { track.release() }
                return@launch
            }
            val feeder = launchFeeder(track, pcm, filtered = warmthEligible)
            val layer = Layer(track, volume, warmthEligible, feeder)
            layer.fadeFactor = 0f
            layers[id] = layer
            rampIn(layer)
        }
        job.invokeOnCompletion { startJobs.remove(id, job) }
        startJobs[id] = job
    }

    private fun rampIn(l: Layer): Job {
        l.fadeJob?.cancel()
        val job = fadeScope.launch {
            val from = l.fadeFactor
            val steps = fadeSteps()
            for (i in 1..steps) {
                l.fadeFactor = from + (1f - from) * fadeInGain(i.toFloat() / steps)
                runCatching { l.track.setVolume(appliedVolume(l)) }
                delay(FADE_STEP_MS)
            }
            l.fadeFactor = 1f
            runCatching { l.track.setVolume(appliedVolume(l)) }
        }
        l.fadeJob = job
        return job
    }

    private fun warmthCutoffHz(w: Float): Float {
        val fMax = 18000.0
        val fMin = 2200.0
        return (fMax * (fMin / fMax).pow(w.coerceIn(0f, 1f).toDouble())).toFloat()
    }

    private fun buildStreamTrack(pcm: Pcm, effective: Float): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            pcm.sampleRate, pcm.channelMask, AudioFormat.ENCODING_PCM_16BIT,
        )
        val desired = NOISE_BLOCK_FRAMES * pcm.channelCount * 2 * 4
        val t = buildTrack(pcm.sampleRate, pcm.channelMask, maxOf(minBuf, desired), stream = true)
        t.setVolume(effective)
        return t
    }

    private fun launchFeeder(track: AudioTrack, pcm: Pcm, filtered: Boolean): Job =
        feederScope.launch {
            val src = pcm.data
            val ch = pcm.channelCount
            val frames = src.size / ch
            if (frames == 0) {
                runCatching { track.release() }
                return@launch
            }
            val sr = pcm.sampleRate
            val fcMax = (sr * 0.45f)
            val block = NOISE_BLOCK_FRAMES
            val total = block * ch
            val buf = ShortArray(total)
            val x1 = DoubleArray(ch); val x2 = DoubleArray(ch)
            val y1 = DoubleArray(ch); val y2 = DoubleArray(ch)
            var fc = warmthCutoffHz(warmth).coerceIn(2200f, fcMax)
            var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0
            
            fun recompute(f: Double) {
                val w0 = 2.0 * PI * f / sr
                val cw = cos(w0)
                val alpha = sin(w0) / (2.0 * 0.70710678)
                val a0 = 1.0 + alpha
                b0 = ((1.0 - cw) / 2.0) / a0
                b1 = (1.0 - cw) / a0
                b2 = b0
                a1 = (-2.0 * cw) / a0
                a2 = (1.0 - alpha) / a0
            }
            
            var pos = 0
            var started = false
            try {
                while (isActive) {
                    if (filtered) {
                        val target = warmthCutoffHz(warmth).coerceIn(2200f, fcMax)
                        fc += (target - fc) * WARMTH_GLIDE
                        recompute(fc.toDouble())
                        for (f in 0 until block) {
                            val base = pos * ch
                            for (c in 0 until ch) {
                                val x0 = src[base + c].toDouble()
                                val y0 = b0 * x0 + b1 * x1[c] + b2 * x2[c] - a1 * y1[c] - a2 * y2[c]
                                x2[c] = x1[c]; x1[c] = x0; y2[c] = y1[c]; y1[c] = y0
                                buf[f * ch + c] = y0.coerceIn(-32768.0, 32767.0).toInt().toShort()
                            }
                            pos++
                            if (pos >= frames) pos = 0
                        }
                    } else {
                        var f = 0
                        while (f < block) {
                            val n = minOf(block - f, frames - pos)
                            System.arraycopy(src, pos * ch, buf, f * ch, n * ch)
                            pos += n
                            f += n
                            if (pos >= frames) pos = 0
                        }
                    }
                    var off = 0
                    while (off < total && isActive) {
                        val n = track.write(buf, off, total - off, AudioTrack.WRITE_NON_BLOCKING)
                        if (n < 0) return@launch
                        off += n
                        if (!started && off > 0) {
                            track.play()
                            started = true
                        }
                        if (off < total) delay(5)
                    }
                }
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }

    fun setWarmth(value: Float) {
        warmth = value.coerceIn(0f, 1f)
    }

    fun setFadeMillis(ms: Long) {
        fadeMillis = ms.coerceIn(0L, MAX_FADE_MILLIS)
    }

    fun setTimerFadeMillis(ms: Long) {
        timerFadeMillis = ms.coerceIn(0L, MAX_FADE_MILLIS)
    }

    fun setBinaural(carrierHz: Float, beatHz: Float) {
        this.carrierHz = carrierHz.coerceIn(20f, 1500f)
        this.beatHz = beatHz.coerceIn(0f, 50f)
    }

    fun startBinaural(volume: Float = 1f) {
        val id = BINAURAL_ID
        if (layers.containsKey(id) || startJobs.containsKey(id)) return
        val job = feederScope.launch {
            val track = withContext(Dispatchers.IO) { buildSynthTrack() }
            if (!isActive) {
                runCatching { track.release() }
                return@launch
            }
            val feeder = launchSynthFeeder(track)
            val layer = Layer(track, volume, warmthEligible = false, feeder = feeder)
            layer.fadeFactor = 0f
            layers[id] = layer
            rampIn(layer)
        }
        job.invokeOnCompletion { startJobs.remove(id, job) }
        startJobs[id] = job
    }

    fun stopBinaural() = stopLayer(BINAURAL_ID)

    private fun buildSynthTrack(): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            SYNTH_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val desired = SYNTH_BLOCK_FRAMES * 2 * 2 * 4 // frames * stereo * 16-bit * headroom
        val t = buildTrack(
            SYNTH_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, maxOf(minBuf, desired), stream = true,
        )
        t.setVolume(0f)
        return t
    }

    private fun launchSynthFeeder(track: AudioTrack): Job =
        feederScope.launch {
            val sr = SYNTH_SAMPLE_RATE
            val block = SYNTH_BLOCK_FRAMES
            val total = block * 2
            val buf = ShortArray(total)
            val amp = SYNTH_AMPLITUDE * Short.MAX_VALUE
            val twoPi = 2.0 * PI
            var phaseL = 0.0
            var phaseR = 0.0
            var curCarrier = carrierHz.toDouble()
            var curBeat = beatHz.toDouble()
            var started = false
            try {
                while (isActive) {
                    curCarrier += (carrierHz - curCarrier) * SYNTH_GLIDE
                    curBeat += (beatHz - curBeat) * SYNTH_GLIDE
                    val incL = twoPi * curCarrier / sr
                    val incR = twoPi * (curCarrier + curBeat) / sr
                    for (f in 0 until block) {
                        buf[f * 2] = (sin(phaseL) * amp).toInt().toShort()
                        buf[f * 2 + 1] = (sin(phaseR) * amp).toInt().toShort()
                        phaseL += incL; if (phaseL >= twoPi) phaseL -= twoPi
                        phaseR += incR; if (phaseR >= twoPi) phaseR -= twoPi
                    }
                    var off = 0
                    while (off < total && isActive) {
                        val n = track.write(buf, off, total - off, AudioTrack.WRITE_NON_BLOCKING)
                        if (n < 0) return@launch
                        off += n
                        if (!started && off > 0) {
                            track.play()
                            started = true
                        }
                        if (off < total) delay(5)
                    }
                }
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }

    fun stopLayer(id: String, fadeMs: Long = fadeMillis) {
        startJobs.remove(id)?.cancel()
        val l = layers.remove(id) ?: return
        l.fadeJob?.cancel()
        fadeScope.launch {
            val from = l.fadeFactor
            val steps = fadeSteps(fadeMs)
            for (i in 1..steps) {
                l.fadeFactor = from * fadeOutGain(i.toFloat() / steps)
                runCatching { l.track.setVolume(appliedVolume(l)) }
                delay(FADE_STEP_MS)
            }
            if (l.feeder != null) {
                l.feeder.cancel()
            } else {
                runCatching { l.track.stop() }
                runCatching { l.track.release() }
            }
        }
    }

    fun setLayerVolume(id: String, volume: Float) {
        val l = layers[id] ?: return
        l.volume = volume
        l.track.setVolume(appliedVolume(l))
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        for ((_, l) in layers) {
            l.track.setVolume(appliedVolume(l))
        }
    }

    fun pauseAll(fade: Boolean = true) {
        fadeJob?.cancel()
        if (!fade) {
            for ((_, l) in layers) runCatching { l.track.pause() }
            return
        }
        fadeJob = fadeScope.launch {
            val steps = fadeSteps()
            for (i in 1..steps) {
                val factor = fadeOutGain(i.toFloat() / steps)
                for ((_, l) in layers) {
                    runCatching { l.track.setVolume(appliedVolume(l) * factor) }
                }
                delay(FADE_STEP_MS)
            }
            for ((_, l) in layers) {
                runCatching { l.track.pause() }
                runCatching { l.track.setVolume(appliedVolume(l)) }
            }
        }
    }

    fun resumeAll() {
        fadeJob?.cancel()
        fadeJob = fadeScope.launch {
            for ((_, l) in layers) {
                runCatching {
                    l.track.setVolume(0f)
                    l.track.play()
                }
            }
            val steps = fadeSteps()
            for (i in 1..steps) {
                val factor = fadeInGain(i.toFloat() / steps)
                for ((_, l) in layers) {
                    runCatching { l.track.setVolume(appliedVolume(l) * factor) }
                }
                delay(FADE_STEP_MS)
            }
            for ((_, l) in layers) {
                runCatching { l.track.setVolume(appliedVolume(l)) }
            }
        }
    }

    fun release() {
        fadeJob?.cancel()
        startJobs.values.forEach { it.cancel() }
        startJobs.clear()
        for ((_, l) in layers) {
            l.fadeJob?.cancel()
            if (l.feeder != null) {
                l.feeder.cancel()
            } else {
                runCatching { l.track.stop() }
                runCatching { l.track.release() }
            }
        }
        layers.clear()
    }

    fun activeLayerIds(): Set<String> = layers.keys.toSet()

    fun layerVolume(id: String): Float? = layers[id]?.volume

    suspend fun switchTo(newId: String, newAssetPath: String) {
        layers.keys.filter { it != newId && it != BINAURAL_ID }.forEach { stopLayer(it) }
        if (!layers.containsKey(newId)) {
            startLayer(newId, newAssetPath, volume = 1f, warmthEligible = true)
        } else {
            setLayerVolume(newId, 1f)
        }
    }

    fun stopAll() {
        layers.keys.toList().forEach { stopLayer(it) }
    }

    fun stopAllTimerFade() {
        layers.keys.toList().forEach { stopLayer(it, timerFadeMillis) }
    }

    private fun buildTrack(
        sampleRate: Int,
        channelMask: Int,
        bufferBytes: Int,
        stream: Boolean = false,
    ): AudioTrack {
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate).setChannelMask(channelMask).build()
        val mode = if (stream) AudioTrack.MODE_STREAM else AudioTrack.MODE_STATIC
        return AudioTrack.Builder().setAudioAttributes(attrs).setAudioFormat(format)
            .setBufferSizeInBytes(bufferBytes).setTransferMode(mode)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }
}
