package com.onehandkeyboard.core.mapping

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.KeyboardLayout
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.domain.model.LayoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of MappingService.
 * 
 * This service coordinates between LayoutManager and CaseModeManager
 * to provide a unified interface for gesture processing and keyboard state management.
 * 
 * Refactored for better separation of concerns and modularity.
 */
@Singleton
class DefaultMappingService @Inject constructor(
    private val layoutManager: LayoutManager,
    private val caseModeManager: CaseModeManager
) : MappingService {
    
    private val mutex = Mutex()
    
    // Delegate reactive flows to specialized managers
    override val currentLayout: Flow<KeyboardLayout> = layoutManager.currentLayout
    override val currentCaseMode: Flow<CaseMode> = caseModeManager.currentCaseMode
    override val availableLayouts: Flow<List<KeyboardLayout>> = layoutManager.availableLayouts
    
    override suspend fun processGesture(gesture: Gesture): KeyboardOutput? = mutex.withLock {
        val layout = getCurrentLayoutWithCaseMode()
        return layout.processGesture(gesture)
    }
    
    override suspend fun switchToLayout(layoutId: String): Boolean = mutex.withLock {
        val success = layoutManager.switchToLayout(layoutId)
        if (success) {
            // Apply current case mode to the new layout
            val currentLayout = layoutManager.getCurrentLayoutSync()
            val layoutWithCase = caseModeManager.applyCurrentCaseModeToLayout(currentLayout)
            layoutManager.setCurrentLayout(layoutWithCase)
        }
        return success
    }
    
    override suspend fun switchToLayoutType(layoutType: LayoutType): Boolean = mutex.withLock {
        val success = layoutManager.switchToLayoutType(layoutType)
        if (success) {
            // Apply current case mode to the new layout
            val currentLayout = layoutManager.getCurrentLayoutSync()
            val layoutWithCase = caseModeManager.applyCurrentCaseModeToLayout(currentLayout)
            layoutManager.setCurrentLayout(layoutWithCase)
        }
        return success
    }
    
    override suspend fun toggleCaseMode() = mutex.withLock {
        caseModeManager.toggleCaseMode()
        updateCurrentLayoutWithCaseMode()
    }
    
    override suspend fun setCaseMode(caseMode: CaseMode) = mutex.withLock {
        caseModeManager.setCaseMode(caseMode)
        updateCurrentLayoutWithCaseMode()
    }
    
    private fun updateCurrentLayoutWithCaseMode() {
        val currentLayout = layoutManager.getCurrentLayoutSync()
        val layoutWithCase = caseModeManager.applyCurrentCaseModeToLayout(currentLayout)
        layoutManager.setCurrentLayout(layoutWithCase)
    }
    
    override fun getCurrentLayoutSync(): KeyboardLayout {
        return layoutManager.getCurrentLayoutSync()
    }
    
    override fun getCurrentCaseModeSync(): CaseMode {
        return caseModeManager.getCurrentCaseModeSync()
    }
    
    override suspend fun loadLayoutsForLocale(locale: String) = mutex.withLock {
        layoutManager.loadLayoutsForLocale(locale)
        updateCurrentLayoutWithCaseMode()
    }
    
    override suspend fun registerCustomLayout(layout: KeyboardLayout) = mutex.withLock {
        layoutManager.registerCustomLayout(layout)
    }
    
    override suspend fun removeCustomLayout(layoutId: String): Boolean = mutex.withLock {
        val removed = layoutManager.removeCustomLayout(layoutId)
        if (removed) {
            updateCurrentLayoutWithCaseMode()
        }
        return removed
    }
    
    private fun getCurrentLayoutWithCaseMode(): KeyboardLayout {
        val layout = layoutManager.getCurrentLayoutSync()
        return caseModeManager.applyCurrentCaseModeToLayout(layout)
    }
}
