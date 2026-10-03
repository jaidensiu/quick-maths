package com.jaidensiu.quickmaths.ui

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaidensiu.quickmaths.R
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
private const val FEATURE_GRAPHIC = "spec:width=1024px,height=500px,dpi=160"

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

private val sampleQuestion = MathQuestion(left = 8, right = 8, operation = Operation.MULTIPLICATION)

private const val SAMPLE_TIME_MS = 19_840L

/** A handwritten "64", drawn with its top-left corner at ([x], [y]) in canvas pixels. */
private fun sampleAnswerStrokes(x: Float, y: Float): List<HandwritingStroke> = listOf(
    // Sweep down the left of the "6", then once around its loop.
    listOf(170f to 0f, 110f to 50f, 55f to 130f, 20f to 230f) +
        (180 downTo -180 step 10).map { degrees ->
            val radians = Math.toRadians(degrees.toDouble())
            110f + 100f * cos(radians).toFloat() to 330f + 100f * sin(radians).toFloat()
        },
    listOf(390f to 0f, 290f to 300f, 520f to 300f),
    listOf(460f to 60f, 460f to 440f),
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
                recognizedText = "64",
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

/** Google Play Store feature graphic: 1024x500, no transparency. */
@Preview(name = "Feature graphic light", device = FEATURE_GRAPHIC, showBackground = true)
@Preview(
    name = "Feature graphic dark",
    device = FEATURE_GRAPHIC,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FeatureGraphicPreview() {
    QuickMathsTheme {
        val ink = MaterialTheme.colorScheme.onBackground
        val strokes = sampleAnswerStrokes(x = 0f, y = 0f)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(horizontal = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(
                space = 40.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.sad_man_playing_qm),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = ink),
                modifier = Modifier.width(width = 300.dp),
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_play_store),
                        contentDescription = null,
                        modifier = Modifier
                            .size(size = 96.dp)
                            .clip(shape = RoundedCornerShape(percent = 22))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(percent = 22),
                            ),
                    )
                    Text(
                        text = "Quick Maths",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            lineHeight = 72.sp,
                        ),
                        color = ink,
                        modifier = Modifier.padding(start = 24.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(
                        text = sampleQuestion.text,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Canvas(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(width = 80.dp, height = 72.dp),
                    ) {
                        scale(scale = 0.16f, pivot = Offset.Zero) {
                            for (stroke in strokes) {
                                drawPath(
                                    path = stroke.path,
                                    color = ink,
                                    style = Stroke(
                                        width = 28f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
