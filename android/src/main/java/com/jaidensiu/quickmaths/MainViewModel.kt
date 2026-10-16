package com.jaidensiu.quickmaths

import androidx.lifecycle.ViewModel
import com.jaidensiu.quickmaths.data.SoundManager
import com.jaidensiu.quickmaths.data.ThemeRepository
import com.jaidensiu.quickmaths.domain.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    themeRepository: ThemeRepository,
    private val soundManager: SoundManager,
) : ViewModel() {
    val theme: StateFlow<ThemePreference> = themeRepository.theme

    private var isAppForegrounded = false
    private var isMusicAllowed = true

    fun onAppForegrounded() {
        isAppForegrounded = true
        soundManager.onAppForegrounded()
        updateMusic()
    }

    fun onAppBackgrounded() {
        isAppForegrounded = false
        updateMusic()
        soundManager.stopPencil()
        soundManager.onAppBackgrounded()
    }

    fun onMusicAllowedChanged(allowed: Boolean) {
        isMusicAllowed = allowed
        updateMusic()
    }

    private fun updateMusic() {
        if (isAppForegrounded && isMusicAllowed) {
            soundManager.startMusic()
        } else {
            soundManager.pauseMusic()
        }
    }
}
