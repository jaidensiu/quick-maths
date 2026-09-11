package com.jaidensiu.quickmaths.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ResultsScreen(
    elapsedTimeMs: Long,
    totalQuestions: Int,
    onPlayAgain: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val storedBestTimeMs by viewModel.bestTimeMs.collectAsStateWithLifecycle()
    val bestTimeMs = (storedBestTimeMs ?: elapsedTimeMs).coerceAtMost(maximumValue = elapsedTimeMs)

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "All $totalQuestions solved!",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Time: ${Util.formatElapsedTime(elapsedTimeMs = elapsedTimeMs)}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "Best time: ${Util.formatElapsedTime(elapsedTimeMs = bestTimeMs)}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.padding(top = 32.dp),
            ) {
                Text(text = "Play again")
            }
            TextButton(
                onClick = onBackToHome,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(text = "Back to home")
            }
        }
    }
}
