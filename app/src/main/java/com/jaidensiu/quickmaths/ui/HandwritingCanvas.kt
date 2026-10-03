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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.input.motionprediction.MotionEventPredictor
import com.jaidensiu.quickmaths.domain.HandwritingPoint
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Low-latency handwriting surface.
 *
 * Every stroke is rendered by the ink library's front-buffered [InProgressStrokesView] (the wet
 * layer) and stays there, still "in progress" from the library's point of view, until the owner
 * drops it from [strokes]. There is no wet-to-dry handover per stroke, so nothing can flicker, and
 * dropping a stroke clears it on the next front-buffer draw. The Compose layer only draws strokes
 * the wet layer does not have, which happens when the canvas is recreated with existing strokes.
 */
@Composable
fun HandwritingCanvas(
    strokes: List<HandwritingStroke>,
    onStrokeFinished: (HandwritingStroke) -> Unit,
    modifier: Modifier = Modifier,
    onStrokeStarted: () -> Unit = {},
    onStrokeMoved: (speedPxPerMs: Float) -> Unit = {},
    onStrokeCancelled: () -> Unit = {},
    strokeWidth: Dp = 4.dp,
    strokeColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    // Ink brushes and views are backed by native code that the preview renderer cannot load.
    if (LocalInspectionMode.current) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            val style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            for (stroke in strokes) {
                drawPath(path = stroke.path, color = strokeColor, style = style)
            }
        }
        return
    }
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
            // This lambda re-runs whenever `strokes` changes, so it is where the wet layer learns
            // which strokes the owner has dropped.
            session.syncWithOwnerStrokes(strokes)
            val style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            for (stroke in strokes) {
                if (!session.isRenderedByWetLayer(stroke)) {
                    drawPath(path = stroke.path, color = strokeColor, style = style)
                }
            }
        }
        // One view for the whole composition. Recreating it is costly (surface + two hardware
        // buffers), and strokes written before the new view is ready are not visible.
        AndroidView(
            factory = session::createView,
            update = {
                session.brush = brush
                session.onStrokeStarted = onStrokeStarted
                session.onStrokeMoved = onStrokeMoved
                session.onStrokeFinished = onStrokeFinished
                session.onStrokeCancelled = onStrokeCancelled
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@SuppressLint("ClickableViewAccessibility")
private class InkStrokeSession : View.OnTouchListener {
    lateinit var brush: Brush
    var onStrokeStarted: () -> Unit = {}
    var onStrokeMoved: (speedPxPerMs: Float) -> Unit = {}
    var onStrokeFinished: (HandwritingStroke) -> Unit = {}
    var onStrokeCancelled: () -> Unit = {}

    private var view: InProgressStrokesView? = null
    private var predictor: MotionEventPredictor? = null
    private var strokeId: InProgressStrokeId? = null
    private var pointerId = 0
    private val points = mutableListOf<HandwritingPoint>()
    private var lastPosition = Offset.Zero
    private var lastMoveUptimeMillis = 0L

    /**
     * Lifted strokes the wet layer still renders, keyed by the object handed to the owner. They
     * are never passed to the library's finish/handoff pipeline, which would park them in the
     * front buffer for up to 500 ms after each handoff and then hand them to a slower renderer.
     */
    private val wetStrokes = IdentityHashMap<HandwritingStroke, InProgressStrokeId>()

    /** Lifted strokes not yet seen in the owner's list; they get a few frames before being judged dropped. */
    private val unconfirmedStrokes: MutableSet<HandwritingStroke> =
        Collections.newSetFromMap(IdentityHashMap())

    fun createView(context: Context): InProgressStrokesView {
        strokeId = null
        points.clear()
        wetStrokes.clear()
        unconfirmedStrokes.clear()
        return InProgressStrokesView(context).also {
            it.eagerInit()
            it.setOnTouchListener(this)
            predictor = MotionEventPredictor.newInstance(it)
            view = it
        }
    }

    fun isRenderedByWetLayer(stroke: HandwritingStroke): Boolean = wetStrokes.containsKey(stroke)

    /** Cancels wet strokes the owner has dropped from its list. */
    fun syncWithOwnerStrokes(ownerStrokes: List<HandwritingStroke>) {
        val view = view ?: return
        if (wetStrokes.isEmpty()) {
            return
        }
        unconfirmedStrokes.removeAll { stroke -> ownerStrokes.any { it === stroke } }
        val iterator = wetStrokes.entries.iterator()
        while (iterator.hasNext()) {
            val (stroke, id) = iterator.next()
            if (stroke in unconfirmedStrokes || ownerStrokes.any { it === stroke }) {
                continue
            }
            view.cancelStroke(strokeId = id)
            iterator.remove()
        }
    }

    /**
     * The owner normally adds a lifted stroke to its list within a frame, and the draw pass above
     * confirms it. If the list is cleared again before Compose draws (answer recognized within the
     * same frame), Compose sees an unchanged empty list, skips the draw, and the stroke would stay
     * wet forever. Treat a stroke still unconfirmed after a few frames as dropped.
     */
    private fun scheduleUnconfirmedStrokeCheck(view: View, stroke: HandwritingStroke) {
        view.postOnAnimation {
            view.postOnAnimation {
                view.postOnAnimation {
                    if (unconfirmedStrokes.remove(stroke)) {
                        wetStrokes.remove(stroke)?.let { this.view?.cancelStroke(strokeId = it) }
                    }
                }
            }
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
                val index = event.actionIndex
                if (event.getPointerId(index) != pointerId) {
                    return true
                }
                points.add(
                    HandwritingPoint(
                        x = event.getX(index),
                        y = event.getY(index),
                        timeMillis = event.eventTime,
                    )
                )
                // Deliberately not finishStroke(): the wet layer keeps rendering this stroke until
                // the owner drops it, see syncWithOwnerStrokes.
                val stroke = HandwritingStroke(path = points.toSmoothedPath(), points = points.toList())
                wetStrokes[stroke] = id
                unconfirmedStrokes.add(stroke)
                scheduleUnconfirmedStrokeCheck(view, stroke)
                strokeId = null
                onStrokeFinished(stroke)
            }

            MotionEvent.ACTION_CANCEL -> {
                val id = strokeId ?: return false
                view.cancelStroke(strokeId = id, event = event)
                strokeId = null
                points.clear()
                onStrokeCancelled()
            }

            else -> return false
        }
        return true
    }
}

internal fun List<HandwritingPoint>.toSmoothedPath(): Path {
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
