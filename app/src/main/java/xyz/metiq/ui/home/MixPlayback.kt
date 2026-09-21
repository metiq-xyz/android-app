package xyz.metiq.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import xyz.metiq.R
import xyz.metiq.audio.BINAURAL_ID
import xyz.metiq.audio.PlaybackService

private const val FALLBACK_BINAURAL_BAND = "alpha"

@Stable
internal class MixPlayback {
    val levels = mutableStateMapOf<String, Float>()

    var binder by mutableStateOf<PlaybackService.EngineBinder?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    private var controller: MediaController? = null

    internal var binauralDefaultVolume = DEFAULT_SOUND_LEVEL
    internal var onBinauralBand: (String) -> Unit = {}
    internal var onBinauralVolume: (Float) -> Unit = {}

    fun toggle(id: String) {
        if (id in levels) {
            remove(id)
        } else {
            val level = if (id in BINAURAL_BAND_IDS) {
                levels.entries.firstOrNull { it.key in BINAURAL_BAND_IDS }?.value ?: binauralDefaultVolume
            } else {
                DEFAULT_SOUND_LEVEL
            }
            add(id, level)
        }
    }

    fun setLevel(id: String, level: Float) {
        if (id !in levels) return
        levels[id] = level
        binder?.engine?.setLayerVolume(engineId(id), level)
    }

    fun settleLevel(id: String) {
        val level = levels[id] ?: return
        when {
            level <= 0f -> remove(id)
            id in BINAURAL_BAND_IDS -> onBinauralVolume(level)
        }
    }

    fun applyMix(mix: Map<String, Float>) {
        val valid = mix.filter { (id, level) -> level > 0f && isKnownSound(id) }
        if (valid.isEmpty() || binder == null) return
        if (!mixMatches(levels, valid)) {
            levels.keys.filter { it !in valid }.forEach { remove(it, stopWhenEmpty = false) }
            valid.forEach { (id, level) -> if (id in levels) setLevel(id, level) else add(id, level) }
        }
        ensurePlaying()
    }

    /** Removes every sound and stops playback. */
    fun clear() {
        if (levels.isEmpty()) return
        levels.keys.toList().forEach { remove(it, stopWhenEmpty = false) }
        controller?.stop()
        playing = false
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (levels.isEmpty()) return
        if (c.playWhenReady) {
            c.pause()
            playing = false
        } else {
            ensurePlaying()
        }
    }

    private fun add(id: String, level: Float) {
        val engine = binder?.engine ?: return
        when (id) {
            in NOISE_IDS -> engine.startLayer(id, "audio/noise/$id.ogg", level, warmthEligible = true)
            in AMBIENT_IDS -> engine.startLayer(id, "audio/ambient/$id.ogg", level)
            in BINAURAL_BAND_IDS -> {
                val band = BINAURAL_BANDS.first { it.id == id }
                val current = levels.keys.firstOrNull { it in BINAURAL_BAND_IDS }
                engine.setBinaural(BINAURAL_CARRIER_HZ, band.beatHz)
                if (current != null) {
                    // Retune the running synth instead of restarting it.
                    levels.remove(current)
                    engine.setLayerVolume(BINAURAL_ID, level)
                } else {
                    engine.startBinaural(level)
                }
                onBinauralBand(id)
            }

            else -> return
        }
        levels[id] = level
        ensurePlaying()
    }

    private fun remove(id: String, stopWhenEmpty: Boolean = true) {
        levels.remove(id) ?: return
        binder?.engine?.stopLayer(engineId(id))
        if (stopWhenEmpty && levels.isEmpty()) {
            controller?.stop()
            playing = false
        }
    }

    private fun ensurePlaying() {
        val c = controller ?: return
        binder?.requestAudioFocusNow()
        if (!c.playWhenReady) c.play()
        playing = true
    }

    internal fun attach(bound: PlaybackService.EngineBinder, savedBand: String?) {
        binder = bound
        val engine = bound.engine
        val ids = engine.activeLayerIds()
        ids.filter { it in NOISE_IDS || it in AMBIENT_IDS }.forEach { id ->
            if (id !in levels) levels[id] = engine.layerVolume(id) ?: DEFAULT_SOUND_LEVEL
        }
        if (BINAURAL_ID in ids && levels.keys.none { it in BINAURAL_BAND_IDS }) {
            val band = savedBand?.takeIf { it in BINAURAL_BAND_IDS } ?: FALLBACK_BINAURAL_BAND
            levels[band] = engine.layerVolume(BINAURAL_ID) ?: binauralDefaultVolume
        }
    }

    internal fun detach() {
        binder = null
    }

    internal fun attachController(c: MediaController?) {
        controller = c
        playing = c != null && isPlaying(c)
    }

    internal fun onPlayerChanged(c: Player) {
        playing = isPlaying(c)
    }

    private fun isPlaying(c: Player): Boolean =
        c.playWhenReady && c.playbackState != Player.STATE_IDLE

    internal fun onPlaybackStateChanged(state: Int) {
        if (
            state == Player.STATE_IDLE &&
            controller?.playWhenReady != true &&
            binder?.engine?.activeLayerIds()?.isEmpty() == true
        ) {
            levels.clear()
        }
    }

    private fun engineId(id: String): String = if (id in BINAURAL_BAND_IDS) BINAURAL_ID else id

    private fun isKnownSound(id: String): Boolean =
        id in NOISE_IDS || id in AMBIENT_IDS || id in BINAURAL_BAND_IDS
}

@Composable
internal fun rememberMixPlayback(
    savedBinauralBand: String?,
    binauralVolume: Float,
    onBinauralBand: (String) -> Unit,
    onBinauralVolume: (Float) -> Unit,
): MixPlayback {
    val context = LocalContext.current
    val playback = remember { MixPlayback() }
    val latestSavedBand by rememberUpdatedState(savedBinauralBand)
    SideEffect {
        playback.binauralDefaultVolume = binauralVolume
        playback.onBinauralBand = onBinauralBand
        playback.onBinauralVolume = onBinauralVolume
    }

    DisposableEffect(Unit) {
        var controllerFuture: ListenableFuture<MediaController>? = null
        var connectedController: MediaController? = null
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                    playback.onPlaybackStateChanged(player.playbackState)
                }
                playback.onPlayerChanged(player)
            }
        }

        fun releaseController() {
            controllerFuture?.let { f ->
                if (connectedController == null) MediaController.releaseFuture(f)
            }
            controllerFuture = null
            connectedController?.removeListener(listener)
            connectedController?.release()
            connectedController = null
            playback.attachController(null)
        }

        fun connectController() {
            releaseController()
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            controllerFuture = future
            future.addListener({
                if (controllerFuture !== future) return@addListener // superseded by a reconnect
                val c = future.get()
                connectedController = c
                playback.attachController(c)
                c.addListener(listener)
            }, ContextCompat.getMainExecutor(context))
        }

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, b: IBinder?) {
                playback.attach(b as PlaybackService.EngineBinder, latestSavedBand)
                connectController()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playback.detach()
            }
        }
        val bindIntent = Intent(context, PlaybackService::class.java)
            .setAction(PlaybackService.ENGINE_BIND_ACTION)
        context.bindService(bindIntent, conn, Context.BIND_AUTO_CREATE)

        onDispose {
            releaseController()
            context.unbindService(conn)
        }
    }
    return playback
}

@Composable
internal fun mixLabel(levels: Map<String, Float>, mixTitle: String?): String? {
    if (levels.isEmpty()) return null
    if (mixTitle != null) return mixTitle
    if (levels.size == 1) {
        val id = levels.keys.first()
        NOISE_COLORS.firstOrNull { it.id == id }?.let { return stringResource(it.noiseTitleRes) }
        AMBIENT_SOUNDS.firstOrNull { it.id == id }?.let { return stringResource(it.labelRes) }
        BINAURAL_BANDS.firstOrNull { it.id == id }?.let {
            return stringResource(R.string.binaural_noti_label, stringResource(it.labelRes))
        }
    }
    return stringResource(R.string.mix_now_playing)
}

@Composable
internal fun mixSoundNames(levels: Map<String, Float>): List<String> = buildList {
    NOISE_COLORS.filter { it.id in levels }.forEach { add(stringResource(it.noiseTitleRes)) }
    AMBIENT_SOUNDS.filter { it.id in levels }.forEach { add(stringResource(it.labelRes)) }
    BINAURAL_BANDS.filter { it.id in levels }.forEach { add(stringResource(R.string.binaural_noti_label, stringResource(it.labelRes))) }
}
