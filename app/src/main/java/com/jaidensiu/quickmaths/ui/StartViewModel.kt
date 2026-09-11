package com.jaidensiu.quickmaths.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.jaidensiu.quickmaths.data.BestTimeRepository
import com.jaidensiu.quickmaths.data.NetworkMonitor
import com.jaidensiu.quickmaths.data.NumberRecognizer
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
            runCatching { recognizer.prepare() }
                .onSuccess { _state.update { it.copy(modelStatus = ModelStatus.READY) } }
                .onFailure {
                    val status = if (networkMonitor.isCurrentlyOnline()) {
                        ModelStatus.ERROR
                    } else {
                        ModelStatus.OFFLINE
                    }
                    _state.update { it.copy(modelStatus = status) }
                }
        }
    }
}
