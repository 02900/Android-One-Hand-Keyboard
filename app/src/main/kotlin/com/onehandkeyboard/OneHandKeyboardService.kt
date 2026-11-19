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
import androidx.core.view.inputmethod.EditorInfoCompat
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.InputCommitter
import com.onehandkeyboard.memes.Meme
import com.onehandkeyboard.memes.MemeCatalog
import com.onehandkeyboard.memes.MemeCategory
import com.onehandkeyboard.memes.MemeLibrary
import com.onehandkeyboard.memes.MemeSender
import com.onehandkeyboard.memes.MemeThumbnails
import com.onehandkeyboard.memes.TrendingMemes
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
import com.onehandkeyboard.voice.VoiceTyping
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
    private lateinit var voiceTyping: VoiceTyping
    private val memeExecutor = Executors.newSingleThreadExecutor()

    /** Know Your Meme's trending tab: only in builds with Internet (debug and online). */
    private var trendingMemes: TrendingMemes? = null
    private val trendingExecutor = Executors.newSingleThreadExecutor()

    /** Bumped each time the panel opens or closes, so a late scan does not reopen it. */
    private var memeRequest = 0

    /** What the open panel shows: the folder's scan and the trending tab, as each arrives. */
    private var memeFolderResult: MemeLibrary.Result? = null
    private var memeTrending: MemeCategory? = null

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
        voiceTyping.cancel()
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
                    view.onStripStatusTapped = voiceTyping::stop
                    view.updateWaiting = keyboardUpdates.waiting
                    keyboardView = view
                }
            val panel =
                MemePanelView(this, memeThumbnails).also { panel ->
                    panel.onBack = ::hideMemes
                    panel.onMemePicked = ::sendMeme
                    panel.onOpenSettings = ::openSettings
                    panel.onSizeChanged = { fraction, final ->
                        resizeMemes(fraction)
                        if (final) preferences.memePanelSize = fraction
                    }
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
        layoutCalculator = LayoutCalculator(this)
        memeLibrary = MemeLibrary(this)
        trendingMemes =
            if (BuildConfig.TRENDING_MEMES) {
                TrendingMemes(cacheDir, "OneHandKeyboard/${BuildConfig.VERSION_NAME} (Android)")
            } else {
                null
            }
        memeSender = MemeSender(this, download = trendingMemes?.let { it::download })
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
                onLayoutSwitch = {
                    spellEngines.preload(layoutManager.currentLanguage)
                    keyboardView?.showCurrentLayout()
                },
                onCaseSwitch = { keyboardView?.refreshLabels() },
                corrector = autoCorrector,
                onShowMemes = ::showMemes,
                onVoiceInput = {
                    autoCorrector.invalidate()
                    voiceTyping.start()
                },
            )

        voiceTyping =
            VoiceTyping(this, { currentInputConnection }, ::voiceLanguageTag).apply {
                onStatus = { keyboardView?.stripStatus = it }
                onMessage = ::toast
                onNeedsPermission = {
                    toast("Permite el micrófono para dictar")
                    startActivity(
                        Intent(this@OneHandKeyboardService, SettingsActivity::class.java)
                            .putExtra(SettingsActivity.EXTRA_REQUEST_MICROPHONE, true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        // While dictating, any key just ends the dictation (it is not typed).
        gestureIntegration.onOutput = { output ->
            if (voiceTyping.isListening) voiceTyping.stop() else actionProcessor.processKeyboardOutput(output)
        }
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
        voiceTyping.cancel()
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
        // An exact height (the keys', or taller with the slider): MATCH_PARENT would fill the screen.
        val size = preferences.memePanelSize
        panel.setSize(size)
        panel.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, memePanelHeight(keys.height, size))
        keys.visibility = View.INVISIBLE
        panel.visibility = View.VISIBLE
        panel.showLoading()
        val request = ++memeRequest
        memeFolderResult = null
        memeTrending = null
        val main = ContextCompat.getMainExecutor(this)
        val folder = preferences.memeFolderUri
        memeExecutor.execute {
            val result = memeLibrary.load(folder)
            main.execute {
                if (request != memeRequest) return@execute
                memeFolderResult = result
                showMemeTabs()
            }
        }
        trendingMemes?.let { trending ->
            trendingExecutor.execute {
                val tab = trending.load() ?: return@execute
                main.execute {
                    if (request != memeRequest) return@execute
                    memeTrending = tab
                    showMemeTabs()
                }
            }
        }
    }

    /** The panel grows upwards from the keys' height up to [MEME_PANEL_MAX_SCREEN] of the screen. */
    private fun memePanelHeight(
        keysHeight: Int,
        fraction: Float,
    ): Int {
        val tallest = maxOf(keysHeight, (resources.displayMetrics.heightPixels * MEME_PANEL_MAX_SCREEN).toInt())
        return keysHeight + ((tallest - keysHeight) * fraction.coerceIn(0f, 1f)).toInt()
    }

    private fun resizeMemes(fraction: Float) {
        val panel = memePanel ?: return
        val keys = keyboardView ?: return
        panel.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, memePanelHeight(keys.height, fraction))
    }

    /** Recents, trending and the folder's tabs; with no usable folder, the trending tab alone. */
    private fun showMemeTabs() {
        val folder = memeFolderResult ?: return // still scanning: the trending tab waits for it
        val trending = memeTrending
        val result =
            when {
                folder is MemeLibrary.Result.Loaded -> {
                    val tabs = MemeCatalog.withTrending(folder.categories, trending)
                    MemeLibrary.Result.Loaded(MemeCatalog.withRecents(tabs, preferences.recentMemes))
                }
                trending != null -> MemeLibrary.Result.Loaded(listOf(trending))
                else -> folder
            }
        memePanel?.show(result)
    }

    private fun hideMemes() {
        val panel = memePanel ?: return
        if (panel.visibility == View.GONE) return
        memeRequest++
        memeFolderResult = null
        memeTrending = null
        panel.visibility = View.GONE
        panel.clear()
        keyboardView?.visibility = View.VISIBLE
    }

    private fun sendMeme(meme: Meme) {
        val fieldTypes = currentInputEditorInfo?.let(EditorInfoCompat::getContentMimeTypes) ?: emptyArray()
        memeExecutor.execute {
            val prepared = memeSender.prepare(meme, fieldTypes)
            ContextCompat.getMainExecutor(this).execute {
                val outcome =
                    if (prepared == null) {
                        MemeSender.Outcome.FAILED
                    } else {
                        memeSender.commit(meme, prepared, currentInputConnection, currentInputEditorInfo)
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

    /** Dictation language: the keyboard language, in the phone's regional variant when it matches. */
    private fun voiceLanguageTag(): String {
        val language = layoutManager.currentLanguage.label.lowercase()
        val device = java.util.Locale.getDefault()
        return if (device.language == language) device.toLanguageTag() else language
    }

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
        if (::voiceTyping.isInitialized) voiceTyping.cancel()
        memeExecutor.shutdownNow()
        trendingExecutor.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "OneHandKeyboard"
        const val MEME_PANEL_MAX_SCREEN = 0.7f
        const val FALLBACK_PADDING_DP = 16f
        const val FALLBACK_TEXT_SP = 16f
    }
}
