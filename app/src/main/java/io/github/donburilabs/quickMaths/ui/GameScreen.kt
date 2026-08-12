package io.github.donburilabs.quickMaths.ui

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
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
    val strokes = remember { mutableStateListOf<HandwritingStroke>() }

    // Swallow the back gesture; leaving the game is only possible via the pause menu.
    BackHandler(enabled = !state.isFinished) {}

    // Auto-pause when the app goes to the background so away-time doesn't count.
    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        viewModel.onPause()
    }

    LaunchedEffect(key1 = state.canvasClearKey) {
        strokes.clear()
    }

    LaunchedEffect(key1 = state.isFinished) {
        if (state.isFinished) {
            onGameFinished(state.elapsedTimeMs)
        }
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
                    Button(
                        onClick = viewModel::onResume,
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
                            onClick = viewModel::onPause,
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
                                    onClick = {
                                        strokes.clear()
                                        viewModel.onClear()
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                            }
                            GameHandwritingCanvas(
                                strokes = strokes,
                                viewModel = viewModel,
                                clearKey = state.canvasClearKey,
                                modifier = Modifier.weight(weight = 3f),
                            )
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
                                onClick = {
                                    strokes.clear()
                                    viewModel.onClear()
                                },
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                        GameHandwritingCanvas(
                            strokes = strokes,
                            viewModel = viewModel,
                            clearKey = state.canvasClearKey,
                            modifier = Modifier.weight(weight = 1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameHandwritingCanvas(
    strokes: MutableList<HandwritingStroke>,
    viewModel: GameViewModel,
    clearKey: Int,
    modifier: Modifier = Modifier,
) {
    HandwritingCanvas(
        strokes = strokes,
        onStrokeFinished = {
            strokes.add(it)
            viewModel.onStrokeFinished(stroke = it)
        },
        onStrokeStarted = viewModel::onStrokeStarted,
        onStrokeMoved = viewModel::onStrokeMoved,
        clearKey = clearKey,
        modifier = modifier.onSizeChanged { viewModel.onCanvasSizeChanged(size = it) },
    )
}
