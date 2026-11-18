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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.InputCommitter
import com.onehandkeyboard.memes.Meme
import com.onehandkeyboard.memes.MemeCatalog
import com.onehandkeyboard.memes.MemeLibrary
import com.onehandkeyboard.memes.MemeSender
import com.onehandkeyboard.memes.MemeThumbnails
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.spell.AutoCorrector
import com.onehandkeyboard.spell.FieldPolicy
import com.onehandkeyboard.spell.SpellEngines
import com.onehandkeyboard.ui.GestureHintProvider
import com.onehandkeyboard.ui.KeyFeedback
import com.onehandkeyboard.ui.KeyboardPalette
import com.onehandkeyboard.ui.KeyboardView
import com.onehandkeyboard.ui.LayoutCalculator
import com.onehandkeyboard.ui.MemePanelView
import com.onehandkeyboard.ui.SettingsActivity
import com.onehandkeyboard.update.KeyboardUpdates
import com.onehandkeyboard.update.SelfUpdater
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executors
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

    @Inject
    lateinit var spellEngines: SpellEngines

    private var themeChangeReceiver: BroadcastReceiver? = null

    private lateinit var layoutCalculator: LayoutCalculator
    private lateinit var labels: GestureHintProvider
    private lateinit var gestureIntegration: GestureIntegration
    private lateinit var actionProcessor: KeyboardActionProcessor
    private lateinit var autoCorrector: AutoCorrector

    /** Current input view; recreated by the system on rotation/config changes. */
    private var keyboardView: KeyboardView? = null
    private var memePanel: MemePanelView? = null

    private lateinit var memeLibrary: MemeLibrary
    private lateinit var memeSender: MemeSender
    private lateinit var memeThumbnails: MemeThumbnails
    private lateinit var keyboardUpdates: KeyboardUpdates
    private val memeExecutor = Executors.newSingleThreadExecutor()

    /** Bumped each time the panel opens or closes, so a late scan does not reopen it. */
    private var memeRequest = 0

    override fun onCreate() {
        super.onCreate() // Hilt injects here
        initializeComponents()
        registerThemeChangeReceiver()
        announceUpdate()
    }

    /** After a self-update the keyboard restarts: say which build is running now. */
    private fun announceUpdate() {
        val previous = preferences.lastRunCommit
        if (previous == BuildConfig.GIT_COMMIT) return
        preferences.lastRunCommit = BuildConfig.GIT_COMMIT
        preferences.updateNeedsConfirmation = false
        if (previous != null) toast("Teclado actualizado · ${BuildConfig.GIT_COMMIT}")
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        keyboardUpdates.onKeyboardHidden()
    }

    @Suppress("TooGenericExceptionCaught") // any failure must still leave the user a usable view
    override fun onCreateInputView(): View {
        return try {
            themeManager.refreshTheme()
            gestureIntegration.cancelAll()
            val keys =
                KeyboardView(
                    context = this,
                    layoutManager = layoutManager,
                    themeManager = themeManager,
                    labels = labels,
                    layoutCalculator = layoutCalculator,
                    gestures = gestureIntegration,
                    prefs = preferences,
                ).also { view ->
                    view.onSuggestionPicked = actionProcessor::pickSuggestion
                    view.updateWaiting = keyboardUpdates.waiting
                    keyboardView = view
                }
            val panel =
                MemePanelView(this, memeThumbnails).also { panel ->
                    panel.onBack = ::hideMemes
                    panel.onMemePicked = ::sendMeme
                    panel.onOpenSettings = ::openSettings
                    panel.applyPalette(KeyboardPalette.from(themeManager.currentTheme.value))
                    panel.visibility = View.GONE
                    memePanel = panel
                }
            // The panel goes over the keys, which stay laid out (INVISIBLE) while it shows.
            FrameLayout(this).apply {
                addView(keys, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                addView(panel, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
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
        layoutCalculator = LayoutCalculator(this, preferences)
        memeLibrary = MemeLibrary(this)
        memeSender = MemeSender(this)
        memeThumbnails = MemeThumbnails(this, ContextCompat.getMainExecutor(this))
        keyboardUpdates =
            KeyboardUpdates(SelfUpdater(this, preferences), preferences, ContextCompat.getMainExecutor(this))
        keyboardUpdates.onWaitingChanged = { keyboardView?.updateWaiting = it }
        labels = GestureHintProvider(layoutManager)
        gestureIntegration = GestureIntegration(this, layoutManager, KeyFeedback(this, preferences))

        autoCorrector =
            AutoCorrector(
                suggester = { spellEngines.suggester(layoutManager.currentLanguage) },
                userDictionary = { spellEngines.userDictionary(layoutManager.currentLanguage) },
                background = spellEngines.executor,
                main = ContextCompat.getMainExecutor(this),
            )
        autoCorrector.onSuggestions = { keyboardView?.suggestions = it }
        spellEngines.preload(layoutManager.currentLanguage)

        actionProcessor =
            KeyboardActionProcessor(
                service = this,
                configManager = layoutManager,
                layoutCalculator = layoutCalculator,
                onLayoutSwitch = {
                    spellEngines.preload(layoutManager.currentLanguage)
                    keyboardView?.showCurrentLayout()
                },
                onCaseSwitch = { keyboardView?.refreshLabels() },
                onHeightChanged = { keyboardView?.applyCellHeight() },
                corrector = autoCorrector,
                onShowMemes = ::showMemes,
            )

        gestureIntegration.onOutput = actionProcessor::processKeyboardOutput
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            hideMemes()
            layoutManager.resetForNewInput(info?.inputType ?: InputType.TYPE_CLASS_TEXT)
            keyboardView?.showCurrentLayout(animate = false)
            keyboardUpdates.onKeyboardShown()
        }
        keyboardView?.previewEnabled = !InputCommitter.isPassword(info)
        keyboardView?.expertMode = preferences.expertMode
        labels.enterLabel = GestureHintProvider.enterLabelFor(info)
        keyboardView?.refreshLabels()
        keyboardView?.applySuggestionsSetting()
        spellEngines.preload(layoutManager.currentLanguage)
        autoCorrector.startInput(FieldPolicy.from(info, preferences.suggestionsEnabled, preferences.autocorrectEnabled))
        autoCorrector.refresh(currentInputConnection)
        updateAutoShift()
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        val tag = subtypeLanguageTag(newSubtype) ?: return
        val language = if (tag.startsWith("en")) Language.ENGLISH else Language.SPANISH
        if (layoutManager.setLanguage(language)) {
            spellEngines.preload(language)
            keyboardView?.showCurrentLayout()
        }
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
        if (newSelStart == newSelEnd) {
            updateAutoShift()
            autoCorrector.refresh(currentInputConnection)
        }
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
        hideMemes()
        autoCorrector.startInput(FieldPolicy.NONE)
    }

    // ------------------------------------------------------------------
    // Meme library
    // ------------------------------------------------------------------

    private fun showMemes() {
        val panel = memePanel ?: return
        val keys = keyboardView ?: return
        gestureIntegration.cancelAll()
        // Exactly the keys' height: a MATCH_PARENT panel would grow the window to the full screen.
        panel.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, keys.height)
        keys.visibility = View.INVISIBLE
        panel.visibility = View.VISIBLE
        panel.showLoading()
        val request = ++memeRequest
        val folder = preferences.memeFolderUri
        val recents = preferences.recentMemes
        memeExecutor.execute {
            val loaded = memeLibrary.load(folder)
            val result =
                if (loaded is MemeLibrary.Result.Loaded) {
                    MemeLibrary.Result.Loaded(MemeCatalog.withRecents(loaded.categories, recents))
                } else {
                    loaded
                }
            ContextCompat.getMainExecutor(this).execute { if (request == memeRequest) memePanel?.show(result) }
        }
    }

    private fun hideMemes() {
        val panel = memePanel ?: return
        if (panel.visibility == View.GONE) return
        memeRequest++
        panel.visibility = View.GONE
        panel.clear()
        keyboardView?.visibility = View.VISIBLE
    }

    private fun sendMeme(meme: Meme) {
        memeExecutor.execute {
            val file = memeSender.prepare(meme)
            ContextCompat.getMainExecutor(this).execute {
                val outcome =
                    if (file == null) {
                        MemeSender.Outcome.FAILED
                    } else {
                        memeSender.commit(meme, file, currentInputConnection, currentInputEditorInfo)
                    }
                when (outcome) {
                    MemeSender.Outcome.SENT -> Unit
                    MemeSender.Outcome.COPIED -> toast("Esta app no acepta imágenes aquí: quedó copiada, pégala")
                    MemeSender.Outcome.FAILED -> toast("No se pudo enviar el meme")
                }
                if (outcome != MemeSender.Outcome.FAILED) {
                    preferences.recentMemes = MemeCatalog.pushRecent(preferences.recentMemes, meme.uri)
                }
            }
        }
    }

    private fun openSettings() {
        hideMemes()
        startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    /**
     * Capitalizes the next letter when the field asks for it at the cursor (sentence start,
     * after ". ", etc.). Fields that do not request capitalization (email, URL, password)
     * return 0 from `getCursorCapsMode` and are left alone.
     */
    private fun updateAutoShift() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo ?: return
        if (layoutManager.currentMode != LayoutMode.LETTERS || info.inputType == InputType.TYPE_NULL) return
        val shouldCapitalize = ic.getCursorCapsMode(info.inputType) != 0
        if (layoutManager.updateAutoShift(shouldCapitalize)) keyboardView?.refreshLabels()
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
                        keyboardView?.applyTheme()
                        memePanel?.applyPalette(KeyboardPalette.from(themeManager.currentTheme.value))
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
            val padding = (FALLBACK_PADDING_DP * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(
                android.widget.TextView(this@OneHandKeyboardService).apply {
                    text = "Keyboard Error - Please restart"
                    textSize = FALLBACK_TEXT_SP
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
                Log.w(TAG, "Theme change receiver was not registered", e)
            }
        }
        themeChangeReceiver = null

        if (::gestureIntegration.isInitialized) {
            gestureIntegration.cleanup()
        }
        if (::memeThumbnails.isInitialized) memeThumbnails.shutdown()
        if (::keyboardUpdates.isInitialized) keyboardUpdates.shutdown()
        memeExecutor.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "OneHandKeyboard"
        const val FALLBACK_PADDING_DP = 16f
        const val FALLBACK_TEXT_SP = 16f
    }
}
