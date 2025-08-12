package com.onehandkeyboard.core.mapping

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.KeyboardLayout
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.domain.model.LayoutType
import kotlinx.coroutines.flow.Flow

/**
 * Service responsible for managing keyboard layouts and mapping gestures to characters/actions
 */
interface MappingService {
    /**
     * Current active keyboard layout
     */
    val currentLayout: Flow<KeyboardLayout>

    /**
     * Current case mode
     */
    val currentCaseMode: Flow<CaseMode>

    /**
     * Available layouts for the current locale
     */
    val availableLayouts: Flow<List<KeyboardLayout>>

    /**
     * Processes a gesture and returns the corresponding keyboard output
     *
     * @param gesture The gesture to process
     * @return The keyboard output (character or action) or null if no mapping exists
     */
    suspend fun processGesture(gesture: Gesture): KeyboardOutput?

    /**
     * Switches to a specific layout by ID
     *
     * @param layoutId The ID of the layout to switch to
     * @return true if the switch was successful, false otherwise
     */
    suspend fun switchToLayout(layoutId: String): Boolean

    /**
     * Switches to a layout of the specified type
     *
     * @param layoutType The type of layout to switch to
     * @return true if the switch was successful, false otherwise
     */
    suspend fun switchToLayoutType(layoutType: LayoutType): Boolean

    /**
     * Toggles the case mode (lowercase -> uppercase -> capitalize -> lowercase)
     */
    suspend fun toggleCaseMode()

    /**
     * Sets a specific case mode
     *
     * @param caseMode The case mode to set
     */
    suspend fun setCaseMode(caseMode: CaseMode)

    /**
     * Gets the current layout synchronously
     *
     * @return The current keyboard layout
     */
    fun getCurrentLayoutSync(): KeyboardLayout

    /**
     * Gets the current case mode synchronously
     *
     * @return The current case mode
     */
    fun getCurrentCaseModeSync(): CaseMode

    /**
     * Loads layouts for a specific locale
     *
     * @param locale The locale to load layouts for (e.g., "es-MX", "en-US")
     */
    suspend fun loadLayoutsForLocale(locale: String)

    /**
     * Registers a custom layout
     *
     * @param layout The custom layout to register
     */
    suspend fun registerCustomLayout(layout: KeyboardLayout)

    /**
     * Removes a custom layout
     *
     * @param layoutId The ID of the layout to remove
     * @return true if the layout was removed, false if it didn't exist or couldn't be removed
     */
    suspend fun removeCustomLayout(layoutId: String): Boolean
}
