@file:OptIn(ExperimentalMaterial3Api::class)

package com.jaidensiu.quickmaths.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StartScreen(
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strokes = remember { mutableStateListOf<HandwritingStroke>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = strokes::clear) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
        ) {
            HandwritingCanvas(
                strokes = strokes,
                onStrokeFinished = { strokes.add(it) },
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Quick Maths",
                    style = MaterialTheme.typography.displayMedium,
                )
                state.bestTimeMs?.let { bestTimeMs ->
                    Text(
                        text = "Best time: ${Util.formatElapsedTime(elapsedTimeMs = bestTimeMs)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                Button(
                    onClick = onStartGame,
                    enabled = state.modelStatus == ModelStatus.READY,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(
                        text = when (state.modelStatus) {
                            ModelStatus.LOADING -> "Loading model..."
                            ModelStatus.OFFLINE -> "No connection"
                            ModelStatus.READY -> "Start game"
                            ModelStatus.ERROR -> "Model loading error"
                        }
                    )
                }
                when (state.modelStatus) {
                    ModelStatus.OFFLINE -> Text(
                        text = "Connect to the internet to download the handwriting model",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    ModelStatus.ERROR -> {
                        Text(
                            text = "Couldn't download the handwriting model",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        TextButton(onClick = viewModel::onRetry) {
                            Text(text = "Retry")
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}
