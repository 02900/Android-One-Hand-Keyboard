package com.onehandkeyboard

import android.text.InputType
import android.util.Log
import com.onehandkeyboard.config.KeyboardConfiguration
import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.DeadKeys
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages keyboard layout configuration, switching, and state management.
 *
 * This component handles:
 * - Language switching (Spanish, English)
 * - Layout mode switching (LETTERS ↔ SYMBOLS)
 * - Case mode cycling (lowercase → UPPERCASE → Capitalize)
 * - Key data retrieval for current configuration
 * - Layout state coordination
 *
 * Consolidated from KeyboardConfigManager to eliminate redundant abstraction layer.
 */
@Singleton
class KeyboardLayoutManager
    @Inject
    constructor(
        private val configFactory: KeyboardConfigurationFactory,
    ) {
        // Estado del teclado
        var currentLanguage = Language.SPANISH
            private set
        var currentMode = LayoutMode.LETTERS
            private set
        var currentCaseMode = CaseMode.LOWERCASE
            private set
        var isCapitalizeNext = false
            private set

        // Sistema de configuración externa
        var keyboardConfiguration: KeyboardConfiguration =
            configFactory.getConfiguration(
                Language.SPANISH,
                LayoutMode.LETTERS,
            )
            private set

        /** True when the one-shot capital was set automatically (sentence start), not by the user. */
        var isAutoShifted = false
            private set

        /** Time source for double-tap detection; replaceable in tests. */
        var clock: () -> Long = { System.nanoTime() / NANOS_PER_MILLI }

        private var lastShiftTapTime = Long.MIN_VALUE / 2

        /**
         * Shift key tap:
         * - off → one-shot capital (next letter only)
         * - one-shot → off, or caps lock when tapped twice within [DOUBLE_TAP_MS]
         * - caps lock → off
         */
        fun switchCaseMode(): Boolean {
            val now = clock()
            val isDoubleTap = now - lastShiftTapTime <= DOUBLE_TAP_MS
            lastShiftTapTime = now
            val previousCaseMode = currentCaseMode
            currentCaseMode =
                when {
                    currentCaseMode == CaseMode.UPPERCASE -> CaseMode.LOWERCASE
                    currentCaseMode == CaseMode.CAPITALIZE && isDoubleTap && !isAutoShifted -> CaseMode.UPPERCASE
                    currentCaseMode == CaseMode.CAPITALIZE -> CaseMode.LOWERCASE
                    else -> CaseMode.CAPITALIZE
                }
            isCapitalizeNext = currentCaseMode == CaseMode.CAPITALIZE
            isAutoShifted = false
            if (currentCaseMode == CaseMode.UPPERCASE) lastShiftTapTime = Long.MIN_VALUE / 2

            Log.d("KeyboardLayoutManager", "Case mode switched from $previousCaseMode to $currentCaseMode")
            return previousCaseMode != currentCaseMode
        }

        /**
         * Applies the field's auto-capitalization hint (from `InputConnection.getCursorCapsMode`).
         * Never overrides caps lock or a shift the user set manually.
         *
         * @return true if the case mode changed (labels must be refreshed)
         */
        fun updateAutoShift(shouldCapitalize: Boolean): Boolean {
            return when {
                currentCaseMode == CaseMode.UPPERCASE -> false
                shouldCapitalize && currentCaseMode == CaseMode.LOWERCASE -> {
                    currentCaseMode = CaseMode.CAPITALIZE
                    isCapitalizeNext = true
                    isAutoShifted = true
                    true
                }
                !shouldCapitalize && currentCaseMode == CaseMode.CAPITALIZE && isAutoShifted -> {
                    currentCaseMode = CaseMode.LOWERCASE
                    isCapitalizeNext = false
                    isAutoShifted = false
                    true
                }
                else -> false
            }
        }

        /** Dead key waiting for the next character (´ or ¨), or null. */
        var pendingDeadKey: Char? = null
            private set

        /** Arms (or, if pressed again, cancels) a dead key. Returns true: labels must refresh. */
        fun setDeadKey(accent: Char): Boolean {
            pendingDeadKey = if (pendingDeadKey == accent) null else accent
            return true
        }

        /** Clears a pending dead key. Returns true if there was one. */
        fun clearDeadKey(): Boolean {
            if (pendingDeadKey == null) return false
            pendingDeadKey = null
            return true
        }

        /** Applies the pending dead key and the case mode to a typed character. */
        fun transformChar(text: String): String {
            val cased = applyCase(text)
            val accent = pendingDeadKey ?: return cased
            return DeadKeys.compose(accent, cased)
        }

        /**
         * Called when a new field gets focus: lowercase, and the layer that suits the field
         * (numbers/phone/date fields open on the symbols layer).
         */
        fun resetForNewInput(inputType: Int = InputType.TYPE_CLASS_TEXT) {
            setMode(initialModeFor(inputType))
            currentCaseMode = CaseMode.LOWERCASE
            isCapitalizeNext = false
            isAutoShifted = false
            pendingDeadKey = null
        }

        /** Selects a language explicitly (e.g. from the system IME subtype). */
        fun setLanguage(language: Language): Boolean {
            if (language == currentLanguage || !language.enabled) return false
            currentLanguage = language
            keyboardConfiguration = configFactory.getConfiguration(currentLanguage, currentMode)
            return true
        }

        /**
         * Aplica el modo de mayúsculas/minúsculas al carácter
         */
        private fun applyCase(char: String): String {
            return when (currentCaseMode) {
                CaseMode.LOWERCASE -> char.lowercase()
                CaseMode.UPPERCASE -> char.uppercase()
                CaseMode.CAPITALIZE -> {
                    if (isCapitalizeNext) {
                        char.uppercase()
                    } else {
                        char.lowercase()
                    }
                }
            }
        }

        /**
         * Consume the one-shot capital after a character is written.
         *
         * @return true if the case mode changed (labels must be refreshed)
         */
        fun onCharacterWritten(): Boolean {
            val hadDeadKey = clearDeadKey()
            if (currentCaseMode == CaseMode.CAPITALIZE && isCapitalizeNext) {
                currentCaseMode = CaseMode.LOWERCASE
                isCapitalizeNext = false
                isAutoShifted = false
                return true
            }
            return hadDeadKey
        }

        /** Switches the layer (letters / numbers & symbols). @return true if it changed. */
        fun setMode(mode: LayoutMode): Boolean {
            if (currentMode == mode) return false
            currentMode = mode
            keyboardConfiguration = configFactory.getConfiguration(currentLanguage, currentMode)
            return true
        }

        /**
         * Gets cell configuration for a specific cell index
         */
        fun getCellConfiguration(cellIndex: Int): com.onehandkeyboard.config.CellConfiguration? {
            return keyboardConfiguration.cells.find { it.cellIndex == cellIndex }
        }

        companion object {
            const val DOUBLE_TAP_MS = 300L
            private const val NANOS_PER_MILLI = 1_000_000L

            /** Numeric, phone and date/time fields start on the symbols (numbers) layer. */
            fun initialModeFor(inputType: Int): LayoutMode =
                when (inputType and InputType.TYPE_MASK_CLASS) {
                    InputType.TYPE_CLASS_NUMBER,
                    InputType.TYPE_CLASS_PHONE,
                    InputType.TYPE_CLASS_DATETIME,
                    -> LayoutMode.SYMBOLS
                    else -> LayoutMode.LETTERS
                }
        }
    }
