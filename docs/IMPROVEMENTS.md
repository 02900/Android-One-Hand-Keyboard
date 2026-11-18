# Audit and improvement plan — OneHandKeyboard

> Audit of the current state (commit `2e0801d`). Goal: fix the bugs and make typing feel **fluid, immediate and forgiving**, not rigid.

---

## 0. Diagnosis in one sentence

The keyboard **gives the user no feedback while typing**: it doesn't highlight the key on press, doesn't vibrate, doesn't show which character will come out during a swipe and doesn't forgive an imprecise gesture. On top of that, it **rebuilds the whole view** on every mode change (shift, layer or height). Those two things explain almost all of the rigid feel.

---

## 1. Bugs — priority P0

| # | Bug | Where | Effect |
|---|-------|-------|--------|
| 1.1 | **Gesture state is global and doesn't distinguish pointers.** `gestureStartX/Y` and `gestureOriginCellIndex` are shared across all cells, `event.action` is used instead of `actionMasked` and `ACTION_CANCEL` is not handled. | `GestureIntegration.kt:306-309, 475` | When typing fast with roll-over (one finger goes down before the previous one lifts), the second `DOWN` overwrites the origin and the first finger's `UP` computes the delta against a different point: you get **wrong characters or phantom swipes**. |
| 1.2 | **The swipe threshold is in fixed pixels** (`swipeThreshold = 50f`). | `GestureIntegration.kt:310` | On xxxhdpi screens that's about 12 dp (too sensitive) and on hdpi about 33 dp (too stiff). The feel changes from phone to phone. |
| 1.3 | **A swipe in a direction with no action does nothing.** | `GestureIntegration.kt:575-579` | On cells with 1 or 2 directions (e.g. cell 0 only has `SE`), a slightly crooked swipe or a "shaky tap" over 50 px is silently lost. This is the main reason the keyboard feels "stiff". |
| 1.4 | **The whole view tree is rebuilt** with `setInputView()` on every shift, layer or height change. | `OneHandKeyboardService.kt:140-144` | Flicker, about 100 `View`s created again, loss of the touch in progress, and jank. Typing **a single capital letter** forces 2 rebuilds. |
| 1.5 | **The theme receiver is registered on every `onCreateInputView`**, and a new `CoroutineScope` is created each time as well. | `OneHandKeyboardService.kt:109-135`, `GestureIntegration.kt:316` | `onCreateInputView` is called again on rotation or configuration change. The previous receivers and scopes are never released (leak), and a theme change triggers N rebuilds. |
| 1.6 | **Keystroke logging to logcat**: every character, pasted text and copied text are logged. There are no ProGuard rules stripping the `Log` calls. | `KeyboardActionProcessor.kt:56, 254, 281`, `GestureIntegration.kt:588, 597` | **Serious privacy problem for an IME**: passwords and messages end up in logcat, in the release build too. |
| 1.7 | **Enter always does `commitText("\n")`** and Submit always sends `IME_ACTION_SEND`. `EditorInfo.imeOptions` is ignored. | `KeyboardActionProcessor.kt:179-200` | In single-line fields (search box, URL, login), Enter does nothing, and "Submit" doesn't trigger the action the field expects (`SEARCH`, `GO`, `NEXT`, `DONE`). |
| 1.8 | **Backspace uses `deleteSurroundingText(1, 0)`.** | `KeyboardActionProcessor.kt:155` | It splits emojis and surrogate pairs (leaving a broken character) and doesn't work in apps that expect key events (terminals, some WebViews). |
| 1.9 | **Duplicate swipe key**: in symbols cell 8, `SWIPE W` is defined twice (`*` and `<`). The `Map` keeps the last one. | `KeyboardConfigurationFactory.kt` (`createSymbolsGrid4x4`, cell 8) | **`*` is unreachable.** `CellConfiguration` validation doesn't catch it because the `Map` has already dropped the duplicate. |
| 1.10 | **The English layout is unreachable and incomplete**: `switchLanguage()` is never called and the letters `u`, `i`, `o`, `p` are missing. | `KeyboardLayoutManager.kt:221`, `createEnglishGrid4x4` | The `en_US` subtype declared in `method.xml` is useless. |
| 1.11 | **Cell height doesn't persist**: `LayoutCalculator` lives inside `KeyboardUiFactory`, which is recreated on every `onCreateInputView`. | `KeyboardUiFactory.kt:405` | The user adjusts the height and loses it on rotation or service restart. Also, the adjustment is **1 dp per swipe**, with a full rebuild at each step. |
| 1.12 | **No accented vowels (á é í ó ú ü) or ¿ ¡** in the Spanish layout, which is the default language (es-MX). | `createLettersGrid4x4` | Spanish can't be typed correctly. |
| 1.13 | **`/` is missing** from symbols (as are `\`, `~` and `` ` ``). | `createSymbolsGrid4x4` | URLs, dates and paths can't be typed. |
| 1.14 | **"Clear all" is on an NW swipe of the shift key, with no confirmation or undo.** | Cell 7 of every layout | An accidental brush wipes the whole message. |
| 1.15 | `calculateCellBounds` looks for `getChildAt(1)` in a `LinearLayout` that has only one child (`GridLayout`). | `GestureIntegration.kt:369` | It always fails ("Cannot find mainRow"). It's dead code that runs on every `onStartInputView`. |
| 1.16 | `onComputeInsets` forces `contentTopInsets = 0`, `visibleTopInsets = 0` and `TOUCHABLE_INSETS_VISIBLE`. | `OneHandKeyboardService.kt:241-251` | Needs checking on a device: it may make the IME's touch area cover the app above the keyboard, or make the app miscalculate its space. Most likely it's better to keep the default behavior. |
| 1.17 | `onEvaluateInputViewShown()` always returns `true`. | `OneHandKeyboardService.kt:253` | The on-screen keyboard shows up even when a physical keyboard is connected. |
| 1.18 | `runBlocking { currentTheme.first() }` is called several times **per cell** on the main thread. | `ButtonStyleProvider.kt:479`, `UnifiedGridKeyboardBuilder.kt:49` | It's a `StateFlow`: `.value` is enough. `runBlocking` on the UI thread is unnecessary and risky. |
| 1.19 | Cell borders use `setStroke(1px)` on all 4 sides of each cell. | `ButtonStyleProvider.kt:526-544` | Inner lines end up double (2 px) and outer ones single. The thickness doesn't scale with density either. |
| 1.20 | Minor inconsistencies: the default theme is called `"Default Light"` but loads `MonokaiDark`; in symbols the shift key does nothing useful; `ActionConfig.Text` lowercases mixed-case texts. | `ThemeManager.kt:30, 37`, etc. | Confusing for both user and maintainer. |

---

## 2. Fluidity: what makes it feel rigid and how to fix it

### 2.1 Immediate response on touch (the most important part)
Today nothing visible happens until `ACTION_UP` (the code itself says *"No visual feedback on press for a flat look"*). The brain needs confirmation in under 20 ms.

- **Pressed state on `ACTION_DOWN`**: change the cell background to a `pressed` shade (add a `backgroundPressed` token to `ColorTokens`, or compute it with the existing, unused `adjustBrightness`). Return to the normal shade on `UP` or `CANCEL` with a short fade (~80 ms).
- **Haptics**: the `VIBRATE` permission is already declared but unused.
  - `DOWN`: `view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)`.
  - On crossing the swipe threshold or changing direction: `HapticFeedbackConstants.CLOCK_TICK` (or `SEGMENT_FREQUENT_TICK` on API 34+).
  - Make it configurable (on/off and intensity).
- **Optional sound**: `AudioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD / FX_KEYPRESS_DELETE / FX_KEYPRESS_SPACEBAR)`, respecting the system setting.

### 2.2 Live swipe preview
Today the user **guesses** which character will come out. On `ACTION_MOVE`:
1. When the displacement passes the threshold, compute the direction, **highlight the target label** (~1.4x scale, accent color) and dim the others.
2. Show a **preview bubble** above the cell with the character that will be typed (a `PopupWindow` or a `TextView` overlaid on the root).
3. If the finger returns to the center, the preview goes back to the tap character: the user can **correct before releasing**.
4. If the direction has no action, show the "ghost" target it will snap to (see 2.3).

### 2.3 Forgiving gestures
- **Threshold in dp and relative to the cell**: `threshold = max(ViewConfiguration.scaledTouchSlop * 1.5, 0.22 × min(anchoCelda, altoCelda))`. With 60 dp cells that gives about 13 to 18 dp.
- **Snap to the nearest configured direction**: if the computed direction has no action, search that cell's configured directions for the closest one by angle, up to ±67°. If there is none, treat it as a **tap**. This eliminates bug 1.3.
- **Uniform 45° angular sectors** with `atan2` instead of the `absX > absY * 2` heuristic. Today the cardinals cover about 53° and the diagonals about 37°, which is why diagonals are hard.
- **Hysteresis** of ~8° so the direction doesn't flicker at sector edges (`GestureConfig.directionHysteresis` already exists but is unused).
- Use `event.eventTime` instead of `System.currentTimeMillis()`.

### 2.4 Correct multitouch and roll-over
Keep state **per `pointerId`** (or, at minimum, per cell) and handle `ACTION_POINTER_DOWN/UP` and `ACTION_CANCEL`. That way you can type fast with the thumb without waiting for the previous finger to lift. Sketch:

```kotlin
private data class Track(val cell: Int, val x0: Float, val y0: Float, val t0: Long, var dir: Direction = Direction.CENTER)
private val tracks = SparseArray<Track>()

fun onTouch(view: View, e: MotionEvent, cell: Int): Boolean {
    val i = e.actionIndex
    val id = e.getPointerId(i)
    when (e.actionMasked) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
            tracks.put(id, Track(cell, e.getX(i), e.getY(i), e.eventTime))
            view.isPressed = true
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        MotionEvent.ACTION_MOVE -> for (p in 0 until e.pointerCount) {
            val t = tracks[e.getPointerId(p)] ?: continue
            val newDir = resolveDirection(t, e.getX(p) - t.x0, e.getY(p) - t.y0) // with threshold, snap and hysteresis
            if (newDir != t.dir) { t.dir = newDir; showPreview(t); view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> tracks[id]?.let { commit(it); tracks.remove(id); view.isPressed = false; hidePreview() }
        MotionEvent.ACTION_CANCEL -> { tracks.clear(); view.isPressed = false; hidePreview() }
    }
    return true
}
```

> Note: with one `OnTouchListener` per cell, Android already splits events between views (`splitMotionEvents`), so keeping state per cell is enough. With a single view (see 4.1) you need the per-`pointerId` tracking from the sketch.

### 2.5 Zero rebuilds: update in place
Build the view **once**. Keep references to each cell's `TextView`s and, on a state change:
- **Shift**: only change the labels' `text`, without creating new views.
- **Letters ↔ Symbols**: refill the same cells with the new configuration. If the `colSpan`s change, keep both grids pre-created and toggle them with `visibility` plus a 100 ms crossfade (`animate().alpha()`).
- **Height**: change the cells' `layoutParams.height` and call `requestLayout()`, without `setInputView`.
- **Theme**: reapply colors to the existing views.

### 2.6 Keys that feel "alive"
- **Backspace with repeat**: hold → 350 ms delay, then accelerating repeat (from 80 ms to 30 ms). **Swipe left** on Backspace deletes the previous word; swipe right undoes the last deletion.
- **Space bar as a trackpad**: dragging horizontally moves the cursor (1 character every ~12 dp, with `sendDownUpKeyEvents(KEYCODE_DPAD_LEFT/RIGHT)` or `setSelection`). This is what helps most when correcting one-handed.
- **Double space → ". "** with auto-capitalization afterwards.
- **Smart shift**:
  - 1 tap = uppercase **for the next letter only** (actually enable `CAPITALIZE` / `isCapitalizeNext`, which exist today but are never activated).
  - Double tap = caps lock.
  - Auto-capitalization at the start of a field and after `. ? !` using `currentInputConnection.getCursorCapsMode(editorInfo.inputType)`.
  - Show the state on the key (empty / filled / underlined arrow).

### 2.7 Continuous height adjustment
Replace "1 dp per swipe" with a **resize mode**: a long-press on the layer key activates a handle that you drag vertically with live height. Persist the value in `SharedPreferences`/DataStore. Also add a **one-handed mode** option: 75–85 % width aligned left or right, with a button to switch sides. It's ironic for a "One Hand Keyboard" to take up the full width on a large phone.

### 2.8 Micro-animations (subtle, < 120 ms)
- Scale to 0.96 on `DOWN` and back to 1.0 on `UP` (`animate().scaleX/Y().setDuration(60)`).
- Swipe target label: interpolated scale and color.
- Layer change: crossfade the content, not an abrupt jump.
- Use `ViewPropertyAnimator` or `ValueAnimator`, never recreate views.

---

## 3. Missing functionality for real-world use

1. **Spanish accents and punctuation**: add them as swipes on the vowels (e.g. `a` + swipe N = `á`), or with a `´` **dead key** that accents the next vowel. Add `¿`, `¡` and `ü`.
2. **Respect `EditorInfo`** in `onStartInputView`:
   - `TYPE_CLASS_NUMBER` / `PHONE` → open directly in symbols.
   - `TYPE_TEXT_VARIATION_EMAIL_ADDRESS` / `URI` → put `@` `.` `/` within easy reach.
   - Passwords (`TYPE_TEXT_VARIATION_PASSWORD`, `VISIBLE_PASSWORD`, `WEB_PASSWORD`) → no preview bubble and **no logging at all**.
   - Enter label and icon based on `imeOptions` (↵, 🔍, →, ✓) and `IME_FLAG_NO_ENTER_ACTION`.
   - Reset shift and layer when switching fields.
3. **Correct Enter**:
   ```kotlin
   val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
   val noEnterAction = info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
   if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action)
   else ic.sendKeyEvent(...KEYCODE_ENTER down/up...)
   ```
4. **Robust Backspace**: if there is no selection, use `sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)`, or compute the length of the previous code point with `Character.charCount(Character.codePointBefore(...))` so emojis aren't split.
5. Accessible **language switching** (swipe on the space bar, or follow the system subtype with `onCurrentInputMethodSubtypeChanged`) and complete the English layout.
6. **Clear all / copy all** with confirmation or undo, and use `ExtractedText` (`getExtractedText`) instead of the 10,000-character limit.
7. **Persistence** of language, height, one-handed mode, haptics and sound.
8. **Key to switch IME** (`switchToNextInputMethod`): `supportsSwitchingToNextInputMethod="true"` is already declared but no key uses it.
9. **Accessibility** (the README promises it): `contentDescription` per cell, an `AccessibilityDelegate` that announces the target character, and support for TalkBack exploration.

---

## 4. Performance and architecture

### 4.1 A single Canvas-drawn view (phase 2, the biggest performance win)
Today there are about 16 `FrameLayout`s with up to 9 `TextView`s each: more than 100 views, measured and laid out on every rebuild. A custom `KeyboardView : View` that:
- draws cells and labels in `onDraw` with precomputed `Paint` and `StaticLayout`,
- handles all touches in a single `onTouchEvent` with `pointerId`,
- invalidates only the rect of the cell that changes (`invalidate(rect)`),

gives minimal latency and power usage, and easier animations (everything is an interpolated value in the draw). It's the approach production IMEs use.

### 4.2 Logging
- Wrap all logs in `if (BuildConfig.DEBUG)` or use Timber with a debug-only `Tree`.
- Add to `proguard-rules.pro`:
  ```
  -assumenosideeffects class android.util.Log { public static int v(...); public static int d(...); public static int i(...); }
  ```
- **Never** log the user's text, not even in debug.
- Remove the `println`s in `DefaultGestureDetector` and the logs on every `ACTION_MOVE`.

### 4.3 Lifecycle
- Create the components **once** in `onCreate()` and in `onCreateInputView()` only create or reuse the view.
- Register the theme receiver in `onCreate()` and unregister it in `onDestroy()`. Better still, observe `themeManager.currentTheme` (it's a `StateFlow`) with a service scope and drop the broadcast.
- Cancel `gestureScope` in `cleanup()` (today only the job is cancelled).

### 4.4 Dead or duplicate code worth deleting
- `DefaultGestureDetector` / `core:gesture`: it's injected and collected, but nothing consumes it (`TODO`). Either integrate it properly (it already has `CANCEL`, velocity and hysteresis) or remove it.
- `GestureIntegration.createDomainKeyboardLayout()` (fake 9-cell layout with `'a'`), `calculateCellBounds`, `processGestureDirectly` (wrapper with no value).
- `actions/ActionProcessor.kt` (unused and duplicates the logic of `GestureIntegration.handleGestureAction`).
- `KeyboardLayoutManager.getCurrentKeyData()` and its 3 data providers (legacy 3x3).
- `ui/SimpleKeyboardGrid.kt` (Compose, unused), `createLegacyButton`, `getLegacyGestureHints`, `createTitleView`, `calculateSquareCellSize`.
- Empty modules: `core:text`, `core:data`, `core:analytics`, `feature:settings` (or start using them).
- The three identical label `when`s in `GestureHintProvider` (`getConfigurationHints`, `getDirectionalLabel`, `getCenterLabel`): centralize them in a single `ActionConfig.label()` function.
- The `ActionConfig → KeyboardOutput` mapping in `GestureIntegration`: move it to a pure, testable function.

### 4.5 Safer configuration
- Build cells with a DSL or builder that **throws on duplicate directions** (bug 1.9), e.g. `cell(8) { tap('7'); swipe(W, '*'); swipe(W, '<') /* → error */ }`.
- A unit test that checks every layout contains the full alphabet and the essential symbols (it would have caught bugs 1.10, 1.12 and 1.13).
- Cache configurations: today `getConfiguration()` creates new objects on every layer change.

### 4.6 Tests and CI
- The current service tests only check that the class exists. Add:
  - tests for `resolveDirection` (threshold, sectors, snapping to the nearest direction, hysteresis),
  - action mapping tests,
  - Robolectric tests of `onStartInputView` with different `EditorInfo`s,
  - a roll-over test with two pointers.
- Move CI to JDK 17 (AGP 8 requires it).

---

## 5. Phased implementation plan

### Phase 1 — Critical fixes (1–2 days)
- [x] 1.6 Remove the user-text logs and add `-assumenosideeffects` to ProGuard.
- [x] 1.9 Fix the duplicate key (`*`/`<`) and add 1.13 (`/`).
- [x] 1.5 Move initialization and the receiver to `onCreate`/`onDestroy`.
- [x] 1.7 Enter and Submit based on `imeOptions`.
- [x] 1.8 Emoji-safe Backspace.
- [x] 1.18 Replace `runBlocking` with `.value`.
- [x] 1.14 Move "Clear all" off the accidental swipe or add undo.

### Phase 2 — Basic fluidity (2–4 days): the biggest impact on feel
- [x] 2.1 Pressed state, haptics and optional sound.
- [x] 2.3 Threshold in dp, 45° sectors with `atan2`, snapping to the nearest configured direction or falling back to tap, hysteresis.
- [x] 2.4 Per-pointer/per-cell gesture state and `ACTION_CANCEL`.
- [x] 2.5 Update in place instead of calling `setInputView`.
- [x] 2.6 Backspace with repeat, one-shot shift and auto-capitalization.

### Phase 3 — Premium feel (3–5 days)
- [x] 2.2 Live swipe preview and bubble.
- [x] 2.6 Space bar as trackpad, double space → period, swipe to delete word.
- [x] 2.7 Continuous resizing, persistence and left/right one-handed mode.
- [x] 2.8 Micro-animations.
- [x] 3.1 Spanish accents and punctuation.
- [x] 3.2 Behavior based on field type.

### Phase 4 — Architecture and quality
- [x] 4.1 Canvas `KeyboardView`.
- [x] 4.4 Remove dead code and unify labels and mapping.
- [x] 4.5 Configuration DSL with validation and alphabet-coverage tests.
- [x] 3.5, 3.8 and 3.9: language, IME switching and accessibility.

### After the phases — Autocorrect
- [x] Word and accent correction when the word is finished, only for unknown words.
- [x] `⌫` right afterwards undoes the correction and learns the word (it isn't corrected again).
- [x] Suggestion bar (alternatives, ambiguous accents, original word).
- [x] Learning of repeated words; clear what was learned from settings.
- [x] Missed space: words run together (`nopuede` → `no puede`) or with the letter of the cell above the space bar instead of the space (`enemexico` → `en méxico`).
- [x] A tap on the space bar that slides without moving the cursor no longer loses the space.
- [x] The keyboard switch (🌐) and one-handed mode are removed from the layer key: they brought back Google's keyboard by accident and the side panel went unused.

### "Biblioteca de memes" (meme library) add-on
- [x] GIF and sticker panel (swipe ↓ on the layer key) with per-category and recents tabs.
- [x] The pack lives outside the app, in a folder the user picks (Storage Access Framework); no Internet or storage permission.
- [x] Sending via `commitContent` to apps that accept images; clipboard as a fallback.

### Updates, icon and settings
- [x] Self-update from the PC (`tools/serve_updates.py`), discovered via UDP broadcast, no fixed IP; debug only.
- [x] Silent install when the keyboard is hidden (Android 12+, after the first confirmed update).
- [x] Share the installed APK via Telegram/WhatsApp.
- [x] Automatic releases in GitHub Actions, signed with a fixed key; the app checks GitHub and the local network, and picks the newest build.
- [x] Custom icon: 4×4 grid with the thumb key highlighted (adaptive + monochrome).
- [x] Settings as a flat list: just label and control.
- [x] "Modo experto" (expert mode): no key shows labels or a preview bubble; backgrounds, borders, press feedback, version and suggestions remain.

---

## 6. Metrics to tell whether it improved

| Metric | Today (estimate) | Target |
|---------|----------------|----------|
| Visual or haptic response on touch | None until release | < 16 ms on `DOWN` |
| Lost gestures (swipe with no action) | Frequent on cells with 1–2 directions | ~0 (snap to nearest direction or tap) |
| Cost of changing shift or layer | Full rebuild (about 100 views) | Only update text or `invalidate()` |
| Wrong characters when typing fast | Possible due to global state | 0 (per-pointer state) |
| User text in logcat | Yes, in release too | Never |

Measure with: *Profile GPU Rendering* or Perfetto (frames > 16 ms on layer change), a debug log with the rate of no-action gestures, and timed typing tests.
