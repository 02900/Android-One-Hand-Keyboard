package com.onehandkeyboard.feature.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onehandkeyboard.core.design.components.KeyboardGrid
import com.onehandkeyboard.core.design.theme.KeyboardTheme
import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.DefaultLayouts

/**
 * Main keyboard screen
 */
@Composable
fun KeyboardScreen(
    modifier: Modifier = Modifier,
    viewModel: KeyboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentLayout by viewModel.currentLayout.collectAsState(initial = null)
    val currentCaseMode by viewModel.currentCaseMode.collectAsState(initial = CaseMode.LOWERCASE)

    KeyboardTheme {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                // Debug info (will be removed in production)
                if (uiState.lastOutput.isNotEmpty()) {
                    Text(
                        text = "Last Output: ${uiState.lastOutput}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                    )
                }

                // Case mode indicator
                Text(
                    text = "Mode: ${currentCaseMode.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                )

                // Main keyboard grid
                currentLayout?.let { layout ->
                    KeyboardGrid(
                        layout = layout,
                        onKeyPress = { cellIndex, direction, duration ->
                            viewModel.onKeyPress(cellIndex, direction, duration)
                        },
                    )
                } ?: run {
                    // Loading state
                    Text(
                        text = "Loading keyboard...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Error state
                uiState.error?.let { error ->
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun KeyboardScreenPreview() {
    KeyboardTheme {
        // Preview with mock data
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Text(
                    text = "Last Output: Character: a",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                )

                Text(
                    text = "Mode: LOWERCASE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                )

                KeyboardGrid(
                    layout = DefaultLayouts.spanishLetters(),
                    onKeyPress = { _, _, _ -> },
                )
            }
        }
    }
}
