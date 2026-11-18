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
import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.gesture.DefaultGestureDetector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main InputMethodService for OneHandKeyboard - a 4x4 grid keyboard with 8-direction swipes.
 *
 * Lifecycle:
 * - [onCreate] builds every component once (after Hilt injection) and registers the theme receiver.
 * - [onCreateInputView] may run several times (rotation, config changes); it only builds the view.
 * - [onDestroy] releases the receiver and coroutine scopes.
 */
@AndroidEntryPoint
class OneHandKeyboardService : InputMethodService() {
    private var keyboardView: LinearLayout? = null

    @Inject
    lateinit var gestureDetector: DefaultGestureDetector

    @Inject
    lateinit var layoutManager: KeyboardLayoutManager

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var configFactory: KeyboardConfigurationFactory

    private var themeChangeReceiver: BroadcastReceiver? = null

    private lateinit var uiFactory: KeyboardUiFactory
    private lateinit var unifiedGridBuilder: UnifiedGridKeyboardBuilder
    private lateinit var gestureIntegration: GestureIntegration
    private lateinit var actionProcessor: KeyboardActionProcessor

    override fun onCreate() {
        super.onCreate() // Hilt injects here
        initializeComponents()
        registerThemeChangeReceiver()
    }

    override fun onCreateInputView(): View {
        return try {
            themeManager.refreshTheme()
            unifiedGridBuilder.createKeyboardLayout().also { keyboardView = it }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating input view", e)
            createFallbackView()
        }
    }

    /**
     * Initializes all components that require the service context. Runs once per service
     * instance, so state such as the cell height in [KeyboardUiFactory.layoutCalculator]
     * survives input view re-creations.
     */
    private fun initializeComponents() {
        uiFactory = KeyboardUiFactory(this, layoutManager, themeManager)

        gestureIntegration = GestureIntegration(this, layoutManager, gestureDetector)
        gestureIntegration.initializeGestureSystem()

        actionProcessor =
            KeyboardActionProcessor(
                service = this,
                configManager = layoutManager,
                layoutCalculator = uiFactory.layoutCalculator,
                onLayoutSwitch = { recreateKeyboard() },
                onCaseSwitch = { recreateKeyboard() },
                onHeightChanged = { recreateKeyboard() },
            )

        unifiedGridBuilder =
            UnifiedGridKeyboardBuilder(
                this,
                themeManager,
                uiFactory.styleProvider,
                layoutManager,
                gestureIntegration,
                actionProcessor,
                uiFactory,
                configFactory,
            )
    }

    /**
     * Recreates the keyboard when layout or case mode changes.
     */
    private fun recreateKeyboard() {
        keyboardView = unifiedGridBuilder.createKeyboardLayout()
        setInputView(keyboardView)
    }

    private fun registerThemeChangeReceiver() {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    if (intent?.action == ThemeManager.ACTION_THEME_CHANGED) {
                        themeManager.refreshTheme()
                        if (keyboardView != null) recreateKeyboard()
                    }
                }
            }
        val filter = IntentFilter(ThemeManager.ACTION_THEME_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        themeChangeReceiver = receiver
    }

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

    override fun onDestroy() {
        themeChangeReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Theme change receiver was not registered")
            }
        }
        themeChangeReceiver = null

        if (::gestureIntegration.isInitialized) {
            gestureIntegration.cleanup()
        }
        gestureDetector.reset()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "OneHandKeyboard"
    }
}
