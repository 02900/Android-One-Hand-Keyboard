package com.onehandkeyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.onehandkeyboard.core.gesture.GestureDetector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main InputMethodService for OneHandKeyboard - A 3x3 gesture-based keyboard.
 *
 * This service provides a compact 9-key keyboard with 8-directional swipe gestures,
 * supporting multiple languages (Spanish, English) and layout modes (Letters, Symbols).
 * 
 * REFACTORED ARCHITECTURE:
 * This service now acts as a clean coordinator, delegating responsibilities to specialized components:
 * - KeyboardLayoutManager: Layout and configuration management (Hilt singleton)
 * - KeyboardViewBuilder: UI creation and view building (manual init)
 * - KeyboardEventCoordinator: Event coordination and gesture integration (manual init)
 * - KeyboardUiFactory: UI factory for components (manual init)
 * - GestureIntegration: Gesture processing (manual init)
 * - KeyboardActionProcessor: Action execution (manual init)
 * 
 * Features:
 * - 3x3 grid with gesture hints and modern styling
 * - Multi-directional swipes (8 directions + tap + long press)
 * - Dynamic layout switching and case mode management
 * - Offline-first, privacy-focused design
 * - Clean modular architecture following SRP
 */
@AndroidEntryPoint
class OneHandKeyboardService : InputMethodService() {
    private var keyboardView: LinearLayout? = null
    
    // Core dependencies - injected by Hilt
    @Inject
    lateinit var configManager: KeyboardConfigManager
    
    @Inject
    lateinit var gestureDetector: GestureDetector
    
    @Inject
    lateinit var layoutManager: KeyboardLayoutManager
    
    // Components initialized manually due to context dependencies
    private lateinit var uiFactory: KeyboardUiFactory
    private lateinit var viewBuilder: KeyboardViewBuilder
    private lateinit var gestureIntegration: GestureIntegration
    private lateinit var actionProcessor: KeyboardActionProcessor
    private lateinit var eventCoordinator: KeyboardEventCoordinator

    // ============================================================================
    // REGION: LIFECYCLE & CORE SERVICE
    // ============================================================================

    override fun onCreateInputView(): View {
        Log.d("OneHandKeyboard", "onCreateInputView() called - MODULAR ARCHITECTURE VERSION")
        
        try {
            // Initialize components that require context
            initializeComponents()
            
            // Create keyboard view using view builder
            keyboardView = viewBuilder.createMainKeyboard()
            
            // Setup events and gesture integration
            eventCoordinator.setupKeyboardEvents(keyboardView!!)
            
            Log.d("OneHandKeyboard", "Modular keyboard created successfully")
            return keyboardView!!
        } catch (e: Exception) {
            Log.e("OneHandKeyboard", "Error creating keyboard", e)
            return createFallbackView()
        }
    }
    
    /**
     * Initializes all components that require context dependencies
     */
    private fun initializeComponents() {
        // Initialize UI factory (requires service context)
        uiFactory = KeyboardUiFactory(this, configManager)
        
        // Initialize gesture integration (requires service callbacks)
        gestureIntegration = GestureIntegration(this, configManager, gestureDetector)
        gestureIntegration.initializeGestureSystem()
        
        // Initialize action processor (requires service callbacks)
        actionProcessor = KeyboardActionProcessor(
            service = this,
            configManager = configManager,
            onLayoutSwitch = { eventCoordinator.recreateKeyboard() },
            onCaseSwitch = { eventCoordinator.recreateKeyboard() }
        )
        
        // Initialize view builder (requires context, layout manager, gesture integration, and action processor)
        viewBuilder = KeyboardViewBuilder(this, layoutManager, uiFactory, gestureIntegration, actionProcessor)
        
        // Initialize event coordinator (coordinates all components)
        eventCoordinator = KeyboardEventCoordinator(
            service = this,
            layoutManager = layoutManager,
            viewBuilder = viewBuilder,
            gestureIntegration = gestureIntegration,
            actionProcessor = actionProcessor
        )
        
        Log.d("OneHandKeyboard", "All components initialized successfully")
    }
    
    /**
     * Creates a fallback view in case of initialization errors
     */
    private fun createFallbackView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(android.widget.TextView(this@OneHandKeyboardService).apply {
                text = "Keyboard Error - Please restart"
                textSize = 16f
                setTextColor(android.graphics.Color.RED)
            })
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d("OneHandKeyboard", "🔥 onStartInputView() called")
        
        // Setup cell bounds when view is ready - multiple attempts for reliability
        keyboardView?.let { view ->
            // Attempt 1: Immediate post
            view.post {
                Log.d("OneHandKeyboard", "🔥 Post runnable executing - about to calculate bounds")
                gestureIntegration.calculateCellBounds(keyboardView)
            }
            
            // Attempt 2: Post with delay to ensure view is rendered
            view.postDelayed({
                Log.d("OneHandKeyboard", "🔥 Delayed post runnable executing - recalculating bounds")
                gestureIntegration.calculateCellBounds(keyboardView)
            }, 100) // 100ms delay
            
            // Attempt 3: Post with more delay as backup
            view.postDelayed({
                Log.d("OneHandKeyboard", "🔥 Final delayed post runnable executing - last attempt")
                gestureIntegration.calculateCellBounds(keyboardView)
            }, 500) // 500ms delay
        } ?: Log.w("OneHandKeyboard", "🔥 keyboardView is null in onStartInputView!")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("OneHandKeyboard", "Service destroying - cleaning up components")
        
        // Cleanup through event coordinator
        if (::eventCoordinator.isInitialized) {
            eventCoordinator.cleanup()
        }
        
        // Reset gesture detector
        gestureDetector.reset()
    }

    // ============================================================================
    // REGION: LIFECYCLE METHODS
    // ============================================================================
    
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d("OneHandKeyboard", "onFinishInputView() called - keyboard hiding")
    }
    
    override fun onComputeInsets(outInsets: InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        Log.d("OneHandKeyboard", "onComputeInsets() called")
        
        // Force keyboard to occupy visible space
        outInsets.contentTopInsets = 0
        outInsets.visibleTopInsets = 0
        outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE
        
        Log.d("OneHandKeyboard", "Insets configured - keyboard should be visible")
    }
    
    override fun onEvaluateInputViewShown(): Boolean {
        Log.d("OneHandKeyboard", "onEvaluateInputViewShown() called")
        // Force keyboard to always show
        return true
    }
}
