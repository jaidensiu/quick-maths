@file:OptIn(ExperimentalMaterial3Api::class)

package com.jaidensiu.quickmaths.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaidensiu.quickmaths.domain.ThemePreference

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to start",
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
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = 16.dp)) {
                ThemePreference.entries.forEachIndexed { index, preference ->
                    SegmentedButton(
                        selected = theme == preference,
                        onClick = { viewModel.onThemeSelected(theme = preference) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemePreference.entries.size,
                        ),
                    ) {
                        Text(text = preference.label)
                    }
                }
            }
        }
    }
}

private val ThemePreference.label: String
    get() = when (this) {
        ThemePreference.LIGHT -> "Light"
        ThemePreference.DARK -> "Dark"
        ThemePreference.SYSTEM -> "System"
    }
