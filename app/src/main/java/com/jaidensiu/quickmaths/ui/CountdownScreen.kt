package com.jaidensiu.quickmaths.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

private const val COUNTDOWN_START = 3
private const val COUNT_DURATION_MS = 1000

@Composable
fun CountdownScreen(
    onCountdownFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CountdownViewModel = hiltViewModel(),
) {
    var count by remember { mutableIntStateOf(value = COUNTDOWN_START) }
    var progress by remember { mutableFloatStateOf(value = 0f) }

    BackHandler {}

    LaunchedEffect(key1 = Unit) {
        for (value in COUNTDOWN_START downTo 1) {
            count = value
            viewModel.onCountShown()
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = COUNT_DURATION_MS),
            ) { fraction, _ ->
                progress = fraction
            }
        }
        onCountdownFinished()
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.graphicsLayer {
                    val scale = 1f + 0.5f * progress
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - progress
                },
            )
        }
    }
}
