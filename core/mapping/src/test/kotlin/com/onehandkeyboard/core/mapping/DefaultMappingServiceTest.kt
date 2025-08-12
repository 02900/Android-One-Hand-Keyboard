package com.onehandkeyboard.core.mapping

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.Cell
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardLayout
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.domain.model.LayoutType
import com.onehandkeyboard.core.domain.model.Position
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.every
import io.mockk.mockk

class DefaultMappingServiceTest {
    
    private lateinit var layoutManager: LayoutManager
    private lateinit var caseModeManager: CaseModeManager
    private lateinit var mappingService: DefaultMappingService
    
    @Before
    fun setup() {
        layoutManager = mockk()
        caseModeManager = mockk()
        
        // Setup default mock behavior
        val mockLayout = KeyboardLayout(
            id = "spanish_letters",
            name = "Spanish Letters",
            description = "Spanish keyboard layout with letters",
            cells = emptyList(),
            type = LayoutType.LETTERS,
            locale = "es-MX"
        )
        every { layoutManager.currentLayout } returns flowOf(mockLayout)
        every { caseModeManager.currentCaseMode } returns flowOf(CaseMode.LOWERCASE)
        
        mappingService = DefaultMappingService(layoutManager, caseModeManager)
    }
    
    @Test
    fun `initial state should have Spanish letters layout`() = runTest {
        val layout = mappingService.currentLayout.first()
        assertEquals("spanish_letters", layout.id)
        assertEquals(LayoutType.LETTERS, layout.type)
        assertEquals("es-MX", layout.locale)
    }
    
    @Test
    fun `initial case mode should be lowercase`() = runTest {
        val caseMode = mappingService.currentCaseMode.first()
        assertEquals(CaseMode.LOWERCASE, caseMode)
    }
    
    @Test
    fun `processGesture should return character for tap on cell 0`() = runTest {
        val gesture = Gesture(
            startCell = 0,
            endCell = 0,
            direction = Direction.CENTER,
            durationMs = 100L,
            speed = 0f,
            distance = 0f,
            startPosition = Position(50f, 50f),
            endPosition = Position(50f, 50f),
            type = GestureType.TAP
        )
        
        val output = mappingService.processGesture(gesture)
        assertTrue(output is KeyboardOutput.Character)
        assertEquals('a', (output as KeyboardOutput.Character).char)
    }
    
    @Test
    fun `processGesture should return character for swipe on cell 0`() = runTest {
        val gesture = Gesture(
            startCell = 0,
            endCell = 0,
            direction = Direction.N,
            durationMs = 200L,
            speed = 150f,
            distance = 30f,
            startPosition = Position(50f, 50f),
            endPosition = Position(50f, 20f),
            type = GestureType.SWIPE
        )
        
        val output = mappingService.processGesture(gesture)
        assertTrue(output is KeyboardOutput.Character)
        assertEquals('á', (output as KeyboardOutput.Character).char)
    }
    
    @Test
    fun `processGesture should return action for backspace`() = runTest {
        val gesture = Gesture(
            startCell = 2, // GHI cell has backspace action
            endCell = 2,
            direction = Direction.S, // Backspace is now on South direction
            durationMs = 100L,
            speed = 0f,
            distance = 0f,
            startPosition = Position(250f, 50f),
            endPosition = Position(250f, 80f),
            type = GestureType.SWIPE
        )
        
        val output = mappingService.processGesture(gesture)
        assertTrue(output is KeyboardOutput.Action)
        assertEquals(KeyAction.Backspace, (output as KeyboardOutput.Action).action)
    }
    
    @Test
    fun `processGesture should return null for unmapped gesture`() = runTest {
        val gesture = Gesture(
            startCell = 9, // Invalid cell index (should be 0-8)
            endCell = 9,
            direction = Direction.CENTER,
            durationMs = 200L,
            speed = 150f,
            distance = 0f,
            startPosition = Position(50f, 50f),
            endPosition = Position(50f, 50f),
            type = GestureType.SWIPE
        )
        
        val output = mappingService.processGesture(gesture)
        assertNull(output)
    }
    
    @Test
    fun `switchToLayoutType should switch to numbers layout`() = runTest {
        val success = mappingService.switchToLayoutType(LayoutType.NUMBERS)
        assertTrue(success)
        
        val layout = mappingService.currentLayout.first()
        assertEquals("numbers", layout.id)
        assertEquals(LayoutType.NUMBERS, layout.type)
    }
    
    @Test
    fun `switchToLayoutType should return false for unavailable layout`() = runTest {
        val success = mappingService.switchToLayoutType(LayoutType.EMOJI)
        assertFalse(success)
    }
    
    @Test
    fun `toggleCaseMode should cycle through case modes`() = runTest {
        // Start with lowercase
        assertEquals(CaseMode.LOWERCASE, mappingService.currentCaseMode.first())
        
        // Toggle to uppercase
        mappingService.toggleCaseMode()
        assertEquals(CaseMode.UPPERCASE, mappingService.currentCaseMode.first())
        
        // Toggle to capitalize
        mappingService.toggleCaseMode()
        assertEquals(CaseMode.CAPITALIZE, mappingService.currentCaseMode.first())
        
        // Toggle back to lowercase
        mappingService.toggleCaseMode()
        assertEquals(CaseMode.LOWERCASE, mappingService.currentCaseMode.first())
    }
    
    @Test
    fun `setCaseMode should update case mode`() = runTest {
        mappingService.setCaseMode(CaseMode.UPPERCASE)
        assertEquals(CaseMode.UPPERCASE, mappingService.currentCaseMode.first())
        
        mappingService.setCaseMode(CaseMode.CAPITALIZE)
        assertEquals(CaseMode.CAPITALIZE, mappingService.currentCaseMode.first())
    }
    
    @Test
    fun `case mode should affect character output`() = runTest {
        // Test lowercase
        mappingService.setCaseMode(CaseMode.LOWERCASE)
        val lowercaseGesture = createTapGesture(0) // Cell 0, center tap for 'a'
        val lowercaseOutput = mappingService.processGesture(lowercaseGesture)
        assertEquals('a', (lowercaseOutput as KeyboardOutput.Character).char)
        
        // Test uppercase
        mappingService.setCaseMode(CaseMode.UPPERCASE)
        val uppercaseGesture = createTapGesture(0) // Cell 0, center tap for 'A'
        val uppercaseOutput = mappingService.processGesture(uppercaseGesture)
        assertEquals('A', (uppercaseOutput as KeyboardOutput.Character).char)
    }
    
    @Test
    fun `loadLayoutsForLocale should switch to English for en-US`() = runTest {
        mappingService.loadLayoutsForLocale("en-US")
        
        val layout = mappingService.currentLayout.first()
        assertEquals("english_letters", layout.id)
        assertEquals("en-US", layout.locale)
    }
    
    @Test
    fun `availableLayouts should include default layouts`() = runTest {
        val layouts = mappingService.availableLayouts.first()
        assertTrue(layouts.size >= 2) // At least letters and numbers
        
        val layoutIds = layouts.map { it.id }
        assertTrue(layoutIds.contains("spanish_letters"))
        assertTrue(layoutIds.contains("numbers"))
    }
    
    @Test
    fun `registerCustomLayout should add layout to available layouts`() = runTest {
        val customLayout = createCustomLayout()
        mappingService.registerCustomLayout(customLayout)
        
        val layouts = mappingService.availableLayouts.first()
        val customLayoutFound = layouts.find { it.id == "custom_test" }
        assertNotNull(customLayoutFound)
    }
    
    @Test
    fun `removeCustomLayout should remove layout from available layouts`() = runTest {
        val customLayout = createCustomLayout()
        mappingService.registerCustomLayout(customLayout)
        
        val success = mappingService.removeCustomLayout("custom_test")
        assertTrue(success)
        
        val layouts = mappingService.availableLayouts.first()
        val customLayoutFound = layouts.find { it.id == "custom_test" }
        assertNull(customLayoutFound)
    }
    
    @Test
    fun `removeCustomLayout should return false for non-existent layout`() = runTest {
        val success = mappingService.removeCustomLayout("non_existent")
        assertFalse(success)
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
    
    private fun createTapGesture(cellIndex: Int): Gesture {
        return Gesture(
            startCell = cellIndex,
            endCell = cellIndex,
            direction = Direction.CENTER,
            durationMs = 100L,
            speed = 0f,
            distance = 0f,
            startPosition = Position(50f, 50f),
            endPosition = Position(50f, 50f),
            type = GestureType.TAP
        )
    }
    
    private fun createCustomLayout(): com.onehandkeyboard.core.domain.model.KeyboardLayout {
        val cells = (0..8).map { index ->
            com.onehandkeyboard.core.domain.model.Cell.create(
                index = index,
                label = "TEST$index",
                primaryChars = listOf('t'),
                secondaryChars = mapOf(Direction.N to 'T')
            )
        }
        
        return com.onehandkeyboard.core.domain.model.KeyboardLayout(
            id = "custom_test",
            name = "Custom Test",
            description = "Test layout",
            cells = cells,
            type = LayoutType.CUSTOM,
            locale = "test",
            supportsCaseSwitch = true,
            defaultCaseMode = CaseMode.LOWERCASE
        )
    }
}
