package com.jaidensiu.quickmaths.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass

@Composable
fun GameScreen(
    onGameFinished: (elapsedTimeMs: Long) -> Unit,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Swallow the back gesture; leaving the game is only possible via the pause menu.
    BackHandler(enabled = !state.isFinished) {}

    // Auto-pause when the app goes to the background so away-time doesn't count.
    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        viewModel.onPause()
    }

    LaunchedEffect(key1 = state.isFinished) {
        if (state.isFinished) {
            onGameFinished(state.elapsedTimeMs)
        }
    }

    GameContent(
        state = state,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        onExitGame = onExitGame,
        onClear = viewModel::onClear,
        onStrokeStarted = viewModel::onStrokeStarted,
        onStrokeMoved = viewModel::onStrokeMoved,
        onStrokeFinished = viewModel::onStrokeFinished,
        onStrokeCancelled = viewModel::onStrokeCancelled,
        onCanvasSizeChanged = viewModel::onCanvasSizeChanged,
        modifier = modifier,
    )
}

@Composable
fun GameContent(
    state: GameState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onExitGame: () -> Unit,
    onClear: () -> Unit,
    onStrokeStarted: () -> Unit,
    onStrokeMoved: (speedPxPerMs: Float) -> Unit,
    onStrokeFinished: (HandwritingStroke) -> Unit,
    onStrokeCancelled: () -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canvas: @Composable (Modifier) -> Unit = { canvasModifier ->
        HandwritingCanvas(
            strokes = state.strokes,
            onStrokeFinished = onStrokeFinished,
            onStrokeStarted = onStrokeStarted,
            onStrokeMoved = onStrokeMoved,
            onStrokeCancelled = onStrokeCancelled,
            modifier = canvasModifier.onSizeChanged(onCanvasSizeChanged),
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        when {
            state.isFinished -> Unit
            state.isPaused -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.displayMedium,
                    )
                    if (!state.canResume) {
                        Text(
                            text = "Park the vehicle to resume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    Button(
                        onClick = onResume,
                        enabled = state.canResume,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(text = "Resume")
                    }
                    TextButton(onClick = onExitGame) {
                        Text(text = "Exit")
                    }
                }
            }
            else -> {
                val isExpandedWidth = currentWindowAdaptiveInfo()
                    .windowSizeClass
                    .isWidthAtLeastBreakpoint(
                        widthDpBreakpoint = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
                    )

                Column(
                    modifier = Modifier.padding(paddingValues = innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${state.questionNumber} / ${state.totalQuestions}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(alignment = Alignment.Center),
                        )
                        TextButton(
                            onClick = onPause,
                            modifier = Modifier
                                .align(alignment = Alignment.CenterEnd)
                                .padding(end = 8.dp),
                        ) {
                            Text(text = "Pause")
                        }
                    }
                    if (isExpandedWidth) {
                        Row(modifier = Modifier.weight(weight = 1f)) {
                            Column(
                                modifier = Modifier
                                    .weight(weight = 2f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = state.question?.text.orEmpty(),
                                    style = MaterialTheme.typography.displayMedium,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Your answer:",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    Text(
                                        text = state.recognizedText,
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                                TextButton(
                                    onClick = onClear,
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                            }
                            canvas(Modifier.weight(weight = 3f))
                        }
                    } else {
                        Text(
                            text = state.question?.text.orEmpty(),
                            style = MaterialTheme.typography.displayMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Your answer:",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = state.recognizedText,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                            Spacer(modifier = Modifier.weight(weight = 1f))
                            TextButton(
                                onClick = onClear,
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                        canvas(Modifier.weight(weight = 1f))
                    }
                }
            }
        }
    }
}
