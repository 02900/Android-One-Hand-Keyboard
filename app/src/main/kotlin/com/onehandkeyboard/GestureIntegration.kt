package com.onehandkeyboard

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Cell
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureConfig
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.domain.model.LayoutType
import com.onehandkeyboard.core.gesture.GestureDetector
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.gestures.GestureAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Turns touch events on a cell into [KeyboardOutput]s using the active keyboard configuration.
 *
 * Privacy: nothing that reveals what the user types (cells, directions, characters) is logged.
 */
class GestureIntegration(
    private val context: Context,
    private val layoutManager: KeyboardLayoutManager,
    private val gestureDetector: GestureDetector,
) {
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var gestureOriginCellIndex = -1
    private val swipeThreshold = 50f // pixels

    private var currentKeyboardLayout: com.onehandkeyboard.core.domain.model.KeyboardLayout? = null
    private var gestureJob: Job? = null
    private val gestureScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun initializeGestureSystem() {
        currentKeyboardLayout = createDomainKeyboardLayout()
        gestureDetector.updateConfig(GestureConfig())

        gestureJob?.cancel()
        gestureJob =
            gestureScope.launch {
                try {
                    gestureDetector.gestures.collect { /* not wired yet */ }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in gesture collection", e)
                }
            }
    }

    /**
     * Crea el layout del dominio según el idioma y modo actual usando configuración externa
     */
    fun createDomainKeyboardLayout(): com.onehandkeyboard.core.domain.model.KeyboardLayout {
        val cells =
            (0..8).map { index ->
                Cell.create(
                    index = index,
                    label = "Cell$index",
                    primaryChars = listOf('a'),
                    secondaryChars = emptyMap(),
                )
            }

        val language = layoutManager.currentLanguage.name
        val mode = layoutManager.currentMode.name
        return com.onehandkeyboard.core.domain.model.KeyboardLayout(
            id = "${language.lowercase()}_${mode.lowercase()}",
            name = "$language $mode",
            description = "Current active layout: $language $mode",
            cells = cells,
            type =
                when (layoutManager.currentMode) {
                    LayoutMode.LETTERS -> LayoutType.LETTERS
                    LayoutMode.SYMBOLS -> LayoutType.NUMBERS
                },
            locale =
                when (layoutManager.currentLanguage) {
                    Language.SPANISH -> "es-MX"
                    Language.ENGLISH -> "en-US"
                },
        )
    }

    /**
     * Procesa eventos de gesto determinando tap/swipe solo en ACTION_UP.
     * Permite gestos que inicien en una celda y terminen fuera de ella.
     */
    fun processGestureEvent(
        event: MotionEvent,
        cellIndex: Int,
        onGestureProcessed: (KeyboardOutput?) -> Unit,
    ) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartX = event.rawX
                gestureStartY = event.rawY
                gestureOriginCellIndex = cellIndex
            }

            MotionEvent.ACTION_UP -> {
                val deltaX = event.rawX - gestureStartX
                val deltaY = event.rawY - gestureStartY
                val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

                if (distance < swipeThreshold) {
                    handleGestureAction(gestureOriginCellIndex, GestureType.TAP, Direction.CENTER, onGestureProcessed)
                } else {
                    val direction = getSwipeDirection(deltaX, deltaY)
                    handleGestureAction(gestureOriginCellIndex, GestureType.SWIPE, direction, onGestureProcessed)
                }
            }
        }
    }

    private fun getSwipeDirection(
        deltaX: Float,
        deltaY: Float,
    ): Direction {
        val absX = kotlin.math.abs(deltaX)
        val absY = kotlin.math.abs(deltaY)

        return when {
            absX > absY * 2 -> if (deltaX > 0) Direction.E else Direction.W
            absY > absX * 2 -> if (deltaY > 0) Direction.S else Direction.N
            deltaX > 0 && deltaY < 0 -> Direction.NE
            deltaX > 0 && deltaY > 0 -> Direction.SE
            deltaX < 0 && deltaY > 0 -> Direction.SW
            deltaX < 0 && deltaY < 0 -> Direction.NW
            else -> Direction.CENTER
        }
    }

    /**
     * Maneja una acción de gesto detectada usando configuración externa
     */
    fun handleGestureAction(
        cellIndex: Int,
        gestureType: GestureType,
        direction: Direction,
        onGestureProcessed: (KeyboardOutput?) -> Unit,
    ) {
        if (!GestureAction.isValidGestureAction(gestureType, direction)) {
            onGestureProcessed(null)
            return
        }

        val gesture = GestureAction(gestureType, direction)
        val actionConfig = layoutManager.getCellConfiguration(cellIndex)?.actions?.get(gesture)
        if (actionConfig == null) {
            onGestureProcessed(null)
            return
        }

        toKeyboardOutput(actionConfig)?.let { onGestureProcessed(it) }
    }

    private fun toKeyboardOutput(actionConfig: ActionConfig): KeyboardOutput? =
        when (actionConfig) {
            is ActionConfig.Character -> KeyboardOutput.Character(layoutManager.applyCase(actionConfig.char.toString()))
            is ActionConfig.Text -> KeyboardOutput.Character(layoutManager.applyCase(actionConfig.text))
            is ActionConfig.Action -> KeyboardOutput.Action(actionConfig.keyAction)
            is ActionConfig.SwitchLayout -> KeyboardOutput.Action(KeyAction.SwitchToNumbers)
            is ActionConfig.SwitchCase -> KeyboardOutput.Action(KeyAction.ToggleCase)
            is ActionConfig.Backspace -> KeyboardOutput.Action(KeyAction.Backspace)
            is ActionConfig.Space -> KeyboardOutput.Action(KeyAction.Space)
            is ActionConfig.Enter -> KeyboardOutput.Action(KeyAction.Enter)
            is ActionConfig.Submit -> KeyboardOutput.Action(KeyAction.Submit)
            is ActionConfig.ShowNumbers -> KeyboardOutput.Action(KeyAction.SwitchToNumbers)
            is ActionConfig.ShowLetters -> KeyboardOutput.Action(KeyAction.SwitchToLetters)
            is ActionConfig.Paste -> KeyboardOutput.Action(KeyAction.Paste)
            is ActionConfig.CopyAll -> KeyboardOutput.Action(KeyAction.CopyAll)
            is ActionConfig.DeleteAll -> KeyboardOutput.Action(KeyAction.DeleteAll)
            is ActionConfig.IncreaseCellHeight -> KeyboardOutput.Action(KeyAction.IncreaseCellHeight)
            is ActionConfig.DecreaseCellHeight -> KeyboardOutput.Action(KeyAction.DecreaseCellHeight)
            is ActionConfig.ResetCellHeight -> KeyboardOutput.Action(KeyAction.ResetCellHeight)
            is ActionConfig.CustomFunction -> null
        }

    /**
     * Libera recursos del sistema de gestos. Cancela todo el scope, no solo el job.
     */
    fun cleanup() {
        gestureJob = null
        gestureScope.cancel()
    }

    private companion object {
        const val TAG = "OneHandKeyboard"
    }
}
