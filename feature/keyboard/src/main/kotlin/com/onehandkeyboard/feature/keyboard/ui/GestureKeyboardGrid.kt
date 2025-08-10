package com.onehandkeyboard.feature.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.gesture.CellBounds
import com.onehandkeyboard.core.gesture.GestureDetector
import kotlinx.coroutines.flow.collectLatest

/**
 * 3x3 gesture-enabled keyboard grid
 * 
 * This component replaces the simple keyboard grid with full gesture support.
 * Each cell can detect taps, long presses, and swipes in 8 directions.
 */
@Composable
fun GestureKeyboardGrid(
    gestureDetector: GestureDetector,
    onGesture: (Gesture) -> Unit,
    modifier: Modifier = Modifier
) {
    var cellBounds by remember { mutableStateOf<Array<CellBounds>>(emptyArray()) }
    val density = LocalDensity.current
    
    // Collect gestures from the detector
    LaunchedEffect(gestureDetector) {
        gestureDetector.gestures.collectLatest { gesture ->
            onGesture(gesture)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(Unit) {
                // Handle touch events and pass to gesture detector
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val motionEvent = event.toMotionEvent()
                        gestureDetector.onTouchEvent(motionEvent, cellBounds)
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Main 3x3 grid
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Cells 0, 1, 2
            KeyRow(
                cellIndices = listOf(0, 1, 2),
                onCellPositioned = { cellIndex, bounds ->
                    cellBounds = cellBounds.copyOf().apply {
                        if (size > cellIndex) {
                            this[cellIndex] = bounds
                        } else {
                            // Initialize array if needed
                            val newBounds = Array(9) { CellBounds(it, 0f, 0f, 0f, 0f) }
                            cellBounds.forEachIndexed { index, bound ->
                                if (index < newBounds.size) newBounds[index] = bound
                            }
                            newBounds[cellIndex] = bounds
                            cellBounds = newBounds
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            
            // Row 2: Cells 3, 4, 5
            KeyRow(
                cellIndices = listOf(3, 4, 5),
                onCellPositioned = { cellIndex, bounds ->
                    cellBounds = cellBounds.copyOf().apply {
                        if (size > cellIndex) {
                            this[cellIndex] = bounds
                        } else {
                            val newBounds = Array(9) { CellBounds(it, 0f, 0f, 0f, 0f) }
                            cellBounds.forEachIndexed { index, bound ->
                                if (index < newBounds.size) newBounds[index] = bound
                            }
                            newBounds[cellIndex] = bounds
                            cellBounds = newBounds
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            
            // Row 3: Cells 6, 7, 8
            KeyRow(
                cellIndices = listOf(6, 7, 8),
                onCellPositioned = { cellIndex, bounds ->
                    cellBounds = cellBounds.copyOf().apply {
                        if (size > cellIndex) {
                            this[cellIndex] = bounds
                        } else {
                            val newBounds = Array(9) { CellBounds(it, 0f, 0f, 0f, 0f) }
                            cellBounds.forEachIndexed { index, bound ->
                                if (index < newBounds.size) newBounds[index] = bound
                            }
                            newBounds[cellIndex] = bounds
                            cellBounds = newBounds
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bottom action bar (for now, keep simple actions)
        ActionBar(
            onBackspace = { 
                // Create a synthetic gesture for backspace
                // This will be replaced with proper mapping in Stage 4
            },
            onSpace = { 
                // Create a synthetic gesture for space
            },
            onEnter = { 
                // Create a synthetic gesture for enter
            }
        )
    }
}

@Composable
private fun KeyRow(
    cellIndices: List<Int>,
    onCellPositioned: (Int, CellBounds) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cellIndices.forEach { cellIndex ->
            GestureKeyCell(
                cellIndex = cellIndex,
                onPositioned = { bounds -> onCellPositioned(cellIndex, bounds) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GestureKeyCell(
    cellIndex: Int,
    onPositioned: (CellBounds) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .onGloballyPositioned { coordinates ->
                val bounds = CellBounds(
                    cellIndex = cellIndex,
                    left = coordinates.localToWindow(androidx.compose.ui.geometry.Offset.Zero).x,
                    top = coordinates.localToWindow(androidx.compose.ui.geometry.Offset.Zero).y,
                    right = coordinates.localToWindow(androidx.compose.ui.geometry.Offset.Zero).x + coordinates.size.width,
                    bottom = coordinates.localToWindow(androidx.compose.ui.geometry.Offset.Zero).y + coordinates.size.height
                )
                onPositioned(bounds)
            },
        contentAlignment = Alignment.Center
    ) {
        // For now, show cell index. This will be replaced with proper key labels in Stage 4
        Text(
            text = cellIndex.toString(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActionBar(
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Backspace button
        ActionButton(
            text = "⌫",
            onClick = onBackspace,
            modifier = Modifier.weight(1f),
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer
        )
        
        // Space button
        ActionButton(
            text = "Space",
            onClick = onSpace,
            modifier = Modifier.weight(3f),
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        
        // Enter button
        ActionButton(
            text = "↵",
            onClick = onEnter,
            modifier = Modifier.weight(1f),
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            textColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// Extension function to convert Compose PointerEvent to Android MotionEvent
// This is a simplified version - in a real implementation, you'd need more complete conversion
private fun androidx.compose.ui.input.pointer.PointerEvent.toMotionEvent(): android.view.MotionEvent {
    // This is a placeholder - actual implementation would require proper MotionEvent creation
    // For now, we'll use mockk in tests and handle this in the actual integration
    TODO("MotionEvent conversion needs platform-specific implementation")
}

@Preview(showBackground = true)
@Composable
private fun GestureKeyboardGridPreview() {
    MaterialTheme {
        // Preview with mock gesture detector
        val mockDetector = object : GestureDetector {
            override val gestures = kotlinx.coroutines.flow.emptyFlow<Gesture>()
            override fun onTouchEvent(event: android.view.MotionEvent, cellBounds: Array<CellBounds>) = true
            override fun updateConfig(config: com.onehandkeyboard.core.domain.model.GestureConfig) {}
            override fun reset() {}
        }
        
        GestureKeyboardGrid(
            gestureDetector = mockDetector,
            onGesture = { }
        )
    }
}
