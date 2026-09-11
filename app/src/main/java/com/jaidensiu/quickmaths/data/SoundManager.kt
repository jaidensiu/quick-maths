package com.jaidensiu.quickmaths.data

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jaidensiu.quickmaths.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.common.AudioAttributes as Media3AudioAttributes

@Singleton
class SoundManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val nativeSfx: NativeSfxEngine,
    private val sfxDecoder: SfxPcmDecoder,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var isNativeSfxReady = false

    @Volatile
    private var isForegrounded = true

    private var pencilVoiceHandle = NativeSfxEngine.INVALID_HANDLE
    private var pencilStartedAtMs = 0L
    private var musicPlayer: ExoPlayer? = null
    private var pendingBackgroundStop: Job? = null

    init {
        scope.launch { initNativeSfx() }
    }

    fun playCorrect() {
        playOnce(sampleId = SAMPLE_CORRECT)
    }

    fun playTick() {
        playOnce(sampleId = SAMPLE_TICK)
    }

    fun startPencil() {
        if (pencilVoiceHandle != NativeSfxEngine.INVALID_HANDLE) {
            return
        }
        playPencilStream(volume = PENCIL_MIN_VOLUME, rate = 1f)
    }

    fun updatePencilSpeed(speedPxPerMs: Float) {
        if (pencilVoiceHandle == NativeSfxEngine.INVALID_HANDLE) {
            return
        }
        val intensity = (speedPxPerMs / PENCIL_FULL_SPEED_PX_PER_MS).coerceIn(0f, 1f)
        val volume = PENCIL_MIN_VOLUME + (PENCIL_MAX_VOLUME - PENCIL_MIN_VOLUME) * intensity
        val rate = 0.9f + 0.2f * intensity
        if (SystemClock.uptimeMillis() - pencilStartedAtMs >= PENCIL_RETRIGGER_MS) {
            playPencilStream(volume = volume, rate = rate)
        } else {
            nativeSfx.setVolume(handle = pencilVoiceHandle, volume = volume)
            nativeSfx.setRate(handle = pencilVoiceHandle, rate = rate)
        }
    }

    fun mutePencil() {
        if (pencilVoiceHandle != NativeSfxEngine.INVALID_HANDLE) {
            nativeSfx.setVolume(handle = pencilVoiceHandle, volume = 0f)
        }
    }

    fun stopPencil() {
        if (pencilVoiceHandle != NativeSfxEngine.INVALID_HANDLE) {
            nativeSfx.stop(handle = pencilVoiceHandle)
            pencilVoiceHandle = NativeSfxEngine.INVALID_HANDLE
        }
    }

    fun onAppForegrounded() {
        pendingBackgroundStop?.cancel()
        pendingBackgroundStop = null
        isForegrounded = true
        if (isNativeSfxReady) {
            nativeSfx.setForeground(foreground = true)
        }
    }

    fun onAppBackgrounded() {
        isForegrounded = false
        pendingBackgroundStop?.cancel()
        pendingBackgroundStop = scope.launch {
            delay(duration = STREAM_RELEASE_DELAY_MS.milliseconds)
            if (isNativeSfxReady && !isForegrounded) {
                nativeSfx.setForeground(foreground = false)
            }
        }
    }

    fun startMusic() {
        val player = musicPlayer ?: createMusicPlayer().also { musicPlayer = it }
        player.play()
    }

    fun pauseMusic() {
        musicPlayer?.pause()
    }

    private fun initNativeSfx() {
        if (!nativeSfx.isAvailable) {
            return
        }
        val audioManager = context.getSystemService(AudioManager::class.java)
        val deviceSampleRate = audioManager
            ?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 0
        val deviceFramesPerBurst = audioManager
            ?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 0
        if (!nativeSfx.init(
                defaultSampleRate = deviceSampleRate,
                defaultFramesPerBurst = deviceFramesPerBurst
            )
        ) {
            Log.w(TAG, "Oboe stream unavailable; SFX disabled")
            return
        }
        val samples = mapOf(
            SAMPLE_CORRECT to R.raw.sfx_correct,
            SAMPLE_TICK to R.raw.sfx_tick,
            SAMPLE_PENCIL to R.raw.sfx_pencil_scratch,
        )
        val loadedAll = samples.all { (sampleId, resId) ->
            val pcm = sfxDecoder.decode(resId = resId)
            pcm != null &&
                    nativeSfx.loadSample(sampleId, pcm.monoFrames, pcm.sampleRate)
        }
        if (!loadedAll) {
            Log.w(TAG, "SFX decode failed; SFX disabled")
            nativeSfx.setForeground(foreground = false)
            return
        }
        isNativeSfxReady = true
        nativeSfx.setForeground(foreground = isForegrounded)
        Log.i(TAG, "Native low-latency SFX engine ready")
    }

    private fun playOnce(sampleId: Int) {
        if (isNativeSfxReady) {
            nativeSfx.play(sampleId, SFX_VOLUME, 1f)
        }
    }

    private fun playPencilStream(volume: Float, rate: Float) {
        if (!isNativeSfxReady) {
            return
        }
        val handle = nativeSfx.play(SAMPLE_PENCIL, volume, rate)
        if (handle != NativeSfxEngine.INVALID_HANDLE) {
            pencilVoiceHandle = handle
            pencilStartedAtMs = SystemClock.uptimeMillis()
        }
    }

    private fun createMusicPlayer(): ExoPlayer =
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setUsage(C.USAGE_GAME)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.w(TAG, "Music playback error", error)
                }
            })
            setMediaItem(
                MediaItem.fromUri(
                    "android.resource://${context.packageName}/${R.raw.music_game}".toUri()
                )
            )
            repeatMode = Player.REPEAT_MODE_ONE
            volume = MUSIC_VOLUME
            prepare()
        }

    private companion object {
        const val TAG = "SoundManager"

        const val SAMPLE_CORRECT = 0
        const val SAMPLE_TICK = 1
        const val SAMPLE_PENCIL = 2

        const val SFX_VOLUME = 1f
        const val MUSIC_VOLUME = 0.35f
        const val PENCIL_MIN_VOLUME = 0.15f
        const val PENCIL_MAX_VOLUME = 0.9f
        const val PENCIL_FULL_SPEED_PX_PER_MS = 1.5f
        const val PENCIL_RETRIGGER_MS = 450L
        const val STREAM_RELEASE_DELAY_MS = 300L
    }
}
