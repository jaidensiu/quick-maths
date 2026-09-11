package com.jaidensiu.quickmaths.data

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import com.jaidensiu.quickmaths.domain.HandwritingPoint
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NumberRecognizer @Inject constructor() {
    private val model = DigitalInkRecognitionModel
        .builder(DigitalInkRecognitionModelIdentifier.EN_US)
        .build()
    private val recognizer = DigitalInkRecognition.getClient(
        DigitalInkRecognizerOptions.builder(model).build(),
    )
    private val prepareMutex = Mutex()
    private var isPrepared = false

    suspend fun isModelDownloaded(): Boolean {
        return isPrepared || RemoteModelManager.getInstance().isModelDownloaded(model).await()
    }

    suspend fun prepare() {
        prepareMutex.withLock {
            if (isPrepared) {
                return
            }
            RemoteModelManager.getInstance()
                .download(model, DownloadConditions.Builder().build())
                .await()
            warmUp()
            isPrepared = true
        }
    }

    suspend fun recognize(
        strokes: List<List<HandwritingPoint>>,
        writingAreaWidth: Float? = null,
        writingAreaHeight: Float? = null,
    ): String {
        val ink = Ink.builder().apply {
            strokes.forEach { points ->
                addStroke(
                    Ink.Stroke.builder().apply {
                        points.forEach {
                            addPoint(Ink.Point.create(it.x, it.y, it.timeMillis))
                        }
                    }.build()
                )
            }
        }.build()
        val context = RecognitionContext.builder()
            .setPreContext("")
            .apply {
                if (writingAreaWidth != null && writingAreaHeight != null) {
                    setWritingArea(WritingArea(writingAreaWidth, writingAreaHeight))
                }
            }
            .build()
        val candidates = recognizer.recognize(ink, context).await().candidates
        return candidates.firstNotNullOfOrNull { candidate ->
            candidate.text.asNumberOrNull()
        } ?: candidates.firstOrNull()?.text?.filter(predicate = Char::isDigit).orEmpty()
    }

    private fun String.asNumberOrNull(): String? {
        val normalized = this
            .filterNot(predicate = Char::isWhitespace)
            .map { DIGIT_LOOKALIKES[it] ?: it }
            .joinToString(separator = "")
        return normalized.takeIf { it.matches(regex = NUMBER_REGEX) }
    }

    private suspend fun warmUp() {
        val stroke = Ink.Stroke.builder().apply {
            addPoint(Ink.Point.create(0f, 0f, 0L))
            addPoint(Ink.Point.create(1f, 1f, 16L))
        }.build()
        recognizer.recognize(Ink.builder().addStroke(stroke).build()).await()
    }

    private companion object {
        val NUMBER_REGEX = Regex(pattern = "-?\\d+([.,]\\d+)?")
        val DIGIT_LOOKALIKES = mapOf(
            'O' to '0', 'o' to '0', 'D' to '0',
            'l' to '1', 'I' to '1', 'i' to '1', '|' to '1', '!' to '1',
            'Z' to '2', 'z' to '2',
            'S' to '5', 's' to '5',
            'G' to '6', 'b' to '6',
            'B' to '8',
            'g' to '9', 'q' to '9',
        )
    }
}
