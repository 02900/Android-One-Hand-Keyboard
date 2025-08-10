package com.onehandkeyboard.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onehandkeyboard.core.design.theme.KeyboardColors
import com.onehandkeyboard.core.design.theme.KeyboardTheme
import com.onehandkeyboard.core.domain.model.Cell
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.KeyAction
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * A single keyboard key component that handles gestures and displays cell content
 */
@Composable
fun KeyboardKey(
    cell: Cell,
    onGesture: (Direction, Long) -> Unit,
    modifier: Modifier = Modifier,
    isPressed: Boolean = false
) {
    val density = LocalDensity.current
    var gestureStart by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var gestureStartTime by remember { mutableStateOf(0L) }
    
    val backgroundColor = if (isPressed) {
        KeyboardColors.KeyBackgroundPressed
    } else {
        KeyboardColors.KeyBackground
    }
    
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(cell.index) {
                detectDragGestures(
                    onDragStart = { offset ->
                        gestureStart = offset.x to offset.y
                        gestureStartTime = System.currentTimeMillis()
                    },
                    onDragEnd = {
                        val start = gestureStart
                        val duration = System.currentTimeMillis() - gestureStartTime
                        
                        if (start != null) {
                            // For now, treat all gestures as center taps
                            // TODO: Implement proper gesture detection based on drag distance and direction
                            onGesture(Direction.CENTER, duration)
                        }
                        gestureStart = null
                    }
                ) { _, _ ->
                    // Handle drag movement if needed
                }
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        KeyContent(cell = cell)
    }
}

@Composable
private fun KeyContent(cell: Cell) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Cell label (ABC, DEF, etc.)
        Text(
            text = cell.label,
            style = MaterialTheme.typography.titleMedium,
            color = KeyboardColors.KeyTextSecondary,
            textAlign = TextAlign.Center
        )
        
        // Primary character
        if (cell.primaryChars.isNotEmpty()) {
            Text(
                text = cell.primaryChars.first().toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = KeyboardColors.KeyText,
                textAlign = TextAlign.Center
            )
        }
        
        // Show action if available for CENTER
        cell.getActionForDirection(Direction.CENTER)?.let { action ->
            Text(
                text = getActionLabel(action),
                style = MaterialTheme.typography.labelLarge,
                color = KeyboardColors.ActionKeyBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getActionLabel(action: KeyAction): String {
    return when (action) {
        is KeyAction.Backspace -> "⌫"
        is KeyAction.Space -> "␣"
        is KeyAction.Enter -> "↵"
        is KeyAction.ToggleCase -> "⇧"
        is KeyAction.SwitchToNumbers -> "123"
        is KeyAction.SwitchToLetters -> "ABC"
        is KeyAction.SwitchToSymbols -> "!@#"
        else -> ""
    }
}

/**
 * Calculate direction from drag gesture
 */
private fun calculateDirection(startX: Float, startY: Float, endX: Float, endY: Float): Direction {
    val deltaX = endX - startX
    val deltaY = endY - startY
    val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
    
    // If movement is too small, consider it a tap (CENTER)
    if (distance < 20f) {
        return Direction.CENTER
    }
    
    // Calculate angle in degrees
    val angle = Math.toDegrees(atan2(-deltaY.toDouble(), deltaX.toDouble())).toFloat()
    val normalizedAngle = if (angle < 0) angle + 360f else angle
    
    return Direction.fromAngle(normalizedAngle)
}

@Preview
@Composable
private fun KeyboardKeyPreview() {
    KeyboardTheme {
        val sampleCell = Cell.create(
            index = 0,
            label = "ABC",
            primaryChars = listOf('a'),
            secondaryChars = mapOf(
                Direction.N to 'á',
                Direction.E to 'b',
                Direction.S to 'c'
            )
        )
        
        KeyboardKey(
            cell = sampleCell,
            onGesture = { _, _ -> }
        )
    }
}
