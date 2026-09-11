package com.jaidensiu.quickmaths.data

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeSfxEngine @Inject constructor() {
    val isAvailable: Boolean get() = isLibraryLoaded

    fun init(defaultSampleRate: Int, defaultFramesPerBurst: Int): Boolean {
        if (!isLibraryLoaded) {
            return false
        }
        return try {
            nativeInit(defaultSampleRate, defaultFramesPerBurst)
        } catch (error: UnsatisfiedLinkError) {
            Log.w(TAG, "Native SFX symbols failed to resolve; SFX disabled", error)
            false
        }
    }

    fun loadSample(sampleId: Int, monoFrames: FloatArray, sampleRate: Int): Boolean =
        isLibraryLoaded && nativeLoadSample(sampleId, monoFrames, sampleRate)

    fun play(sampleId: Int, volume: Float, rate: Float): Int =
        if (isLibraryLoaded) nativePlay(sampleId, volume, rate) else INVALID_HANDLE

    fun setVolume(handle: Int, volume: Float) {
        if (isLibraryLoaded) {
            nativeSetVolume(handle, volume)
        }
    }

    fun setRate(handle: Int, rate: Float) {
        if (isLibraryLoaded) {
            nativeSetRate(handle, rate)
        }
    }

    fun stop(handle: Int) {
        if (isLibraryLoaded) {
            nativeStop(handle)
        }
    }

    fun setForeground(foreground: Boolean) {
        if (isLibraryLoaded) {
            nativeSetForeground(foreground)
        }
    }

    private external fun nativeInit(defaultSampleRate: Int, defaultFramesPerBurst: Int): Boolean
    private external fun nativeLoadSample(
        sampleId: Int,
        monoFrames: FloatArray,
        sampleRate: Int,
    ): Boolean

    private external fun nativePlay(sampleId: Int, volume: Float, rate: Float): Int
    private external fun nativeSetVolume(handle: Int, volume: Float)
    private external fun nativeSetRate(handle: Int, rate: Float)
    private external fun nativeStop(handle: Int)
    private external fun nativeSetForeground(foreground: Boolean)

    companion object {
        const val INVALID_HANDLE = -1
        private const val TAG = "NativeSfxEngine"

        private val isLibraryLoaded: Boolean = try {
            System.loadLibrary("quickmaths_audio")
            true
        } catch (error: UnsatisfiedLinkError) {
            Log.w(TAG, "Native audio library unavailable; SFX disabled", error)
            false
        }
    }
}
