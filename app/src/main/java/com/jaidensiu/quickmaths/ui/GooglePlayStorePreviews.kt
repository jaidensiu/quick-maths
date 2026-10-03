package com.jaidensiu.quickmaths.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jaidensiu.quickmaths.domain.HandwritingPoint
import com.jaidensiu.quickmaths.domain.MathQuestion
import com.jaidensiu.quickmaths.domain.Operation
import com.jaidensiu.quickmaths.ui.theme.QuickMathsTheme
import kotlin.math.cos
import kotlin.math.sin

// Google Play Store screenshot sizes: 9:16 phone, 16:9 landscape for Android Automotive OS.
private const val PHONE = "spec:width=1080px,height=1920px,dpi=420"
private const val CAR = "spec:width=1920px,height=1080px,dpi=160"

@Preview(name = "Phone light", device = PHONE, showBackground = true)
@Preview(
    name = "Phone dark",
    device = PHONE,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class PhonePreviews

@Preview(name = "Car light", device = CAR, showBackground = true)
@Preview(
    name = "Car dark",
    device = CAR,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class CarPreviews

private val sampleQuestion = MathQuestion(left = 6, right = 7, operation = Operation.MULTIPLICATION)

private const val SAMPLE_TIME_MS = 19_840L

/** A handwritten "42", drawn with its top-left corner at ([x], [y]) in canvas pixels. */
private fun sampleAnswerStrokes(x: Float, y: Float): List<HandwritingStroke> = listOf(
    listOf(100f to 0f, 0f to 300f, 230f to 300f),
    listOf(170f to 60f, 170f to 440f),
    // Arc over the top of the "2", then the diagonal and the base.
    (200..380 step 15).map { degrees ->
        val radians = Math.toRadians(degrees.toDouble())
        370f + 100f * cos(radians).toFloat() to 110f + 100f * sin(radians).toFloat()
    } + listOf(270f to 420f, 500f to 420f),
).map { corners ->
    val points = corners.zipWithNext().flatMap { (from, to) ->
        (0 until 8).map { step ->
            val t = step / 8f
            HandwritingPoint(
                x = x + from.first + (to.first - from.first) * t,
                y = y + from.second + (to.second - from.second) * t,
                timeMillis = 0L,
            )
        }
    } + corners.last().let { HandwritingPoint(x = x + it.first, y = y + it.second, timeMillis = 0L) }
    HandwritingStroke(path = points.toSmoothedPath(), points = points)
}

@Composable
private fun PreviewStart() {
    QuickMathsTheme {
        StartContent(
            state = StartState(
                modelStatus = ModelStatus.READY,
                bestTimeMs = SAMPLE_TIME_MS,
                isParked = true,
            ),
            strokes = emptyList(),
            onStrokeFinished = {},
            onClear = {},
            onStartGame = {},
            onOpenSettings = {},
            onRetry = {},
        )
    }
}

@Composable
private fun PreviewGame(strokes: List<HandwritingStroke>) {
    QuickMathsTheme {
        GameContent(
            state = GameState(
                recognizedText = "42",
                question = sampleQuestion,
                questionNumber = 7,
                strokes = strokes,
            ),
            onPause = {},
            onResume = {},
            onExitGame = {},
            onClear = {},
            onStrokeStarted = {},
            onStrokeMoved = {},
            onStrokeFinished = {},
            onStrokeCancelled = {},
            onCanvasSizeChanged = {},
        )
    }
}

@Composable
private fun PreviewResults() {
    QuickMathsTheme {
        ResultsContent(
            elapsedTimeMs = SAMPLE_TIME_MS,
            bestTimeMs = SAMPLE_TIME_MS,
            totalQuestions = TOTAL_QUESTIONS,
            onPlayAgain = {},
            onBackToHome = {},
        )
    }
}

@PhonePreviews
@Composable
private fun PhoneStartPreview() = PreviewStart()

@PhonePreviews
@Composable
private fun PhoneGamePreview() = PreviewGame(strokes = sampleAnswerStrokes(x = 290f, y = 300f))

@PhonePreviews
@Composable
private fun PhoneResultsPreview() = PreviewResults()

@CarPreviews
@Composable
private fun CarStartPreview() = PreviewStart()

@CarPreviews
@Composable
private fun CarGamePreview() = PreviewGame(strokes = sampleAnswerStrokes(x = 330f, y = 260f))

@CarPreviews
@Composable
private fun CarResultsPreview() = PreviewResults()
