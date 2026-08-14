package xyz.metiq.audio

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(markerClass = [UnstableApi::class])
class PlaybackService : MediaSessionService() {
    private lateinit var engine: AudioEngine
    private lateinit var player: EnginePlayer
    private var session: MediaSession? = null
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private var focusHeld = false

    @Volatile
    private var requestFocusEnabled = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null
    private val timerRemainingSeconds = MutableStateFlow<Long?>(null)

    inner class EngineBinder : Binder() {
        val engine: AudioEngine get() = this@PlaybackService.engine
        val timerRemainingSeconds: StateFlow<Long?>
            get() = this@PlaybackService.timerRemainingSeconds
        fun setActiveColor(label: String?, tintArgb: Int?) {
            this@PlaybackService.player.setActiveColor(label, tintArgb)
        }
        fun requestAudioFocusNow(): Boolean = this@PlaybackService.requestAudioFocus()
        fun setRequestAudioFocus(enabled: Boolean) =
            this@PlaybackService.setRequestAudioFocus(enabled)
        fun startSleepTimer(seconds: Long) = this@PlaybackService.startSleepTimer(seconds)
        fun cancelSleepTimer() = this@PlaybackService.cancelSleepTimer()
    }

    private val engineBinder = EngineBinder()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hardStop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player.notifyPausedExternally()
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_IDLE) {
                abandonAudioFocus()
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) requestAudioFocus()
        }
    }

    override fun onCreate() {
        super.onCreate()
        engine = AudioEngine(this)
        player = EnginePlayer(engine, mainLooper)
        player.addListener(playerListener)
        session = MediaSession.Builder(this, player).build()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        PcmStore.preloadAll(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == ENGINE_BIND_ACTION) return engineBinder
        return super.onBind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        hardStop()
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        abandonAudioFocus()
        session?.run {
            player.removeListener(playerListener)
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private fun startSleepTimer(seconds: Long) {
        timerJob?.cancel()
        if (seconds <= 0L) {
            timerRemainingSeconds.value = null
            return
        }
        timerRemainingSeconds.value = seconds
        timerJob = serviceScope.launch {
            val deadline = SystemClock.elapsedRealtime() + seconds * 1000L
            while (true) {
                val leftMs = deadline - SystemClock.elapsedRealtime()
                if (leftMs <= 0L) break
                timerRemainingSeconds.value = (leftMs + 999L) / 1000L
                delay(minOf(1000L, leftMs))
            }
            engine.stopAllTimerFade()
            player.setActiveColor(null, null)
            player.stop()
            timerRemainingSeconds.value = null
        }
    }

    private fun cancelSleepTimer() {
        timerJob?.cancel()
        timerJob = null
        timerRemainingSeconds.value = null
    }

    private fun hardStop() {
        cancelSleepTimer()
        engine.release()
        player.setActiveColor(null, null)
        player.notifyStopped()
        abandonAudioFocus()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    private fun requestAudioFocus(): Boolean {
        if (!requestFocusEnabled) return true
        if (focusHeld) return true
        val result = audioManager.requestAudioFocus(focusRequest)
        focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return focusHeld
    }

    private fun abandonAudioFocus() {
        if (!focusHeld) return
        audioManager.abandonAudioFocusRequest(focusRequest)
        focusHeld = false
    }

    private fun setRequestAudioFocus(enabled: Boolean) {
        if (requestFocusEnabled == enabled) return
        requestFocusEnabled = enabled
        if (enabled) {
            if (engine.activeLayerIds().isNotEmpty()) requestAudioFocus()
        } else {
            abandonAudioFocus()
        }
    }

    companion object {
        const val ENGINE_BIND_ACTION = "xyz.metiq.audio.ENGINE"
    }
}
