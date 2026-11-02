@file:OptIn(ExperimentalMaterial3Api::class)

package com.jaidensiu.quickmaths.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaidensiu.quickmaths.R
import com.jaidensiu.quickmaths.ui.shared.formatElapsedTime

@Composable
fun StartScreen(
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StartContent(
        state = state,
        onStartGame = onStartGame,
        onOpenSettings = onOpenSettings,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

@Composable
fun StartContent(
    state: StartState,
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.sad_man_playing_qm),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .width(width = 200.dp)
                    .padding(bottom = 16.dp),
            )
            Text(
                text = "Quick Maths",
                style = MaterialTheme.typography.displayMedium,
            )
            state.bestTimeMs?.let { bestTimeMs ->
                Text(
                    text = "Best time: ${bestTimeMs.formatElapsedTime()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Button(
                onClick = onStartGame,
                enabled = state.canStart,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(
                    text = when (state.modelStatus) {
                        ModelStatus.LOADING -> "Loading model..."
                        ModelStatus.OFFLINE -> "No connection"
                        ModelStatus.READY -> "Start game"
                        ModelStatus.ERROR -> "Model loading error"
                    },
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
                    TextButton(onClick = onRetry) {
                        Text(text = "Retry")
                    }
                }

                else -> Unit
            }
        }
    }
}
