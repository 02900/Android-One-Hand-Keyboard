package com.onehandkeyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.onehandkeyboard.KeyboardActionProcessor
import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.KeyboardUiFactory
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.gesture.DefaultGestureDetector
import com.onehandkeyboard.GestureIntegration
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
    lateinit var gestureDetector: DefaultGestureDetector

    @Inject
    lateinit var layoutManager: KeyboardLayoutManager

    @Inject
    lateinit var themeManager: ThemeManager
    
    @Inject
    lateinit var configFactory: com.onehandkeyboard.config.KeyboardConfigurationFactory
    
    // BroadcastReceiver for theme changes
    private var themeChangeReceiver: BroadcastReceiver? = null

    // Components initialized manually due to context dependencies
    private lateinit var uiFactory: KeyboardUiFactory
    private lateinit var unifiedGridBuilder: UnifiedGridKeyboardBuilder
    private lateinit var gestureIntegration: GestureIntegration
    private lateinit var actionProcessor: KeyboardActionProcessor

    // ============================================================================
    // REGION: LIFECYCLE & CORE SERVICE
    // ============================================================================

    override fun onCreateInputView(): View? {
        Log.d("OneHandKeyboard", "Creating input view")

        return try {
            Log.d("OneHandKeyboard", "Step 1: Refreshing theme...")
            // Refresh theme from SharedPreferences to get latest selection
            themeManager.refreshTheme()
            Log.d("OneHandKeyboard", "Step 1: Theme refreshed successfully")
            
            Log.d("OneHandKeyboard", "Step 2: Initializing components...")
            // Initialize all components first
            initializeComponents()
            Log.d("OneHandKeyboard", "Step 2: Components initialized successfully")

            Log.d("OneHandKeyboard", "Step 3: Creating main keyboard...")
            // Create the main keyboard view using unified grid builder
            val mainKeyboard = unifiedGridBuilder.createKeyboardLayout()
            keyboardView = mainKeyboard
            Log.d("OneHandKeyboard", "Step 3: Main keyboard created successfully")

            Log.d("OneHandKeyboard", "Step 4: Keyboard ready")

            Log.d("OneHandKeyboard", "Input view created successfully")
            mainKeyboard
        } catch (e: Exception) {
            Log.e("OneHandKeyboard", "Error creating input view at step: ${e.message}", e)
            e.printStackTrace()
            createFallbackView()
        }
    }

    /**
     * Initializes all components that require context dependencies
     */
    private fun initializeComponents() {
        // Initialize UI factory (requires context, config manager, and theme manager)
        uiFactory = KeyboardUiFactory(this, layoutManager, themeManager)

        // Initialize gesture integration (requires service callbacks)
        gestureIntegration = GestureIntegration(this, layoutManager, gestureDetector)
        gestureIntegration.initializeGestureSystem()

        // Initialize action processor (requires service callbacks)
        actionProcessor =
            KeyboardActionProcessor(
                service = this,
                configManager = layoutManager,
                layoutCalculator = uiFactory.layoutCalculator,
                onLayoutSwitch = { recreateKeyboard() },
                onCaseSwitch = { recreateKeyboard() },
                onHeightChanged = { recreateKeyboard() },
            )
        
        // Initialize unified grid builder (new 4x4 grid system)
        unifiedGridBuilder = UnifiedGridKeyboardBuilder(this, themeManager, uiFactory.styleProvider, layoutManager, gestureIntegration, actionProcessor, uiFactory, configFactory)

        Log.d("OneHandKeyboard", "All components initialized successfully")
        
        // Register theme change receiver
        registerThemeChangeReceiver()
    }
    
    /**
     * Recreates the keyboard when layout or case mode changes
     */
    private fun recreateKeyboard() {
        Log.d("OneHandKeyboard", "Recreating keyboard due to layout/case change")
        keyboardView = unifiedGridBuilder.createKeyboardLayout()
        setInputView(keyboardView)
    }
    
    /**
     * Register BroadcastReceiver to listen for theme changes
     */
    private fun registerThemeChangeReceiver() {
        themeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ThemeManager.ACTION_THEME_CHANGED) {
                    val themeName = intent.getStringExtra(ThemeManager.EXTRA_THEME_NAME)
                    Log.d("OneHandKeyboard", "Received theme change broadcast: $themeName")
                    
                    // Refresh theme and recreate keyboard
                    themeManager.refreshTheme()
                    recreateKeyboard()
                }
            }
        }
        
        val filter = IntentFilter(ThemeManager.ACTION_THEME_CHANGED)
        
        // Register receiver with proper flags for Android 14+ compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(themeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(themeChangeReceiver, filter)
        }
        
        Log.d("OneHandKeyboard", "Theme change receiver registered")
    }

    /**
     * Creates a fallback view in case of initialization errors
     */
    private fun createFallbackView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(
                android.widget.TextView(this@OneHandKeyboardService).apply {
                    text = "Keyboard Error - Please restart"
                    textSize = 16f
                    setTextColor(android.graphics.Color.RED)
                },
            )
        }
    }

    override fun onStartInputView(
        info: android.view.inputmethod.EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        Log.d("OneHandKeyboard", "Starting input view")
        
        // Setup cell bounds when view is ready
        keyboardView?.let { view ->
            view.post {
                gestureIntegration.calculateCellBounds(keyboardView)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("OneHandKeyboard", "Service destroying - cleaning up components")

        // Unregister theme change receiver
        themeChangeReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
                Log.d("OneHandKeyboard", "Theme change receiver unregistered")
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered, ignore
                Log.w("OneHandKeyboard", "Theme change receiver was not registered")
            }
        }
        themeChangeReceiver = null

        // Cleanup components
        if (::gestureIntegration.isInitialized) {
            gestureIntegration.cleanup()
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
