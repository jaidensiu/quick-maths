package com.jaidensiu.quickmaths.ui.game

import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaidensiu.quickmaths.data.audio.SoundManager
import com.jaidensiu.quickmaths.data.device.GearMonitor
import com.jaidensiu.quickmaths.data.handwriting.NumberRecognizer
import com.jaidensiu.quickmaths.data.prefs.BestTimeRepository
import com.jaidensiu.quickmaths.domain.MathQuestion
import com.jaidensiu.quickmaths.ui.components.HandwritingStroke
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val recognizer: NumberRecognizer,
    private val bestTimeRepository: BestTimeRepository,
    private val soundManager: SoundManager,
    gearMonitor: GearMonitor,
) : ViewModel() {
    private val _state = MutableStateFlow(value = GameState(question = MathQuestion.random()))
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val startTimeMs = SystemClock.elapsedRealtime()
    private var totalPausedMs = 0L
    private var pauseStartedAtMs = 0L
    private var writingArea: IntSize? = null
    private var recognitionJob: Job? = null
    private var pencilIdleJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                recognizer.prepare()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Recognizer preparation failed; recognition may be unavailable", error)
            }
        }

        viewModelScope.launch {
            gearMonitor.isParked.collect { parked ->
                // Fail closed: an unknown gear state (null) blocks play just like driving does.
                val canResume = parked == true
                _state.update { it.copy(canResume = canResume) }
                if (!canResume) {
                    onPause()
                }
            }
        }
    }

    fun onCanvasSizeChanged(size: IntSize) {
        writingArea = size
    }

    fun onStrokeStarted() {
        recognitionJob?.cancel()
        soundManager.startPencil()
        scheduleIdleMute()
    }

    fun onStrokeMoved(speedPxPerMs: Float) {
        if (speedPxPerMs < PENCIL_MIN_SPEED_PX_PER_MS) {
            return
        }
        soundManager.updatePencilSpeed(speedPxPerMs = speedPxPerMs)
        scheduleIdleMute()
    }

    private fun scheduleIdleMute() {
        pencilIdleJob?.cancel()
        pencilIdleJob = viewModelScope.launch {
            delay(timeMillis = PENCIL_IDLE_TIMEOUT_MS)
            soundManager.mutePencil()
        }
    }

    fun onStrokeFinished(stroke: HandwritingStroke) {
        stopPencil()
        if (_state.value.isPaused || _state.value.isFinished) {
            return
        }
        val strokes = _state.value.strokes + stroke
        _state.update { it.copy(strokes = strokes) }
        recognize(strokes = strokes)
    }

    /** Re-reads every stroke written so far, superseding any read still in flight. */
    private fun recognize(strokes: List<HandwritingStroke>) {
        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch {
            val text = try {
                recognizer.recognize(
                    strokes = strokes.map { it.points },
                    writingAreaWidth = writingArea?.width?.toFloat(),
                    writingAreaHeight = writingArea?.height?.toFloat(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Handwriting recognition failed for ${strokes.size} stroke(s)", error)
                _state.update { it.copy(recognizedText = "?") }
                return@launch
            }
            _state.update { it.copy(recognizedText = text.ifBlank { "?" }) }
            checkAnswer(text = text)
        }
    }

    fun onStrokeCancelled() {
        stopPencil()
    }

    fun onPause() {
        if (_state.value.isPaused || _state.value.isFinished) {
            return
        }
        pauseStartedAtMs = SystemClock.elapsedRealtime()
        recognitionJob?.cancel()
        stopPencil()
        _state.update { it.copy(isPaused = true) }
    }

    fun onResume() {
        val current = _state.value
        if (!current.isPaused || !current.canResume) {
            return
        }
        totalPausedMs += SystemClock.elapsedRealtime() - pauseStartedAtMs
        _state.update { it.copy(isPaused = false) }
        // onPause cancelled any read in flight; re-read what's on the canvas.
        if (current.strokes.isNotEmpty()) {
            recognize(strokes = current.strokes)
        }
    }

    fun onClear() {
        recognitionJob?.cancel()
        _state.update { it.copy(strokes = emptyList(), recognizedText = "") }
    }

    private fun checkAnswer(text: String) {
        val question = _state.value.question ?: return
        if (text.trim() != question.answer.toString()) {
            return
        }
        _state.update { current ->
            if (current.questionNumber >= current.totalQuestions) {
                current.copy(
                    question = null,
                    isFinished = true,
                    elapsedTimeMs = SystemClock.elapsedRealtime() - startTimeMs - totalPausedMs,
                    recognizedText = "",
                    strokes = emptyList(),
                )
            } else {
                current.copy(
                    question = MathQuestion.random(),
                    questionNumber = current.questionNumber + 1,
                    recognizedText = "",
                    strokes = emptyList(),
                )
            }
        }
        val finished = _state.value
        if (finished.isFinished) {
            bestTimeRepository.recordTime(timeMs = finished.elapsedTimeMs)
        } else {
            soundManager.playCorrect()
        }
    }

    private fun stopPencil() {
        pencilIdleJob?.cancel()
        soundManager.stopPencil()
    }

    override fun onCleared() {
        stopPencil()
    }

    private companion object {
        const val TAG = "GameViewModel"
        const val PENCIL_MIN_SPEED_PX_PER_MS = 0.01f
        const val PENCIL_IDLE_TIMEOUT_MS = 50L
    }
}
