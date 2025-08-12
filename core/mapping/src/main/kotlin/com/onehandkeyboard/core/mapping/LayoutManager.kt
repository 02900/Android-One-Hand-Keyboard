package com.onehandkeyboard.core.mapping

import com.onehandkeyboard.core.domain.model.DefaultLayouts
import com.onehandkeyboard.core.domain.model.KeyboardLayout
import com.onehandkeyboard.core.domain.model.LayoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de layouts del teclado.
 * 
 * Responsabilidades:
 * - Gestionar el layout actual del teclado
 * - Cargar layouts por locale/idioma
 * - Registrar y remover layouts personalizados
 * - Cambiar entre diferentes layouts y tipos
 * - Proporcionar acceso reactivo a layouts disponibles
 */
@Singleton
class LayoutManager @Inject constructor() {
    
    private val _currentLayout = MutableStateFlow(DefaultLayouts.spanishLetters())
    val currentLayout: Flow<KeyboardLayout> = _currentLayout.asStateFlow()
    
    private val _availableLayouts = MutableStateFlow<List<KeyboardLayout>>(emptyList())
    val availableLayouts: Flow<List<KeyboardLayout>> = _availableLayouts.asStateFlow()
    
    // Internal storage
    private val registeredLayouts = mutableMapOf<String, KeyboardLayout>()
    private var currentLocale = "es-MX"
    
    init {
        // Initialize with default layouts
        loadDefaultLayouts()
    }
    
    /**
     * Cambia al layout especificado por ID
     */
    fun switchToLayout(layoutId: String): Boolean {
        val layout = registeredLayouts[layoutId] ?: return false
        _currentLayout.value = layout
        return true
    }
    
    /**
     * Cambia al primer layout disponible del tipo especificado
     */
    fun switchToLayoutType(layoutType: LayoutType): Boolean {
        val layout = _availableLayouts.value.find { it.type == layoutType } ?: return false
        _currentLayout.value = layout
        return true
    }
    
    /**
     * Obtiene el layout actual de forma síncrona
     */
    fun getCurrentLayoutSync(): KeyboardLayout {
        return _currentLayout.value
    }
    
    /**
     * Establece el layout actual
     */
    fun setCurrentLayout(layout: KeyboardLayout) {
        _currentLayout.value = layout
    }
    
    /**
     * Carga layouts para un locale específico
     */
    fun loadLayoutsForLocale(locale: String) {
        currentLocale = locale
        loadDefaultLayouts()
    }
    
    /**
     * Registra un layout personalizado
     */
    fun registerCustomLayout(layout: KeyboardLayout) {
        registeredLayouts[layout.id] = layout
        updateAvailableLayouts()
    }
    
    /**
     * Remueve un layout personalizado
     */
    fun removeCustomLayout(layoutId: String): Boolean {
        val removed = registeredLayouts.remove(layoutId) != null
        if (removed) {
            updateAvailableLayouts()
            // Si el layout removido era el actual, cambiar al default
            if (_currentLayout.value.id == layoutId) {
                _currentLayout.value = getDefaultLayoutForLocale()
            }
        }
        return removed
    }
    
    /**
     * Obtiene el layout por defecto para el locale actual
     */
    fun getDefaultLayoutForLocale(): KeyboardLayout {
        return when (currentLocale) {
            "es-MX", "es-ES", "es" -> DefaultLayouts.spanishLetters()
            "en-US", "en-GB", "en" -> DefaultLayouts.englishLetters()
            else -> DefaultLayouts.englishLetters()
        }
    }
    
    /**
     * Obtiene el locale actual
     */
    fun getCurrentLocale(): String {
        return currentLocale
    }
    
    /**
     * Verifica si un layout está registrado
     */
    fun isLayoutRegistered(layoutId: String): Boolean {
        return registeredLayouts.containsKey(layoutId)
    }
    
    /**
     * Obtiene un layout registrado por ID
     */
    fun getRegisteredLayout(layoutId: String): KeyboardLayout? {
        return registeredLayouts[layoutId]
    }
    
    /**
     * Obtiene todos los layouts registrados
     */
    fun getAllRegisteredLayouts(): Map<String, KeyboardLayout> {
        return registeredLayouts.toMap()
    }
    
    private fun loadDefaultLayouts() {
        registeredLayouts.clear()
        
        // Cargar layouts basados en el locale actual
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
                // Default a inglés para locales desconocidos
                registeredLayouts["english_letters"] = DefaultLayouts.englishLetters()
                _currentLayout.value = DefaultLayouts.englishLetters()
            }
        }
        
        // Siempre incluir layout de números
        registeredLayouts["numbers"] = DefaultLayouts.numbersLayout()
        
        updateAvailableLayouts()
    }
    
    private fun updateAvailableLayouts() {
        _availableLayouts.value = registeredLayouts.values.toList()
    }
}
