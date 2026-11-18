package com.onehandkeyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import android.widget.LinearLayout
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.input.InputCommitter
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.ui.GestureHintProvider
import com.onehandkeyboard.ui.KeyFeedback
import com.onehandkeyboard.ui.KeyboardViewController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main InputMethodService for OneHandKeyboard - a 4x4 grid keyboard with 8-direction swipes.
 *
 * Lifecycle:
 * - [onCreate] builds every component once (after Hilt injection) and registers the theme receiver.
 * - [onCreateInputView] may run several times (rotation, config changes); it only builds the view.
 * - State changes (case, layer, height, theme) update the existing view in place.
 * - [onDestroy] releases the receiver and pending callbacks.
 */
@AndroidEntryPoint
class OneHandKeyboardService : InputMethodService() {
    @Inject
    lateinit var layoutManager: KeyboardLayoutManager

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var preferences: KeyboardPreferences

    private var themeChangeReceiver: BroadcastReceiver? = null

    private lateinit var uiFactory: KeyboardUiFactory
    private lateinit var gestureIntegration: GestureIntegration
    private lateinit var actionProcessor: KeyboardActionProcessor
    private lateinit var viewController: KeyboardViewController

    override fun onCreate() {
        super.onCreate() // Hilt injects here
        initializeComponents()
        registerThemeChangeReceiver()
    }

    override fun onCreateInputView(): View {
        return try {
            themeManager.refreshTheme()
            viewController.createView()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating input view", e)
            createFallbackView()
        }
    }

    /**
     * Initializes all components that require the service context. Runs once per service
     * instance, so their state survives input view re-creations.
     */
    private fun initializeComponents() {
        uiFactory = KeyboardUiFactory(this, layoutManager, themeManager, preferences)
        gestureIntegration = GestureIntegration(this, layoutManager, KeyFeedback(this, preferences))

        viewController =
            KeyboardViewController(
                context = this,
                layoutManager = layoutManager,
                themeManager = themeManager,
                styleProvider = uiFactory.styleProvider,
                hintProvider = uiFactory.hintProvider,
                layoutCalculator = uiFactory.layoutCalculator,
                gestureIntegration = gestureIntegration,
                prefs = preferences,
            )

        actionProcessor =
            KeyboardActionProcessor(
                service = this,
                configManager = layoutManager,
                layoutCalculator = uiFactory.layoutCalculator,
                onLayoutSwitch = { viewController.showCurrentLayout() },
                onCaseSwitch = { viewController.refreshLabels() },
                onHeightChanged = { viewController.applyCellHeight() },
                onToggleOneHand = { viewController.toggleOneHand() },
            )

        gestureIntegration.onOutput = actionProcessor::processKeyboardOutput
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            layoutManager.resetForNewInput(info?.inputType ?: InputType.TYPE_CLASS_TEXT)
            viewController.showCurrentLayout(animate = false)
        }
        viewController.previewEnabled = !InputCommitter.isPassword(info)
        uiFactory.hintProvider.enterLabel = GestureHintProvider.enterLabelFor(info)
        viewController.refreshLabels()
        updateAutoShift()
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        val tag = subtypeLanguageTag(newSubtype) ?: return
        val language = if (tag.startsWith("en")) Language.ENGLISH else Language.SPANISH
        if (layoutManager.setLanguage(language)) viewController.showCurrentLayout()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (newSelStart == newSelEnd) updateAutoShift()
    }

    @Suppress("DEPRECATION") // InputMethodSubtype.locale is the only option before API 24
    private fun subtypeLanguageTag(subtype: InputMethodSubtype?): String? {
        subtype ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            subtype.languageTag.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return subtype.locale.takeIf { it.isNotEmpty() }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        gestureIntegration.cancelAll()
    }

    /**
     * Capitalizes the next letter when the field asks for it at the cursor (sentence start,
     * after ". ", etc.). Fields that do not request capitalization (email, URL, password)
     * return 0 from `getCursorCapsMode` and are left alone.
     */
    private fun updateAutoShift() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo ?: return
        if (!layoutManager.isLettersMode() || info.inputType == InputType.TYPE_NULL) return
        val shouldCapitalize = ic.getCursorCapsMode(info.inputType) != 0
        if (layoutManager.updateAutoShift(shouldCapitalize)) viewController.refreshLabels()
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
                        viewController.applyTheme()
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
        super.onDestroy()
    }

    private companion object {
        const val TAG = "OneHandKeyboard"
    }
}
