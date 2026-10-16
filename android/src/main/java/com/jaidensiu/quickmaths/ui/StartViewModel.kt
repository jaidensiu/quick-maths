package com.jaidensiu.quickmaths.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaidensiu.quickmaths.data.BestTimeRepository
import com.jaidensiu.quickmaths.data.GearMonitor
import com.jaidensiu.quickmaths.data.NetworkMonitor
import com.jaidensiu.quickmaths.data.NumberRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val recognizer: NumberRecognizer,
    private val bestTimeRepository: BestTimeRepository,
    private val networkMonitor: NetworkMonitor,
    gearMonitor: GearMonitor,
) : ViewModel() {
    private val _state = MutableStateFlow(value = StartState())
    val state: StateFlow<StartState> = _state.asStateFlow()

    init {
        prepareModel()
        viewModelScope.launch {
            bestTimeRepository.bestTimeMs.collect { bestTimeMs ->
                _state.update { it.copy(bestTimeMs = bestTimeMs) }
            }
        }
        // Injecting the monitor here also makes the car service connect while the user is still
        // on the start screen, so the gear is known before the first game begins.
        viewModelScope.launch {
            gearMonitor.isParked.collect { parked ->
                _state.update { it.copy(isParked = parked) }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                when {
                    online && _state.value.modelStatus == ModelStatus.OFFLINE -> prepareModel()
                    // ML Kit's download task doesn't fail when the network drops;
                    // it silently waits, so surface the offline state ourselves.
                    !online && _state.value.modelStatus == ModelStatus.LOADING &&
                            !recognizer.isModelDownloaded() ->
                        _state.update { it.copy(modelStatus = ModelStatus.OFFLINE) }
                }
            }
        }
    }

    fun onRetry() {
        if (_state.value.modelStatus == ModelStatus.ERROR) {
            prepareModel()
        }
    }

    private fun prepareModel() {
        viewModelScope.launch {
            if (!recognizer.isModelDownloaded() && !networkMonitor.isCurrentlyOnline()) {
                _state.update { it.copy(modelStatus = ModelStatus.OFFLINE) }
                return@launch
            }
            _state.update { it.copy(modelStatus = ModelStatus.LOADING) }
            val status = try {
                recognizer.prepare()
                ModelStatus.READY
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val online = networkMonitor.isCurrentlyOnline()
                Log.w(TAG, "Handwriting model preparation failed (online=$online)", error)
                if (online) ModelStatus.ERROR else ModelStatus.OFFLINE
            }
            _state.update { it.copy(modelStatus = status) }
        }
    }

    private companion object {
        const val TAG = "StartViewModel"
    }
}
