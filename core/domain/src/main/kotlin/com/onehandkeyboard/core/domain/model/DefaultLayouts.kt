package com.onehandkeyboard.core.domain.model

/**
 * Provides default keyboard layouts for different languages and input modes
 */
object DefaultLayouts {
    /**
     * Spanish (es-MX) letters layout
     * Based on frequency analysis and common letter combinations in Spanish
     */
    fun spanishLetters(): KeyboardLayout {
        val cells =
            listOf(
                // Row 0: ABC, DEF, GHI
                Cell.create(
                    index = 0,
                    label = "ABC",
                    primaryChars = listOf('a'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'á',
                            Direction.NE to 'b',
                            Direction.E to 'c',
                            Direction.SE to '1',
                            Direction.S to '@',
                            Direction.SW to '!',
                            Direction.W to '¡',
                            Direction.NW to 'à',
                        ),
                ),
                Cell.create(
                    index = 1,
                    label = "DEF",
                    primaryChars = listOf('d'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'e',
                            Direction.NE to 'é',
                            Direction.E to 'f',
                            Direction.SE to '2',
                            Direction.S to '#',
                            Direction.SW to '"',
                            Direction.W to '$',
                            Direction.NW to 'è',
                        ),
                ),
                Cell.create(
                    index = 2,
                    label = "GHI",
                    primaryChars = listOf('g'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'h',
                            Direction.NE to 'i',
                            Direction.E to 'í',
                            Direction.SE to '3',
                            Direction.SW to '&',
                            Direction.W to '*',
                            Direction.NW to 'ì',
                        ),
                    actions =
                        mapOf(
                            Direction.S to KeyAction.Backspace, // Backspace on South, removed '%' character
                        ),
                ),
                // Row 1: JKL, MNÑ, OPQ
                Cell.create(
                    index = 3,
                    label = "JKL",
                    primaryChars = listOf('j'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'k',
                            Direction.NE to 'l',
                            Direction.E to '4',
                            Direction.SE to '(',
                            Direction.S to ')',
                            Direction.SW to '-',
                            Direction.W to '_',
                            Direction.NW to '+',
                        ),
                ),
                Cell.create(
                    index = 4,
                    label = "MNÑ",
                    primaryChars = listOf('m'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'n',
                            Direction.NE to 'ñ',
                            Direction.E to '5',
                            Direction.SE to '[',
                            Direction.S to ']',
                            Direction.SW to '{',
                            Direction.W to '}',
                            Direction.NW to '|',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.Space,
                        ),
                ),
                Cell.create(
                    index = 5,
                    label = "OPQ",
                    primaryChars = listOf('o'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'ó',
                            Direction.NE to 'p',
                            Direction.E to 'q',
                            Direction.SE to '6',
                            Direction.S to '\\',
                            Direction.SW to '/',
                            Direction.W to '?',
                            Direction.NW to '¿',
                        ),
                ),
                // Row 2: RST, UVW, XYZ
                Cell.create(
                    index = 6,
                    label = "RST",
                    primaryChars = listOf('r'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 's',
                            Direction.NE to 't',
                            Direction.E to '7',
                            Direction.SE to '<',
                            Direction.S to '>',
                            Direction.SW to '=',
                            Direction.W to '~',
                            Direction.NW to '`',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.ToggleCase,
                        ),
                ),
                Cell.create(
                    index = 7,
                    label = "UVW",
                    primaryChars = listOf('u'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'ú',
                            Direction.NE to 'v',
                            Direction.E to 'w',
                            Direction.SE to '8',
                            Direction.S to ':',
                            Direction.SW to ';',
                            Direction.W to ',',
                            Direction.NW to 'ü',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.Enter,
                        ),
                ),
                Cell.create(
                    index = 8,
                    label = "XYZ",
                    primaryChars = listOf('x'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'y',
                            Direction.NE to 'z',
                            Direction.E to '9',
                            Direction.SE to '0',
                            Direction.S to '.',
                            Direction.SW to '°',
                            Direction.W to '\'',
                            Direction.NW to '"',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.SwitchToNumbers,
                        ),
                ),
            )

        return KeyboardLayout(
            id = "spanish_letters",
            name = "Español - Letras",
            description = "Layout de letras optimizado para español mexicano",
            cells = cells,
            type = LayoutType.LETTERS,
            locale = "es-MX",
            supportsCaseSwitch = true,
            defaultCaseMode = CaseMode.LOWERCASE,
        )
    }

    /**
     * English (en-US) letters layout
     * Based on QWERTY frequency and common English letter patterns
     */
    fun englishLetters(): KeyboardLayout {
        val cells =
            listOf(
                // Row 0: ABC, DEF, GHI
                Cell.create(
                    index = 0,
                    label = "ABC",
                    primaryChars = listOf('a'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'b',
                            Direction.NE to 'c',
                            Direction.E to '1',
                            Direction.SE to '!',
                            Direction.S to '@',
                            Direction.SW to '#',
                            Direction.W to '$',
                            Direction.NW to '%',
                        ),
                ),
                Cell.create(
                    index = 1,
                    label = "DEF",
                    primaryChars = listOf('d'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'e',
                            Direction.NE to 'f',
                            Direction.E to '2',
                            Direction.SE to '"',
                            Direction.S to '&',
                            Direction.SW to '*',
                            Direction.W to '(',
                            Direction.NW to ')',
                        ),
                ),
                Cell.create(
                    index = 2,
                    label = "GHI",
                    primaryChars = listOf('g'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'h',
                            Direction.NE to 'i',
                            Direction.E to '3',
                            Direction.SE to '-',
                            Direction.S to '_',
                            Direction.SW to '+',
                            Direction.W to '=',
                            Direction.NW to '|',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.Backspace,
                        ),
                ),
                // Row 1: JKL, MNO, PQR
                Cell.create(
                    index = 3,
                    label = "JKL",
                    primaryChars = listOf('j'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'k',
                            Direction.NE to 'l',
                            Direction.E to '4',
                            Direction.SE to '[',
                            Direction.S to ']',
                            Direction.SW to '{',
                            Direction.W to '}',
                            Direction.NW to '\\',
                        ),
                ),
                Cell.create(
                    index = 4,
                    label = "MNO",
                    primaryChars = listOf('m'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'n',
                            Direction.NE to 'o',
                            Direction.E to '5',
                            Direction.SE to '/',
                            Direction.S to '?',
                            Direction.SW to '<',
                            Direction.W to '>',
                            Direction.NW to ':',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.Space,
                        ),
                ),
                Cell.create(
                    index = 5,
                    label = "PQR",
                    primaryChars = listOf('p'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'q',
                            Direction.NE to 'r',
                            Direction.E to '6',
                            Direction.SE to ';',
                            Direction.S to '\'',
                            Direction.SW to '"',
                            Direction.W to '`',
                            Direction.NW to '~',
                        ),
                ),
                // Row 2: STU, VWX, YZ
                Cell.create(
                    index = 6,
                    label = "STU",
                    primaryChars = listOf('s'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 't',
                            Direction.NE to 'u',
                            Direction.E to '7',
                            Direction.SE to ',',
                            Direction.S to '.',
                            Direction.SW to '^',
                            Direction.W to '°',
                            Direction.NW to '§',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.ToggleCase,
                        ),
                ),
                Cell.create(
                    index = 7,
                    label = "VWX",
                    primaryChars = listOf('v'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'w',
                            Direction.NE to 'x',
                            Direction.E to '8',
                            Direction.SE to '€',
                            Direction.S to '£',
                            Direction.SW to '¥',
                            Direction.W to '¢',
                            Direction.NW to '₹',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.Enter,
                        ),
                ),
                Cell.create(
                    index = 8,
                    label = "YZ",
                    primaryChars = listOf('y'),
                    secondaryChars =
                        mapOf(
                            Direction.N to 'z',
                            Direction.E to '9',
                            Direction.SE to '0',
                            Direction.S to ' ',
                            Direction.SW to '\t',
                            Direction.W to '\n',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.SwitchToNumbers,
                        ),
                ),
            )

        return KeyboardLayout(
            id = "english_letters",
            name = "English - Letters",
            description = "Letter layout optimized for English",
            cells = cells,
            type = LayoutType.LETTERS,
            locale = "en-US",
            supportsCaseSwitch = true,
            defaultCaseMode = CaseMode.LOWERCASE,
        )
    }

    /**
     * Numbers and basic symbols layout
     */
    fun numbersLayout(): KeyboardLayout {
        val cells =
            listOf(
                // Row 0: 1, 2, 3
                Cell.create(
                    index = 0,
                    label = "1",
                    primaryChars = listOf('1'),
                    secondaryChars =
                        mapOf(
                            Direction.N to '!',
                            Direction.NE to '@',
                            Direction.E to '#',
                            Direction.SE to '$',
                            Direction.S to '%',
                            Direction.SW to '^',
                            Direction.W to '&',
                            Direction.NW to '*',
                        ),
                ),
                Cell.create(
                    index = 1,
                    label = "2",
                    primaryChars = listOf('2'),
                    secondaryChars =
                        mapOf(
                            Direction.N to '(',
                            Direction.NE to ')',
                            Direction.E to '-',
                            Direction.SE to '_',
                            Direction.S to '+',
                            Direction.SW to '=',
                            Direction.W to '[',
                            Direction.NW to ']',
                        ),
                ),
                Cell.create(
                    index = 2,
                    label = "3",
                    primaryChars = listOf('3'),
                    secondaryChars =
                        mapOf(
                            Direction.N to '{',
                            Direction.NE to '}',
                            Direction.E to '|',
                            Direction.SE to '\\',
                            Direction.S to '/',
                            Direction.SW to '?',
                            Direction.W to '<',
                            Direction.NW to '>',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.Backspace,
                        ),
                ),
                // Row 1: 4, 5, 6
                Cell.create(
                    index = 3,
                    label = "4",
                    primaryChars = listOf('4'),
                    secondaryChars =
                        mapOf(
                            Direction.N to ':',
                            Direction.NE to ';',
                            Direction.E to '\'',
                            Direction.SE to '"',
                            Direction.S to '`',
                            Direction.SW to '~',
                            Direction.W to '€',
                            Direction.NW to '£',
                        ),
                ),
                Cell.create(
                    index = 4,
                    label = "5",
                    primaryChars = listOf('5'),
                    secondaryChars =
                        mapOf(
                            Direction.N to '¥',
                            Direction.NE to '₹',
                            Direction.E to '¢',
                            Direction.SE to '°',
                            Direction.S to '§',
                            Direction.SW to '©',
                            Direction.W to '®',
                            Direction.NW to '™',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.Space,
                        ),
                ),
                Cell.create(
                    index = 5,
                    label = "6",
                    primaryChars = listOf('6'),
                    secondaryChars =
                        mapOf(
                            Direction.N to ',',
                            Direction.NE to '.',
                            Direction.E to '…',
                            Direction.SE to '•',
                            Direction.S to '·',
                            Direction.SW to '‚',
                            Direction.W to '\'',
                            Direction.NW to '\'',
                        ),
                ),
                // Row 2: 7, 8, 9
                Cell.create(
                    index = 6,
                    label = "7",
                    primaryChars = listOf('7'),
                    secondaryChars =
                        mapOf(
                            Direction.N to '"',
                            Direction.NE to '"',
                            Direction.E to '„',
                            Direction.SE to '«',
                            Direction.S to '»',
                            Direction.SW to '‹',
                            Direction.W to '›',
                            Direction.NW to '¿',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.ToggleCase,
                        ),
                ),
                Cell.create(
                    index = 7,
                    label = "8",
                    primaryChars = listOf('8'),
                    secondaryChars =
                        mapOf(
                            Direction.N to '¡',
                            Direction.NE to '¨',
                            Direction.E to '´',
                            Direction.SE to '¸',
                            Direction.S to '¯',
                            Direction.SW to '˜',
                            Direction.W to '˙',
                            Direction.NW to '˚',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.Enter,
                        ),
                ),
                Cell.create(
                    index = 8,
                    label = "9",
                    primaryChars = listOf('9'),
                    secondaryChars =
                        mapOf(
                            Direction.N to '0',
                            Direction.E to '+',
                            Direction.SE to '-',
                            Direction.S to '*',
                            Direction.SW to '/',
                            Direction.W to '=',
                            Direction.NW to '%',
                        ),
                    actions =
                        mapOf(
                            Direction.CENTER to KeyAction.SwitchToLetters,
                        ),
                ),
            )

        return KeyboardLayout(
            id = "numbers",
            name = "Números",
            description = "Layout de números y símbolos básicos",
            cells = cells,
            type = LayoutType.NUMBERS,
            locale = "universal",
            supportsCaseSwitch = false,
            defaultCaseMode = CaseMode.LOWERCASE,
        )
    }
}
