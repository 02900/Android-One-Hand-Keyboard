package com.onehandkeyboard.core.mapping

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.KeyboardLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de modos de mayúsculas/minúsculas para el teclado.
 * 
 * Responsabilidades:
 * - Gestionar el estado actual del modo de case (lowercase, uppercase, capitalize)
 * - Alternar entre diferentes modos de case
 * - Aplicar el modo de case a layouts que lo soporten
 * - Proporcionar acceso reactivo al estado del case mode
 */
@Singleton
class CaseModeManager @Inject constructor() {
    
    private val _currentCaseMode = MutableStateFlow(CaseMode.LOWERCASE)
    val currentCaseMode: Flow<CaseMode> = _currentCaseMode.asStateFlow()
    
    /**
     * Alterna entre los diferentes modos de case en secuencia cíclica
     */
    fun toggleCaseMode() {
        val currentMode = _currentCaseMode.value
        val newMode = when (currentMode) {
            CaseMode.LOWERCASE -> CaseMode.UPPERCASE
            CaseMode.UPPERCASE -> CaseMode.CAPITALIZE
            CaseMode.CAPITALIZE -> CaseMode.LOWERCASE
        }
        setCaseMode(newMode)
    }
    
    /**
     * Establece un modo de case específico
     */
    fun setCaseMode(caseMode: CaseMode) {
        _currentCaseMode.value = caseMode
    }
    
    /**
     * Obtiene el modo de case actual de forma síncrona
     */
    fun getCurrentCaseModeSync(): CaseMode {
        return _currentCaseMode.value
    }
    
    /**
     * Aplica el modo de case actual a un layout si este lo soporta
     */
    fun applyCurrentCaseModeToLayout(layout: KeyboardLayout): KeyboardLayout {
        val currentMode = _currentCaseMode.value
        return if (layout.supportsCaseSwitch && layout.defaultCaseMode != currentMode) {
            layout.withCaseMode(currentMode)
        } else {
            layout
        }
    }
    
    /**
     * Aplica un modo de case específico a un layout si este lo soporta
     */
    fun applyCaseModeToLayout(layout: KeyboardLayout, caseMode: CaseMode): KeyboardLayout {
        return if (layout.supportsCaseSwitch && layout.defaultCaseMode != caseMode) {
            layout.withCaseMode(caseMode)
        } else {
            layout
        }
    }
    
    /**
     * Verifica si un layout soporta cambio de case
     */
    fun layoutSupportsCaseSwitch(layout: KeyboardLayout): Boolean {
        return layout.supportsCaseSwitch
    }
}
