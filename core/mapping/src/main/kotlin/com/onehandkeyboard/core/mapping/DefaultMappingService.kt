package com.onehandkeyboard.core.mapping

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.DefaultLayouts
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.KeyboardLayout
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.domain.model.LayoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of MappingService
 */
@Singleton
class DefaultMappingService @Inject constructor() : MappingService {
    
    private val mutex = Mutex()
    
    // State flows for reactive updates
    private val _currentLayout = MutableStateFlow(DefaultLayouts.spanishLetters())
    override val currentLayout: Flow<KeyboardLayout> = _currentLayout.asStateFlow()
    
    private val _currentCaseMode = MutableStateFlow(CaseMode.LOWERCASE)
    override val currentCaseMode: Flow<CaseMode> = _currentCaseMode.asStateFlow()
    
    private val _availableLayouts = MutableStateFlow<List<KeyboardLayout>>(emptyList())
    override val availableLayouts: Flow<List<KeyboardLayout>> = _availableLayouts.asStateFlow()
    
    // Internal storage
    private val registeredLayouts = mutableMapOf<String, KeyboardLayout>()
    private var currentLocale = "es-MX"
    
    init {
        // Initialize with default layouts
        loadDefaultLayouts()
    }
    
    override suspend fun processGesture(gesture: Gesture): KeyboardOutput? = mutex.withLock {
        val layout = getCurrentLayoutWithCaseMode()
        return layout.processGesture(gesture)
    }
    
    override suspend fun switchToLayout(layoutId: String): Boolean = mutex.withLock {
        val layout = registeredLayouts[layoutId] ?: return false
        _currentLayout.value = layout.withCaseMode(_currentCaseMode.value)
        return true
    }
    
    override suspend fun switchToLayoutType(layoutType: LayoutType): Boolean = mutex.withLock {
        val layout = _availableLayouts.value.find { it.type == layoutType } ?: return false
        _currentLayout.value = layout.withCaseMode(_currentCaseMode.value)
        return true
    }
    
    override suspend fun toggleCaseMode() = mutex.withLock {
        val currentMode = _currentCaseMode.value
        val newMode = when (currentMode) {
            CaseMode.LOWERCASE -> CaseMode.UPPERCASE
            CaseMode.UPPERCASE -> CaseMode.CAPITALIZE
            CaseMode.CAPITALIZE -> CaseMode.LOWERCASE
        }
        setCaseModeInternal(newMode)
    }
    
    override suspend fun setCaseMode(caseMode: CaseMode) = mutex.withLock {
        setCaseModeInternal(caseMode)
    }
    
    private fun setCaseModeInternal(caseMode: CaseMode) {
        _currentCaseMode.value = caseMode
        val currentLayout = _currentLayout.value
        if (currentLayout.supportsCaseSwitch) {
            _currentLayout.value = currentLayout.withCaseMode(caseMode)
        }
    }
    
    override fun getCurrentLayoutSync(): KeyboardLayout {
        return _currentLayout.value
    }
    
    override fun getCurrentCaseModeSync(): CaseMode {
        return _currentCaseMode.value
    }
    
    override suspend fun loadLayoutsForLocale(locale: String) = mutex.withLock {
        currentLocale = locale
        loadDefaultLayouts()
    }
    
    override suspend fun registerCustomLayout(layout: KeyboardLayout) = mutex.withLock {
        registeredLayouts[layout.id] = layout
        updateAvailableLayouts()
    }
    
    override suspend fun removeCustomLayout(layoutId: String): Boolean = mutex.withLock {
        val removed = registeredLayouts.remove(layoutId) != null
        if (removed) {
            updateAvailableLayouts()
            // If the removed layout was the current one, switch to default
            if (_currentLayout.value.id == layoutId) {
                _currentLayout.value = getDefaultLayoutForLocale()
            }
        }
        return removed
    }
    
    private fun loadDefaultLayouts() {
        registeredLayouts.clear()
        
        // Load layouts based on current locale
        when (currentLocale) {
            "es-MX", "es-ES", "es" -> {
                registeredLayouts["spanish_letters"] = DefaultLayouts.spanishLetters()
                _currentLayout.value = DefaultLayouts.spanishLetters()
            }
            "en-US", "en-GB", "en" -> {
                registeredLayouts["english_letters"] = DefaultLayouts.englishLetters()
                _currentLayout.value = DefaultLayouts.englishLetters()
            }
            else -> {
                // Default to English for unknown locales
                registeredLayouts["english_letters"] = DefaultLayouts.englishLetters()
                _currentLayout.value = DefaultLayouts.englishLetters()
            }
        }
        
        // Always include numbers layout
        registeredLayouts["numbers"] = DefaultLayouts.numbersLayout()
        
        updateAvailableLayouts()
    }
    
    private fun updateAvailableLayouts() {
        _availableLayouts.value = registeredLayouts.values.toList()
    }
    
    private fun getDefaultLayoutForLocale(): KeyboardLayout {
        return when (currentLocale) {
            "es-MX", "es-ES", "es" -> DefaultLayouts.spanishLetters()
            "en-US", "en-GB", "en" -> DefaultLayouts.englishLetters()
            else -> DefaultLayouts.englishLetters()
        }
    }
    
    private fun getCurrentLayoutWithCaseMode(): KeyboardLayout {
        val layout = _currentLayout.value
        val caseMode = _currentCaseMode.value
        return if (layout.supportsCaseSwitch && layout.defaultCaseMode != caseMode) {
            layout.withCaseMode(caseMode)
        } else {
            layout
        }
    }
}
