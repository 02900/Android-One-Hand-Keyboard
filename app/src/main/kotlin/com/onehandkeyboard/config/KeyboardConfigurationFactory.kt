package com.onehandkeyboard.config

import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.gestures.GestureAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory para crear configuraciones de teclado específicas por idioma y modo
 */
@Singleton
class KeyboardConfigurationFactory @Inject constructor() {
    /**
     * Obtiene la configuración para un idioma y modo específicos
     */
    fun getConfiguration(
        language: Language,
        mode: LayoutMode,
    ): KeyboardConfiguration {
        return when (language) {
            Language.SPANISH ->
                when (mode) {
                    LayoutMode.LETTERS -> createSpanishLetters()
                    LayoutMode.SYMBOLS -> createSpanishSymbols()
                }
            Language.ENGLISH ->
                when (mode) {
                    LayoutMode.LETTERS -> createEnglishLetters()
                    LayoutMode.SYMBOLS -> createEnglishSymbols()
                }
        }
    }

    /**
     * Configuración por defecto para layout español (mantener compatibilidad)
     */
    fun createSpanishDefault(): KeyboardConfiguration {
        return createSpanishLetters()
    }

    /**
     * Configuración española - modo letras (4x4 grid)
     */
    private fun createSpanishLetters(): KeyboardConfiguration {
        return KeyboardConfiguration(
            name = "Español - Letras",
            description = "Layout español con letras y acentos - Grid 4x4",
            language = Language.SPANISH,
            mode = LayoutMode.LETTERS,
            cells = createLettersGrid4x4()
        )
    }

    /**
     * Configuración española - modo símbolos (4x4 grid)
     */
    private fun createSpanishSymbols(): KeyboardConfiguration {
        return KeyboardConfiguration(
            name = "Español - Símbolos",
            description = "Layout español con números y símbolos - Grid 4x4",
            language = Language.SPANISH,
            mode = LayoutMode.SYMBOLS,
            cells = createSymbolsGrid4x4()
        )
    }

    /**
     * Configuración inglesa - modo letras (4x4 grid)
     */
    private fun createEnglishLetters(): KeyboardConfiguration {
        return KeyboardConfiguration(
            name = "English - Letters",
            description = "English layout with letters - Grid 4x4",
            language = Language.ENGLISH,
            mode = LayoutMode.LETTERS,
            cells = createEnglishGrid4x4()
        )
    }

    /**
     * Configuración inglesa - modo símbolos (reutiliza la española)
     */
    private fun createEnglishSymbols(): KeyboardConfiguration {
        return createSpanishSymbols().copy(
            name = "English - Symbols",
            description = "English layout with numbers and symbols",
            language = Language.ENGLISH,
        )
    }

    /**
     * Crea el grid 4x4 completo para modo letras
     * Grid layout:
     * 00(0)  01(1)  02(2)  03(3)
     * 04(4)  05(5)  06(6)  07(7)  
     * 08(8)  09(9)  10(10) 11(11)
     * 12(12) ----spacebar---- 15(15)
     */
    private fun createLettersGrid4x4(): List<CellConfiguration> {
        return listOf(
            // Row 0: Main grid cells 0-2, Action cell 3
            CellConfiguration(0, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('a'),
                GestureAction(GestureType.SWIPE, Direction.SE) to ActionConfig.Character('v'),
            )),
            CellConfiguration(1, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('n'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('ñ'),
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('l'),
            )),
            CellConfiguration(2, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('i'),
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('?'),
                GestureAction(GestureType.SWIPE, Direction.SW) to ActionConfig.Character('x'),
            )),
            CellConfiguration(3, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.SwitchLayout,
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.IncreaseCellHeight,
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.DecreaseCellHeight,
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.ResetCellHeight,
            )),
            
            // Row 1: Main grid cells 4-6, Action cell 7
            CellConfiguration(4, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('d'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('k'),
            )),
            CellConfiguration(5, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('o'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('u'),
                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('p'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('b'),
                GestureAction(GestureType.SWIPE, Direction.SE) to ActionConfig.Character('j'),
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('h'),
                GestureAction(GestureType.SWIPE, Direction.SW) to ActionConfig.Character('g'),
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('c'),
                GestureAction(GestureType.SWIPE, Direction.NW) to ActionConfig.Character('q'),
            )),
            CellConfiguration(6, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('r'),
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('m'),
            )),
            CellConfiguration(7, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.SwitchCase,
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Paste,
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.CopyAll,
                GestureAction(GestureType.SWIPE, Direction.NW) to ActionConfig.DeleteAll,
            )),
            
            // Row 2: Main grid cells 8-10, Action cell 11
            CellConfiguration(8, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('t'),
                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('y'),
            )),
            CellConfiguration(9, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('e'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('w'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('z'),
                GestureAction(GestureType.SWIPE, Direction.SW) to ActionConfig.Character(','),
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('.'),
            )),
            CellConfiguration(10, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('s'),
                GestureAction(GestureType.SWIPE, Direction.NW) to ActionConfig.Character('f'),
            )),
            CellConfiguration(11, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Backspace,
            )),
            
            // Row 3: Spacebar (spans 3 columns) and Enter
            CellConfiguration(12, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Space,
            ), colSpan = 3),
            CellConfiguration(15, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Enter,
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Submit,
            )),
        )
    }

    /**
     * Crea el grid 4x4 completo para modo símbolos
     * Grid layout:
     * 00(0)  01(1)  02(2)  03(3)
     * 04(4)  05(5)  06(6)  07(7)  
     * 08(8)  09(9)  10(10) 11(11)
     * 12(12) 13(13) --space-- 15(15)
     */
    private fun createSymbolsGrid4x4(): List<CellConfiguration> {
        return listOf(
            // Row 0: Numbers 1-3, Action cell
            CellConfiguration(0, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('1'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('!'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('@'),
            )),
            CellConfiguration(1, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('2'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('"'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('#'),
            )),
            CellConfiguration(2, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('3'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('$'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('%'),
            )),
            CellConfiguration(3, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.ShowLetters,
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.IncreaseCellHeight,
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.DecreaseCellHeight,
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.ResetCellHeight,
            )),
            
            // Row 1: Numbers 4-6, Action cell
            CellConfiguration(4, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('4'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('&'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('*'),
            )),
            CellConfiguration(5, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('5'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('('),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character(')'),
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('-'),
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('='),
            )),
            CellConfiguration(6, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('6'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('['),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character(']'),
            )),
            CellConfiguration(7, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.SwitchCase,
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Paste,
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.CopyAll,
                GestureAction(GestureType.SWIPE, Direction.NW) to ActionConfig.DeleteAll,
            )),
            
            // Row 2: Numbers 7-9, Action cell
            CellConfiguration(8, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('7'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('{'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('}'),
            )),
            CellConfiguration(9, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('8'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('|'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('\\'),
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character(','),
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('.'),
            )),
            CellConfiguration(10, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('9'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('?'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('¿'),
            )),
            CellConfiguration(11, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Backspace,
            )),
            
            // Row 3: 0, Spacebar (spans 2 cols), Enter
            CellConfiguration(12, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('0'),
            ), colSpan = 2),
            CellConfiguration(14, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Space,
            )),
            CellConfiguration(15, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Enter,
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Submit,
            )),
        )
    }

    /**
     * Crea el grid 4x4 para inglés
     */
    private fun createEnglishGrid4x4(): List<CellConfiguration> {
        return listOf(
            // Similar structure but with English letters
            CellConfiguration(0, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('q'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('w'),
            )),
            CellConfiguration(1, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('e'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('r'),
            )),
            CellConfiguration(2, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('t'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('y'),
            )),
            CellConfiguration(3, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.ShowNumbers,
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.IncreaseCellHeight,
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.DecreaseCellHeight,
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.ResetCellHeight,
            )),
            
            CellConfiguration(4, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('a'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('s'),
            )),
            CellConfiguration(5, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('d'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('f'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('g'),
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('h'),
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('j'),
            )),
            CellConfiguration(6, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('k'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('l'),
            )),
            CellConfiguration(7, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.SwitchCase,
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Paste,
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.CopyAll,
                GestureAction(GestureType.SWIPE, Direction.NW) to ActionConfig.DeleteAll,
            )),
            
            CellConfiguration(8, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('z'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('x'),
            )),
            CellConfiguration(9, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('c'),
                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('v'),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('b'),
                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('n'),
                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('m'),
            )),
            CellConfiguration(10, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character(','),
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('.'),
            )),
            CellConfiguration(11, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Backspace,
            )),
            
            // Row 3: Spacebar and Enter
            CellConfiguration(12, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Space,
            ), colSpan = 3),
            CellConfiguration(15, mapOf(
                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Enter,
                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Submit,
            )),
        )
    }
}
