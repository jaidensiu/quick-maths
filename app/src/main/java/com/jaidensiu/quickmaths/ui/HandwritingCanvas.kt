package com.jaidensiu.quickmaths.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.input.motionprediction.MotionEventPredictor
import com.jaidensiu.quickmaths.domain.HandwritingPoint
import androidx.ink.strokes.Stroke as InkStroke

@Composable
fun HandwritingCanvas(
    strokes: List<HandwritingStroke>,
    onStrokeFinished: (HandwritingStroke) -> Unit,
    modifier: Modifier = Modifier,
    onStrokeStarted: () -> Unit = {},
    onStrokeMoved: (speedPxPerMs: Float) -> Unit = {},
    clearKey: Int = 0,
    strokeWidth: Dp = 4.dp,
    strokeColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val brush = remember(strokeColor, strokeWidthPx) {
        Brush.createWithColorIntArgb(
            family = StockBrushes.marker(),
            colorIntArgb = strokeColor.toArgb(),
            size = strokeWidthPx,
            epsilon = 0.1f,
        )
    }
    val session = remember { InkStrokeSession() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            strokes.forEach { drawPath(path = it.path, color = strokeColor, style = style) }
        }
        key(clearKey) {
            AndroidView(
                factory = session::createView,
                update = {
                    session.brush = brush
                    session.onStrokeStarted = onStrokeStarted
                    session.onStrokeMoved = onStrokeMoved
                    session.onStrokeFinished = onStrokeFinished
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
private class InkStrokeSession : View.OnTouchListener, InProgressStrokesFinishedListener {
    lateinit var brush: Brush
    var onStrokeStarted: () -> Unit = {}
    var onStrokeMoved: (speedPxPerMs: Float) -> Unit = {}
    var onStrokeFinished: (HandwritingStroke) -> Unit = {}

    private var view: InProgressStrokesView? = null
    private var predictor: MotionEventPredictor? = null
    private var strokeId: InProgressStrokeId? = null
    private var pointerId = 0
    private val points = mutableListOf<HandwritingPoint>()
    private var lastPosition = Offset.Zero
    private var lastMoveUptimeMillis = 0L

    fun createView(context: Context): InProgressStrokesView {
        strokeId = null
        points.clear()
        return InProgressStrokesView(context).also {
            it.eagerInit()
            it.addFinishedStrokesListener(this)
            it.setOnTouchListener(this)
            predictor = MotionEventPredictor.newInstance(it)
            view = it
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val view = view ?: return false
        if (v !== view) {
            return false
        }
        predictor?.record(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                view.requestUnbufferedDispatch(event)
                pointerId = event.getPointerId(event.actionIndex)
                strokeId = view.startStroke(event = event, pointerId = pointerId, brush = brush)
                points.clear()
                points.add(
                    HandwritingPoint(x = event.x, y = event.y, timeMillis = event.eventTime)
                )
                lastPosition = Offset(x = event.x, y = event.y)
                lastMoveUptimeMillis = event.eventTime
                onStrokeStarted()
            }

            MotionEvent.ACTION_MOVE -> {
                val id = strokeId ?: return false
                val index = event.findPointerIndex(pointerId)
                if (index < 0) {
                    return true
                }
                val previousPosition = lastPosition
                val elapsedMs = event.eventTime - lastMoveUptimeMillis
                for (h in 0 until event.historySize) {
                    points.add(
                        HandwritingPoint(
                            x = event.getHistoricalX(index, h),
                            y = event.getHistoricalY(index, h),
                            timeMillis = event.getHistoricalEventTime(h),
                        )
                    )
                }
                val position = Offset(x = event.getX(index), y = event.getY(index))
                points.add(
                    HandwritingPoint(x = position.x, y = position.y, timeMillis = event.eventTime)
                )
                view.addToStroke(
                    event = event,
                    pointerId = pointerId,
                    strokeId = id,
                    prediction = predictor?.predict(),
                )
                lastPosition = position
                if (elapsedMs > 0) {
                    onStrokeMoved((position - previousPosition).getDistance() / elapsedMs)
                    lastMoveUptimeMillis = event.eventTime
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = strokeId ?: return false
                if (event.getPointerId(event.actionIndex) != pointerId) {
                    return true
                }
                view.finishStroke(event = event, pointerId = pointerId, strokeId = id)
                strokeId = null
                onStrokeFinished(
                    HandwritingStroke(path = points.toSmoothedPath(), points = points.toList())
                )
            }

            MotionEvent.ACTION_CANCEL -> {
                val id = strokeId ?: return false
                view.cancelStroke(strokeId = id, event = event)
                strokeId = null
                points.clear()
            }

            else -> return false
        }
        return true
    }

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, InkStroke>) {
        view?.removeFinishedStrokes(strokes.keys)
    }
}

private fun List<HandwritingPoint>.toSmoothedPath(): Path {
    val path = Path()
    val first = firstOrNull() ?: return path
    path.moveTo(x = first.x, y = first.y)
    var last = first
    for (i in 1 until size) {
        val point = this[i]
        path.quadraticTo(
            x1 = last.x,
            y1 = last.y,
            x2 = (last.x + point.x) / 2f,
            y2 = (last.y + point.y) / 2f,
        )
        last = point
    }
    path.lineTo(x = last.x, y = last.y)
    return path
}
