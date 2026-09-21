package xyz.metiq.audio

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(markerClass = [UnstableApi::class])
class EnginePlayer(
    private val engine: AudioEngine,
    looper: Looper,
    private val artwork: ByteArray,
) : SimpleBasePlayer(looper) {

    private var playing = false
    private var stopped = true
    private var currentVolume = 1f
    private var activeLabel: String? = null

    fun setActiveLabel(label: String?) {
        activeLabel = label
        invalidateState()
    }

    fun notifyPausedExternally() {
        if (!playing) return
        playing = false
        // Instant pause: this fires on audio route loss (e.g. headphones
        // unplugged), where a fade would leak sound out of the speaker.
        engine.pauseAll(fade = false)
        invalidateState()
    }

    fun notifyStopped() {
        playing = false
        stopped = true
        invalidateState()
    }

    override fun getState(): State {
        val title = activeLabel ?: "Metiq"
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist("Metiq")
            .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .build()
        val item = MediaItemData.Builder("metiq")
            .setMediaItem(
                MediaItem.Builder()
                    .setMediaId("metiq")
                    .setMediaMetadata(metadata)
                    .build()
            )
            .setDurationUs(C.TIME_UNSET)
            .setIsPlaceholder(false)
            .build()
        val commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_RELEASE,
                Player.COMMAND_SET_VOLUME,
                Player.COMMAND_GET_VOLUME,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TIMELINE,
            )
            .build()
        return State.Builder()
            .setAvailableCommands(commands)
            .setPlayWhenReady(playing, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(if (stopped) Player.STATE_IDLE else Player.STATE_READY)
            .setVolume(currentVolume)
            .setPlaylist(listOf(item))
            .setCurrentMediaItemIndex(0)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        playing = playWhenReady
        if (playWhenReady) {
            stopped = false
            engine.resumeAll()
        } else {
            engine.pauseAll()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetVolume(
        volume: Float,
        volumeFlags: Int,
    ): ListenableFuture<*> {
        currentVolume = volume.coerceIn(0f, 1f)
        engine.setMasterVolume(currentVolume)
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        playing = false
        stopped = true
        engine.pauseAll()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        engine.release()
        return Futures.immediateVoidFuture()
    }
}
