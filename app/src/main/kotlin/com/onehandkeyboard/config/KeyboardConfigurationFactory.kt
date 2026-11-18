package com.onehandkeyboard.config

import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Direction.E
import com.onehandkeyboard.core.domain.model.Direction.N
import com.onehandkeyboard.core.domain.model.Direction.NE
import com.onehandkeyboard.core.domain.model.Direction.NW
import com.onehandkeyboard.core.domain.model.Direction.S
import com.onehandkeyboard.core.domain.model.Direction.SE
import com.onehandkeyboard.core.domain.model.Direction.SW
import com.onehandkeyboard.core.domain.model.Direction.W
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keyboard configurations per language and mode.
 *
 * Grid (cell indexes):
 * ```
 *  0  1  2 | 3   layout / height
 *  4  5  6 | 7   case / clipboard
 *  8  9 10 | 11  backspace / delete all / undo
 * 12 ---- 14 | 15  space (spans) / enter / submit
 * ```
 */
@Singleton
class KeyboardConfigurationFactory
    @Inject
    constructor() {
        fun getConfiguration(
            language: Language,
            mode: LayoutMode,
        ): KeyboardConfiguration =
            when (language) {
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

        fun createSpanishDefault(): KeyboardConfiguration = createSpanishLetters()

        private fun createSpanishLetters() =
            KeyboardConfiguration(
                name = "Español - Letras",
                description = "Layout español con letras - Grid 4x4",
                language = Language.SPANISH,
                mode = LayoutMode.LETTERS,
                cells = spanishLettersGrid(),
            )

        private fun createSpanishSymbols() =
            KeyboardConfiguration(
                name = "Español - Símbolos",
                description = "Layout español con números y símbolos - Grid 4x4",
                language = Language.SPANISH,
                mode = LayoutMode.SYMBOLS,
                cells = symbolsGrid(),
            )

        private fun createEnglishLetters() =
            KeyboardConfiguration(
                name = "English - Letters",
                description = "English layout with letters - Grid 4x4",
                language = Language.ENGLISH,
                mode = LayoutMode.LETTERS,
                cells = englishLettersGrid(),
            )

        private fun createEnglishSymbols() =
            createSpanishSymbols().copy(
                name = "English - Symbols",
                description = "English layout with numbers and symbols",
                language = Language.ENGLISH,
            )

        private fun spanishLettersGrid() =
            keyboardGrid {
                cell(0) {
                    tap('a')
                    swipe(SE, 'v')
                }
                cell(1) {
                    tap('n')
                    swipe(N, 'ñ')
                    swipe(S, 'l')
                }
                cell(2) {
                    tap('i')
                    swipe(W, '?')
                    swipe(SW, 'x')
                }
                layoutCell(ActionConfig.SwitchLayout)

                cell(4) {
                    tap('d')
                    swipe(E, 'k')
                }
                cell(5) {
                    tap('o')
                    swipe(N, 'u')
                    swipe(NE, 'p')
                    swipe(E, 'b')
                    swipe(SE, 'j')
                    swipe(S, 'h')
                    swipe(SW, 'g')
                    swipe(W, 'c')
                    swipe(NW, 'q')
                }
                cell(6) {
                    tap('r')
                    swipe(W, 'm')
                }
                caseCell()

                cell(8) {
                    tap('t')
                    swipe(NE, 'y')
                }
                cell(9) {
                    tap('e')
                    swipe(N, 'w')
                    swipe(E, 'z')
                    swipe(SW, ',')
                    swipe(S, '.')
                    swipe(SE, ':')
                    swipe(NE, '\'')
                }
                cell(10) {
                    tap('s')
                    swipe(NW, 'f')
                }
                backspaceCell()

                cell(12, colSpan = 3) { tap(ActionConfig.Space) }
                enterCell()
            }

        private fun symbolsGrid() =
            keyboardGrid {
                cell(0) {
                    tap('1')
                    swipe(E, '-')
                    swipe(SW, '$')
                }
                cell(1) {
                    tap('2')
                    swipe(N, '^')
                    swipe(E, '!')
                    swipe(S, '+')
                }
                cell(2) {
                    tap('3')
                    swipe(SE, '€')
                    swipe(S, '=')
                    swipe(W, '?')
                }
                layoutCell(ActionConfig.ShowLetters)

                cell(4) {
                    tap('4')
                    swipe(NE, '%')
                    swipe(SE, '_')
                    swipe(SW, '[')
                    swipe(W, '(')
                    swipe(NW, '{')
                }
                cell(5) {
                    tap('5')
                    swipe(E, '/')
                    swipe(W, '\\')
                    swipe(N, '~')
                    swipe(S, '`')
                }
                cell(6) {
                    tap('6')
                    swipe(NE, '}')
                    swipe(E, ')')
                    swipe(SE, ']')
                    swipe(NW, '|')
                    swipe(SW, '@')
                }
                caseCell(withShift = false)

                cell(8) {
                    tap('7')
                    swipe(W, '*')
                    swipe(SW, '<')
                }
                cell(9) {
                    tap('8')
                    swipe(NW, '"')
                    swipe(S, '.')
                    swipe(SE, ':')
                    swipe(SW, ',')
                    swipe(NE, '\'')
                }
                cell(10) {
                    tap('9')
                    swipe(N, '&')
                    swipe(E, '>')
                    swipe(W, '#')
                    swipe(NW, ';')
                }
                backspaceCell()

                cell(12, colSpan = 2) { tap('0') }
                cell(14) { tap(ActionConfig.Space) }
                enterCell()
            }

        private fun englishLettersGrid() =
            keyboardGrid {
                cell(0) {
                    tap('q')
                    swipe(E, 'w')
                }
                cell(1) {
                    tap('e')
                    swipe(E, 'r')
                }
                cell(2) {
                    tap('t')
                    swipe(E, 'y')
                }
                layoutCell(ActionConfig.ShowNumbers)

                cell(4) {
                    tap('a')
                    swipe(E, 's')
                }
                cell(5) {
                    tap('d')
                    swipe(N, 'f')
                    swipe(E, 'g')
                    swipe(S, 'h')
                    swipe(W, 'j')
                }
                cell(6) {
                    tap('k')
                    swipe(E, 'l')
                }
                caseCell()

                cell(8) {
                    tap('z')
                    swipe(E, 'x')
                }
                cell(9) {
                    tap('c')
                    swipe(N, 'v')
                    swipe(E, 'b')
                    swipe(S, 'n')
                    swipe(W, 'm')
                }
                cell(10) {
                    tap(',')
                    swipe(E, '.')
                }
                backspaceCell()

                cell(12, colSpan = 3) { tap(ActionConfig.Space) }
                enterCell()
            }

        // ------------------------------------------------------------------
        // Control column, shared by every layout
        // ------------------------------------------------------------------

        private fun GridBuilder.layoutCell(switchAction: ActionConfig) =
            cell(3) {
                tap(switchAction)
                swipe(N, ActionConfig.IncreaseCellHeight)
                swipe(S, ActionConfig.DecreaseCellHeight)
                swipe(W, ActionConfig.ResetCellHeight)
            }

        /** Shift has no meaning on the symbols layer, so there it only keeps the clipboard swipes. */
        private fun GridBuilder.caseCell(withShift: Boolean = true) =
            cell(7) {
                if (withShift) tap(ActionConfig.SwitchCase)
                swipe(S, ActionConfig.Paste)
                swipe(N, ActionConfig.CopyAll)
            }

        /** "Delete all" lives on the delete key (deliberate swipe up) and can be undone. */
        private fun GridBuilder.backspaceCell() =
            cell(11) {
                tap(ActionConfig.Backspace)
                swipe(N, ActionConfig.DeleteAll)
                swipe(E, ActionConfig.Action(KeyAction.UndoDelete))
            }

        private fun GridBuilder.enterCell() =
            cell(15) {
                tap(ActionConfig.Enter)
                swipe(E, ActionConfig.Submit)
            }
    }
