package com.onehandkeyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.Gravity
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
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.emoji.EmojiCatalog
import com.onehandkeyboard.emoji.EmojiGroup
import com.onehandkeyboard.emoji.EmojiKitchen
import com.onehandkeyboard.emoji.EmojiMix
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.InputCommitter
import com.onehandkeyboard.memes.Meme
import com.onehandkeyboard.memes.MemeCatalog
import com.onehandkeyboard.memes.MemeLibrary
import com.onehandkeyboard.memes.MemeSearch
import com.onehandkeyboard.memes.MemeSender
import com.onehandkeyboard.memes.MemeThumbnails
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.spell.AutoCorrector
import com.onehandkeyboard.spell.FieldPolicy
import com.onehandkeyboard.spell.SpellEngines
import com.onehandkeyboard.ui.EmojiPanelView
import com.onehandkeyboard.ui.GestureHintProvider
import com.onehandkeyboard.ui.KeyFeedback
import com.onehandkeyboard.ui.KeyboardPalette
import com.onehandkeyboard.ui.KeyboardView
import com.onehandkeyboard.ui.LayoutCalculator
import com.onehandkeyboard.ui.MemePanelView
import com.onehandkeyboard.ui.MemeSearchBar
import com.onehandkeyboard.ui.SettingsActivity
import com.onehandkeyboard.update.KeyboardUpdates
import com.onehandkeyboard.update.SelfUpdater
import com.onehandkeyboard.voice.VoiceTyping
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
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
    private var emojiPanel: EmojiPanelView? = null
    private var searchBar: MemeSearchBar? = null

    /** The input view built by [onCreateInputView] (null when the fallback view is shown). */
    private var inputRoot: FrameLayout? = null

    /** What sits on screen: the panel, or the keys (with the search box while it is open). */
    private var keysColumn: View? = null

    private lateinit var memeLibrary: MemeLibrary
    private lateinit var memeSender: MemeSender
    private lateinit var memeThumbnails: MemeThumbnails
    private lateinit var keyboardUpdates: KeyboardUpdates
    private lateinit var voiceTyping: VoiceTyping
    private val memeExecutor = Executors.newSingleThreadExecutor()

    /** Know Your Meme's image search: only in builds with Internet (debug and online). */
    private var memeSearch: MemeSearch? = null
    private val searchExecutor = Executors.newSingleThreadExecutor()

    /** Bumped each time the panel opens, closes or searches, so a late answer does not show up. */
    private var memeRequest = 0

    /** The search whose results the panel shows: its query, results so far and next page. */
    private var searchQuery: String? = null
    private var searchResults: List<Meme> = emptyList()
    private var searchNext: Int? = null

    /** The emoji panel's data, read once (off the main thread) the first time it opens. */
    private class EmojiData(
        val groups: List<EmojiGroup>,
        /** Null in builds without Internet (the mixes are downloaded). */
        val kitchen: EmojiKitchen?,
    )

    @Volatile private var emojiData: EmojiData? = null

    /** True while the keys type into the meme search box instead of the app. */
    private val typingSearch get() = searchBar?.visibility == View.VISIBLE

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
            val panel = createMemePanel()
            val emojis = createEmojiPanel()
            val search =
                MemeSearchBar(this).also { bar ->
                    bar.onSearch = ::runSearch
                    bar.onCancel = ::cancelSearchBox
                    bar.applyPalette(KeyboardPalette.from(themeManager.currentTheme.value))
                    bar.visibility = View.GONE
                    searchBar = bar
                }
            val column =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(search, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    addView(keys, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    keysColumn = this
                }
            // The panel goes over the keys, which stay laid out (INVISIBLE) while it shows. Both sit
            // at the bottom of a view as tall as the screen (see updateWindowHeight).
            FrameLayout(this).apply {
                addView(column, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
                addView(panel, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
                addView(emojis, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
                panel.previewHost = this // the big preview floats above the panel, clear of the finger
                inputRoot = this
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating input view", e)
            inputRoot = null
            createFallbackView()
        }
    }

    // ------------------------------------------------------------------
    // Window: as tall as the screen, touchable only where the keyboard is
    // ------------------------------------------------------------------

    override fun setInputView(view: View) {
        super.setInputView(view)
        updateWindowHeight()
    }

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        updateWindowHeight()
    }

    /**
     * Makes the keyboard's window reach the top of the screen; the part above the keyboard is
     * transparent and lets touches through to the app ([onComputeInsets]). Android stops tracking
     * a finger at the edge of the window that got it, so without this the meme panel's grab bar
     * could only be dragged a little upwards at a time.
     */
    private fun updateWindowHeight() {
        val root = inputRoot ?: return
        val height = if (isFullscreenMode) WRAP_CONTENT else MATCH_PARENT
        window.window?.let { window ->
            val attributes = window.attributes
            if (attributes.height != MATCH_PARENT) {
                attributes.height = MATCH_PARENT
                window.attributes = attributes
            }
            window.findViewById<View>(android.R.id.inputArea)?.let { area ->
                area.layoutParams =
                    area.layoutParams?.also { params ->
                        params.height = height
                        (params as? LinearLayout.LayoutParams)?.gravity = Gravity.BOTTOM
                        (params as? FrameLayout.LayoutParams)?.gravity = Gravity.BOTTOM
                    }
            }
        }
        root.layoutParams = root.layoutParams?.also { it.height = height }
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val root = inputRoot ?: return
        if (isFullscreenMode || !root.isShown || root.height == 0) return
        val panel = listOfNotNull(memePanel, emojiPanel).firstOrNull { it.visibility == View.VISIBLE }
        val visibleHeight = (panel ?: keysColumn)?.height ?: return
        val location = IntArray(2)
        root.getLocationInWindow(location)
        val bottom = location[1] + root.height
        val top = (bottom - visibleHeight).coerceAtLeast(0)
        outInsets.contentTopInsets = top
        outInsets.visibleTopInsets = top
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        outInsets.touchableRegion.set(0, top, root.width, bottom)
    }

    private fun createActionProcessor() =
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
            onShowEmojis = ::showEmojis,
            onVoiceInput = {
                // Dictation writes into the app, not into the search box.
                if (!typingSearch) {
                    autoCorrector.invalidate()
                    voiceTyping.start()
                }
            },
            connection = { if (typingSearch) searchBar?.connection else currentInputConnection },
            editor = { if (typingSearch) searchBar?.editorInfo else currentInputEditorInfo },
            cleanPastedLinks = { preferences.cleanPastedLinks },
        )

    private fun createMemePanel() =
        MemePanelView(this, memeThumbnails).also { panel ->
            panel.onBack = ::hideMemes
            panel.onMemePicked = ::sendMeme
            panel.onOpenSettings = ::openSettings
            panel.onHeightChanging = { resizeMemes(it, remember = false) }
            panel.onHeightChosen = { resizeMemes(it, remember = true) }
            panel.searchEnabled = memeSearch != null
            panel.onSearchTapped = ::openSearchBox
            panel.onSearchClosed = ::showMemes
            panel.onLoadMore = ::loadSearchPage
            panel.applyPalette(KeyboardPalette.from(themeManager.currentTheme.value))
            panel.visibility = View.GONE
            memePanel = panel
        }

    private fun createEmojiPanel() =
        EmojiPanelView(this, memeThumbnails).also { panel ->
            panel.onBack = ::hideEmojis
            panel.onEmojiPicked = ::typeEmoji
            panel.onBackspace = { actionProcessor.processKeyboardOutput(KeyboardOutput.Action(KeyAction.Backspace)) }
            panel.onMixPicked = ::sendMix
            panel.applyPalette(KeyboardPalette.from(themeManager.currentTheme.value))
            panel.visibility = View.GONE
            emojiPanel = panel
        }

    /**
     * Initializes all components that require the service context. Runs once per service
     * instance, so their state survives input view re-creations.
     */
    private fun initializeComponents() {
        layoutCalculator = LayoutCalculator(this)
        memeLibrary = MemeLibrary(this)
        memeSearch =
            if (BuildConfig.MEME_SEARCH) MemeSearch("OneHandKeyboard/${BuildConfig.VERSION_NAME} (Android)") else null
        memeSender = MemeSender(this, download = memeSearch?.let { it::download })
        memeThumbnails =
            MemeThumbnails(this, ContextCompat.getMainExecutor(this), fetch = memeSearch?.let { it::fetch })
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

        actionProcessor = createActionProcessor()

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
            hideEmojis()
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
        hideEmojis()
        autoCorrector.startInput(FieldPolicy.NONE)
    }

    // ------------------------------------------------------------------
    // Meme library
    // ------------------------------------------------------------------

    /** Opens the panel with the library (recents and the folder's categories). */
    private fun showMemes() {
        val panel = memePanel ?: return
        if (keyboardView == null) return
        hideEmojis()
        gestureIntegration.cancelAll()
        closeSearchBox()
        clearSearch()
        showPanel(panel)
        panel.showLoading()
        val request = ++memeRequest
        val main = ContextCompat.getMainExecutor(this)
        val folder = preferences.memeFolderUri
        memeExecutor.execute {
            val result = memeLibrary.load(folder)
            main.execute {
                if (request != memeRequest) return@execute
                panel.show(
                    if (result is MemeLibrary.Result.Loaded) {
                        MemeLibrary.Result.Loaded(MemeCatalog.withRecents(result.categories, preferences.recentMemes))
                    } else {
                        result
                    },
                )
            }
        }
    }

    /** Puts the panel over the keys, at its remembered height. */
    private fun showPanel(panel: MemePanelView) {
        val keys = keyboardView ?: return
        // An exact height (the keys', or taller with the grab bar): MATCH_PARENT would fill the screen.
        val tallest = memePanelTallest(keys.height)
        val height = keys.height + ((tallest - keys.height) * preferences.memePanelSize).toInt()
        panel.setHeightRange(keys.height..tallest)
        panel.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, height, Gravity.BOTTOM)
        keys.visibility = View.INVISIBLE
        panel.visibility = View.VISIBLE
    }

    /** The panel grows upwards from the keys' height up to [MEME_PANEL_MAX_SCREEN] of the screen. */
    private fun memePanelTallest(keysHeight: Int) =
        maxOf(keysHeight, (resources.displayMetrics.heightPixels * MEME_PANEL_MAX_SCREEN).toInt())

    /**
     * Grab bar: resizes the panel while it is dragged (the window already reaches the top of the
     * screen, so only the panel changes) and, once released, remembers the size for next time.
     */
    private fun resizeMemes(
        requested: Int,
        remember: Boolean,
    ) {
        val panel = memePanel ?: return
        val keys = keyboardView ?: return
        val tallest = memePanelTallest(keys.height)
        val height = requested.coerceIn(keys.height, tallest)
        if (panel.layoutParams?.height != height) {
            panel.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, height, Gravity.BOTTOM)
        }
        if (!remember) return
        preferences.memePanelSize =
            if (tallest > keys.height) (height - keys.height).toFloat() / (tallest - keys.height) else 0f
    }

    private fun hideMemes() {
        val searching = typingSearch
        closeSearchBox()
        val panel = memePanel ?: return
        if (panel.visibility == View.GONE && !searching) return
        memeRequest++
        clearSearch()
        panel.visibility = View.GONE
        panel.clear()
        keyboardView?.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------------
    // Meme search (Know Your Meme)
    // ------------------------------------------------------------------

    /** Shows the keys with the search box above them; the keys type the query. */
    private fun openSearchBox() {
        val bar = searchBar ?: return
        gestureIntegration.cancelAll()
        memePanel?.visibility = View.GONE // keeps what it shows, for ✕
        keyboardView?.visibility = View.VISIBLE
        bar.start(searchQuery.orEmpty())
        bar.visibility = View.VISIBLE
        autoCorrector.startInput(FieldPolicy.NONE)
        if (layoutManager.updateAutoShift(false)) keyboardView?.refreshLabels()
    }

    /** Hides the search box; the keys write into the app again. */
    private fun closeSearchBox() {
        val bar = searchBar ?: return
        if (bar.visibility == View.GONE) return
        bar.visibility = View.GONE
        gestureIntegration.cancelAll()
        val info = currentInputEditorInfo
        autoCorrector.startInput(FieldPolicy.from(info, preferences.suggestionsEnabled, preferences.autocorrectEnabled))
        autoCorrector.refresh(currentInputConnection)
        updateAutoShift()
    }

    /** ✕ in the search box: back to the panel as it was. */
    private fun cancelSearchBox() {
        closeSearchBox()
        val panel = memePanel ?: return
        keyboardView?.visibility = View.INVISIBLE
        panel.visibility = View.VISIBLE
    }

    private fun runSearch(query: String) {
        val panel = memePanel ?: return
        closeSearchBox()
        showPanel(panel)
        searchQuery = query
        searchResults = emptyList()
        searchNext = 0
        panel.showSearching(query)
        loadSearchPage()
    }

    /** Loads the next page of the current search (the panel asks for it near the end of the list). */
    private fun loadSearchPage() {
        val search = memeSearch ?: return
        val query = searchQuery ?: return
        val offset = searchNext ?: return
        val request = ++memeRequest
        val main = ContextCompat.getMainExecutor(this)
        searchExecutor.execute {
            val page = search.search(query, offset)
            main.execute {
                if (request != memeRequest) return@execute
                val panel = memePanel ?: return@execute
                if (page == null) {
                    panel.showSearchFailed(query)
                    return@execute
                }
                searchResults = (searchResults + page.memes).distinctBy { it.uri }
                searchNext = page.nextOffset
                if (searchResults.isEmpty() && page.nextOffset != null) {
                    loadSearchPage() // this page only had hidden images: try the next one
                } else {
                    panel.showSearchResults(query, searchResults, hasMore = page.nextOffset != null)
                }
            }
        }
    }

    private fun clearSearch() {
        searchQuery = null
        searchResults = emptyList()
        searchNext = null
    }

    // ------------------------------------------------------------------
    // Emojis and Emoji Kitchen
    // ------------------------------------------------------------------

    /** Opens the emoji panel, as tall as the keys. */
    private fun showEmojis() {
        val panel = emojiPanel ?: return
        val keys = keyboardView ?: return
        hideMemes()
        gestureIntegration.cancelAll()
        panel.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, keys.height, Gravity.BOTTOM)
        keys.visibility = View.INVISIBLE
        panel.visibility = View.VISIBLE
        val main = ContextCompat.getMainExecutor(this)
        memeExecutor.execute {
            val data = emojiData ?: loadEmojis().also { emojiData = it }
            main.execute {
                if (panel.visibility != View.VISIBLE) return@execute
                panel.show(EmojiCatalog.withRecents(data.groups, preferences.recentEmojis))
                panel.kitchen = data.kitchen
            }
        }
    }

    /** Reads the emoji list (only the emojis this phone can draw) and, with Internet, the mixes. */
    private fun loadEmojis(): EmojiData {
        val paint = Paint()
        val groups =
            try {
                assets.open(EMOJI_LIST).bufferedReader().useLines { EmojiCatalog.parse(it, paint::hasGlyph) }
            } catch (e: IOException) {
                Log.e(TAG, "Cannot read the emoji list", e)
                emptyList()
            }
        val kitchen =
            try {
                if (memeSearch != null) assets.open(EMOJI_KITCHEN).use(EmojiKitchen::read) else null
            } catch (e: IOException) {
                Log.e(TAG, "Cannot read the Emoji Kitchen table", e)
                null
            }
        return EmojiData(groups, kitchen)
    }

    private fun hideEmojis() {
        val panel = emojiPanel ?: return
        if (panel.visibility == View.GONE) return
        panel.visibility = View.GONE
        panel.reset()
        memeThumbnails.clear()
        keyboardView?.visibility = View.VISIBLE
    }

    private fun typeEmoji(emoji: String) {
        actionProcessor.processKeyboardOutput(KeyboardOutput.Character(emoji))
        preferences.recentEmojis = EmojiCatalog.pushRecent(preferences.recentEmojis, emoji)
    }

    /**
     * Sends an Emoji Kitchen mix as a sticker. Once the app takes it, the two emojis it mixes are
     * removed from the text if they are still right before the cursor (they became the sticker).
     */
    private fun sendMix(mix: EmojiMix) {
        val sticker = Meme(mix.url, "${mix.title}.png", "image/png")
        sendMeme(sticker, remember = false) { removeBeforeCursor(mix.first + mix.second) }
    }

    private fun removeBeforeCursor(text: String) {
        val ic = currentInputConnection ?: return
        if (ic.getTextBeforeCursor(text.length, 0)?.toString() == text) {
            autoCorrector.invalidate()
            ic.deleteSurroundingText(text.length, 0)
        }
    }

    private fun sendMeme(
        meme: Meme,
        remember: Boolean = true,
        onSent: () -> Unit = {},
    ) {
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
                    MemeSender.Outcome.SENT -> onSent()
                    MemeSender.Outcome.COPIED -> toast("Esta app no acepta imágenes aquí: quedó copiada, pégala")
                    MemeSender.Outcome.FAILED -> toast("No se pudo enviar el meme")
                }
                if (remember && outcome != MemeSender.Outcome.FAILED) {
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
        if (typingSearch) return
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
                        val palette = KeyboardPalette.from(themeManager.currentTheme.value)
                        memePanel?.applyPalette(palette)
                        emojiPanel?.applyPalette(palette)
                        searchBar?.applyPalette(palette)
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
        searchExecutor.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "OneHandKeyboard"
        const val MEME_PANEL_MAX_SCREEN = 0.7f
        const val EMOJI_LIST = "emoji/emoji.tsv"
        const val EMOJI_KITCHEN = "emoji/kitchen.bin"
        const val FALLBACK_PADDING_DP = 16f
        const val FALLBACK_TEXT_SP = 16f
    }
}
