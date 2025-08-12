package com.onehandkeyboard.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onehandkeyboard.core.design.theme.KeyboardTheme
import com.onehandkeyboard.core.domain.model.DefaultLayouts
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.KeyboardLayout

/**
 * 3×3 keyboard grid component
 */
@Composable
fun KeyboardGrid(
    layout: KeyboardLayout,
    onKeyPress: (cellIndex: Int, direction: Direction, duration: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressedKey by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Row 0: cells 0, 1, 2
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (cellIndex in 0..2) {
                    val cell = layout.getCell(cellIndex)
                    if (cell != null) {
                        KeyboardKey(
                            cell = cell,
                            onGesture = { direction, duration ->
                                pressedKey = cellIndex
                                onKeyPress(cellIndex, direction, duration)
                                pressedKey = null
                            },
                            isPressed = pressedKey == cellIndex,
                        )
                    }
                }
            }

            // Row 1: cells 3, 4, 5
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (cellIndex in 3..5) {
                    val cell = layout.getCell(cellIndex)
                    if (cell != null) {
                        KeyboardKey(
                            cell = cell,
                            onGesture = { direction, duration ->
                                pressedKey = cellIndex
                                onKeyPress(cellIndex, direction, duration)
                                pressedKey = null
                            },
                            isPressed = pressedKey == cellIndex,
                        )
                    }
                }
            }

            // Row 2: cells 6, 7, 8
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (cellIndex in 6..8) {
                    val cell = layout.getCell(cellIndex)
                    if (cell != null) {
                        KeyboardKey(
                            cell = cell,
                            onGesture = { direction, duration ->
                                pressedKey = cellIndex
                                onKeyPress(cellIndex, direction, duration)
                                pressedKey = null
                            },
                            isPressed = pressedKey == cellIndex,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun KeyboardGridPreview() {
    KeyboardTheme {
        KeyboardGrid(
            layout = DefaultLayouts.spanishLetters(),
            onKeyPress = { _, _, _ -> },
        )
    }
}
