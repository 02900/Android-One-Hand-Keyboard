package com.onehandkeyboard.core.mapping

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.Cell
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardLayout
import com.onehandkeyboard.core.domain.model.LayoutType
import com.onehandkeyboard.core.domain.model.Position
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DefaultMappingServiceTest {
    private lateinit var layoutManager: LayoutManager
    private lateinit var caseModeManager: CaseModeManager
    private lateinit var mappingService: DefaultMappingService

    @Before
    fun setup() {
        layoutManager = mockk()
        caseModeManager = mockk()

        // Setup default mock behavior with proper 9 cells
        val mockCells =
            listOf(
                Cell(0, 0, 0, "Cell0", listOf('a'), emptyMap(), emptyMap()),
                Cell(1, 0, 1, "Cell1", listOf('b'), emptyMap(), emptyMap()),
                Cell(2, 0, 2, "Cell2", listOf('c'), emptyMap(), emptyMap()),
                Cell(3, 1, 0, "Cell3", listOf('d'), emptyMap(), emptyMap()),
                Cell(4, 1, 1, "Cell4", listOf('e'), emptyMap(), emptyMap()),
                Cell(5, 1, 2, "Cell5", listOf('f'), emptyMap(), emptyMap()),
                Cell(6, 2, 0, "Cell6", listOf('g'), emptyMap(), emptyMap()),
                Cell(7, 2, 1, "Cell7", listOf('h'), emptyMap(), emptyMap()),
                Cell(8, 2, 2, "Cell8", listOf('i'), emptyMap(), mapOf(Direction.CENTER to KeyAction.Backspace)),
            )

        val mockLayout =
            KeyboardLayout(
                id = "spanish_letters",
                name = "Spanish Letters",
                description = "Spanish keyboard layout with letters",
                cells = mockCells,
                type = LayoutType.LETTERS,
                locale = "es-MX",
            )
        // Setup flow-based properties
        every { layoutManager.currentLayout } returns flowOf(mockLayout)
        every { caseModeManager.currentCaseMode } returns flowOf(CaseMode.LOWERCASE)
        every { layoutManager.availableLayouts } returns flowOf(listOf(mockLayout))

        // Setup synchronous methods
        every { layoutManager.getCurrentLayoutSync() } returns mockLayout
        every { caseModeManager.getCurrentCaseModeSync() } returns CaseMode.LOWERCASE
        every { caseModeManager.applyCurrentCaseModeToLayout(any()) } returns mockLayout

        // Setup methods that modify state
        every { layoutManager.setCurrentLayout(any()) } just Runs
        every { caseModeManager.toggleCaseMode() } just Runs
        every { caseModeManager.setCaseMode(any()) } just Runs
        every { layoutManager.switchToLayout(any()) } returns true
        every { layoutManager.switchToLayoutType(any()) } returns true
        every { layoutManager.loadLayoutsForLocale(any()) } just Runs
        every { layoutManager.registerCustomLayout(any()) } just Runs
        every { layoutManager.removeCustomLayout(any()) } returns true

        mappingService = DefaultMappingService(layoutManager, caseModeManager)
    }

    @Test
    fun `initial state should have Spanish letters layout`() =
        runTest {
            val layout = mappingService.currentLayout.first()
            assertEquals("spanish_letters", layout.id)
            assertEquals(LayoutType.LETTERS, layout.type)
            assertEquals("es-MX", layout.locale)
        }

    @Test
    fun `initial case mode should be lowercase`() =
        runTest {
            val caseMode = mappingService.currentCaseMode.first()
            assertEquals(CaseMode.LOWERCASE, caseMode)
        }

    @Test
    fun `processGesture should return null for unmapped gesture`() =
        runTest {
            val gesture =
                Gesture(
                    startCell = 0,
                    endCell = 0,
                    direction = Direction.S,
                    durationMs = 200L,
                    speed = 100f,
                    distance = 50f,
                    startPosition = Position(50f, 50f),
                    endPosition = Position(50f, 100f),
                    type = GestureType.SWIPE,
                )

            val output = mappingService.processGesture(gesture)
            assertNull(output)
        }

    @Test
    fun `getCurrentLayoutSync should return current layout`() {
        val layout = mappingService.getCurrentLayoutSync()
        assertEquals("spanish_letters", layout.id)
    }

    @Test
    fun `getCurrentCaseModeSync should return current case mode`() {
        val caseMode = mappingService.getCurrentCaseModeSync()
        assertEquals(CaseMode.LOWERCASE, caseMode)
    }
}
