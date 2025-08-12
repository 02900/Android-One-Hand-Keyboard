package com.onehandkeyboard.feature.keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardLayout
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.domain.model.Position
import com.onehandkeyboard.core.mapping.MappingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the keyboard feature
 */
@HiltViewModel
class KeyboardViewModel @Inject constructor(
    private val mappingService: MappingService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()
    
    val currentLayout = mappingService.currentLayout
    val currentCaseMode = mappingService.currentCaseMode
    
    init {
        // Initialize with current layout
        viewModelScope.launch {
            currentLayout.collect { layout ->
                _uiState.value = _uiState.value.copy(
                    currentLayout = layout
                )
            }
        }
    }
    
    /**
     * Handles key press events from the UI
     */
    fun onKeyPress(cellIndex: Int, direction: Direction, duration: Long) {
        viewModelScope.launch {
            // Create gesture from UI input
            val gesture = createGestureFromInput(cellIndex, direction, duration)
            
            // Process gesture through mapping service
            val output = mappingService.processGesture(gesture)
            
            // Handle the output
            when (output) {
                is KeyboardOutput.Character -> {
                    onCharacterOutput(output.char.firstOrNull() ?: ' ')
                }
                is KeyboardOutput.Action -> {
                    onActionOutput(output.action)
                }
                null -> {
                    // No output for this gesture
                }
            }
        }
    }
    
    /**
     * Handles character output
     */
    private fun onCharacterOutput(char: Char) {
        _uiState.value = _uiState.value.copy(
            lastOutput = "Character: $char"
        )
        
        // TODO: Send character to InputConnection
        // This will be implemented when we integrate with InputMethodService
    }
    
    /**
     * Handles action output
     */
    private fun onActionOutput(action: KeyAction) {
        viewModelScope.launch {
            when (action) {
                is KeyAction.Backspace -> {
                    _uiState.value = _uiState.value.copy(
                        lastOutput = "Action: Backspace"
                    )
                    // TODO: Send backspace to InputConnection
                }
                is KeyAction.Space -> {
                    _uiState.value = _uiState.value.copy(
                        lastOutput = "Action: Space"
                    )
                    // TODO: Send space to InputConnection
                }
                is KeyAction.Enter -> {
                    _uiState.value = _uiState.value.copy(
                        lastOutput = "Action: Enter"
                    )
                    // TODO: Send enter to InputConnection
                }
                is KeyAction.ToggleCase -> {
                    mappingService.toggleCaseMode()
                    _uiState.value = _uiState.value.copy(
                        lastOutput = "Action: Toggle Case"
                    )
                }
                is KeyAction.SwitchToNumbers -> {
                    mappingService.switchToLayoutType(com.onehandkeyboard.core.domain.model.LayoutType.NUMBERS)
                    _uiState.value = _uiState.value.copy(
                        lastOutput = "Action: Switch to Numbers"
                    )
                }
                is KeyAction.SwitchToLetters -> {
                    mappingService.switchToLayoutType(com.onehandkeyboard.core.domain.model.LayoutType.LETTERS)
                    _uiState.value = _uiState.value.copy(
                        lastOutput = "Action: Switch to Letters"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        lastOutput = "Action: ${action::class.simpleName}"
                    )
                }
            }
        }
    }
    
    /**
     * Creates a gesture from UI input
     */
    private fun createGestureFromInput(cellIndex: Int, direction: Direction, duration: Long): Gesture {
        val gestureType = when {
            duration > 500L -> GestureType.LONG_PRESS
            direction != Direction.CENTER -> GestureType.SWIPE
            else -> GestureType.TAP
        }
        
        return Gesture(
            startCell = cellIndex,
            endCell = cellIndex,
            direction = direction,
            durationMs = duration,
            speed = 0f, // Not calculated from UI
            distance = 0f, // Not calculated from UI
            startPosition = Position(0f, 0f), // Not needed for mapping
            endPosition = Position(0f, 0f), // Not needed for mapping
            type = gestureType
        )
    }
}

/**
 * UI state for the keyboard
 */
data class KeyboardUiState(
    val currentLayout: KeyboardLayout? = null,
    val lastOutput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
