package com.onehandkeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple 3x3 keyboard grid for initial IME implementation
 *
 * This is a temporary implementation that will be replaced with the full
 * gesture-based keyboard in later stages. For now, it provides basic
 * text input functionality to test the IME integration.
 */
@Composable
fun SimpleKeyboardGrid(
    onTextInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onAction: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Main 3x3 grid
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Row 1: A, B, C
            KeyRow(
                keys = listOf("A", "B", "C"),
                onKeyClick = onTextInput,
                modifier = Modifier.weight(1f),
            )

            // Row 2: D, E, F
            KeyRow(
                keys = listOf("D", "E", "F"),
                onKeyClick = onTextInput,
                modifier = Modifier.weight(1f),
            )

            // Row 3: G, H, I
            KeyRow(
                keys = listOf("G", "H", "I"),
                onKeyClick = onTextInput,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom action bar
        ActionBar(
            onBackspace = onBackspace,
            onSpace = { onTextInput(" ") },
            onEnter = { onAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE) },
        )
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keys.forEach { key ->
            KeyButton(
                text = key,
                onClick = { onKeyClick(key.lowercase()) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActionBar(
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Backspace button
        KeyButton(
            text = "⌫",
            onClick = onBackspace,
            modifier = Modifier.weight(1f),
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer,
        )

        // Space button
        KeyButton(
            text = "Space",
            onClick = onSpace,
            modifier = Modifier.weight(3f),
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        // Enter button
        KeyButton(
            text = "↵",
            onClick = onEnter,
            modifier = Modifier.weight(1f),
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            textColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SimpleKeyboardGridPreview() {
    MaterialTheme {
        SimpleKeyboardGrid(
            onTextInput = { },
            onBackspace = { },
            onAction = { },
        )
    }
}
