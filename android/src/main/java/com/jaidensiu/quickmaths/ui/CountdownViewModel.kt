package com.jaidensiu.quickmaths.ui

import androidx.lifecycle.ViewModel
import com.jaidensiu.quickmaths.data.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CountdownViewModel @Inject constructor(
    private val soundManager: SoundManager,
) : ViewModel() {
    fun onCountShown() {
        soundManager.playTick()
    }
}
