# Original design

> Initial design document; parts may be out of date compared with the code (see [ARCHITECTURE.md](ARCHITECTURE.md)).

**Product**: 3×3 swipe-based Android keyboard (inspired by MessagEase)\
**Platform**: Android IME (Input Method Editor)\
**Language**: Kotlin (minSdk 26, targetSdk latest)\
**UI**: Jetpack Compose inside `InputMethodService`\
**Paradigm**: Clean Architecture + UDF (MVI) + feature-based modularization\
**Principles**: Offline-first, privacy by design, touch performance, accessibility

---

## 1) Executive summary

We will build a lightweight, private (no network by default) and fast IME, with **nine large keys** and **8-direction gestures** per cell to emit characters, symbols and commands. The UX prioritizes touch precision, immediate feedback (optional haptic/sound), and per-language adaptability.

**Key goals**

- Input latency → **≤ 20 ms** from `ACTION_UP` to `commitText`.
- Gesture precision → high recall with **adaptive thresholds** based on pixel density.
- Accessibility: high contrast, scalable sizes, TalkBack support and switch navigation.
- Security: no mandatory telemetry; optional, exportable on-device learning.

**Non-goals (MVP)**

- Complex ML prediction models in v1 (a hook is left in place).
- No built-in clipboard manager.
- No cloud/remote backup (other than system backup).

---

## 2) Requirements

### 2.1 Functional

- System-selectable IME; support for `EditorInfo.inputType` (text, email, URI, password, numeric).
- Configurable **3×3 layout**: map (cell, direction) → symbol/action.
- Gestures: tap, long-press, 8-direction swipe, repeat on hold for delete/space.
- Auxiliary bar: delete, space, Enter/Done/Next (derived from `imeOptions`).
- Themes: light/dark + high contrast.
- Settings: gesture sensitivity, haptic intensities, sounds, language/mapping.

### 2.2 Non-functional

- Performance: sustained 60 fps while dragging; **minimal recomposition** in Compose.
- Memory: foreground footprint **< 40 MB** on mid-range devices.
- Battery: no background work; throttling of haptics/sound.
- Privacy: **no network permissions** by default.
- Reliability: ANR-free; unit test coverage **> 80%** in the gesture and mapping core.

---

## 3) Overall architecture (Clean + UDF)

```
app-ime (Android app, InputMethodService)
│
├─ feature:keyboard         (MVI presentation + Compose)
├─ feature:settings         (Settings screens and logic)
├─ core:gesture             (Gesture engine, thresholds, normalization)
├─ core:mapping             (Per-language mapping strategies, symbols, rules)
├─ core:text                (InputConnection adapters, commands)
├─ core:design              (Design System: tokens, base components)
├─ core:analytics           (Optional; off by default)
├─ core:data                (DataStore/Room, repositories)
├─ core:domain              (Use cases, domain models)
└─ core:testkit             (Fakes, generators, golden tests)
```

**Presentation layer (MVI/UDF)**: Intent → Reducer → State → Render (Compose).\
**Domain layer**: Pure use cases (e.g. `ResolveSwipe`, `CommitAction`).\
**Data layer**: Repositories (settings, mapping tables, local dictionaries).

**DI**: Hilt; **concurrency**: Coroutines + Flow; **IME routing**: events to `InputConnection` via commands.

---

## 4) Design patterns

- **Strategy**: per-language/theme mapping (`MappingStrategy` per locale; swappable).
- **State**: IME states (idle, touching, dragging, composing, selecting).
- **Command**: actions sent to the editor (`CommitText`, `Backspace`, `SetComposing`, `FinishComposing`, `MoveCursor`).
- **Adapter**: `InputConnectionAdapter` to encapsulate host-app quirks.
- **Composite**: layout model (KeyCluster 3×3 → Key → Variants).
- **Decorator**: gesture enrichment (filters: snap to 8 directions, braking, acceleration).
- **Observer (Flow)**: UDF and reactive settings.
- **Builder**: declarative generation of layouts/themes from tokens.
- **Flyweight** (optional): cache of glyphs/SVG paths for key icons.

---

## 5) Gesture engine (core\:gesture)

### 5.1 Pipeline

1. **Pointer Down** → capture the origin cell.
2. **Move** → accumulate samples; noise filter (Kalman/simple EMA); compute the average vector.
3. **Direction Resolver** → split the circle into 8 sectors (N, NE, E, SE, S, SW, W, NW) with angular hysteresis.
4. **Thresholds**: minimum distance depending on DPI and cell size; dynamic threshold based on speed (flick vs. drag).
5. **Up** → emit `Gesture(endCell, direction, duration, speed)`.

### 5.2 Thresholds and tolerances

- Dead angle around 0°±θ to distinguish **tap** vs. **short swipe**.
- Hysteresis to avoid bouncing between sectors when crossing their edges.
- Curvature: if the trajectory changes by > φ, prioritize the **last stable segment**.

### 5.3 Errors and recovery

- If the destination cell ≠ origin and the direction ≈ straight, prioritize the **direction**.
- Safe cancellation: leaving the keyboard area → discard the event and restore the UI.

---

## 6) 3×3 layout system (core\:mapping)

### 6.1 Model

```kotlin
data class Key(val id: KeyId, val label: String?, val actions: Map<Direction, Action>)
data class KeyCluster(val cells: List<Key>) // 9 cells
```

**Action** can be a symbol, a sequence, or a command (e.g. `Backspace`).

### 6.2 Mapping strategies (Strategy)

- `EsLatamMapping`, `EnUsMapping`, `SymbolicMapping`, `NumericMapping`.
- Loaded from JSON/YAML resources or Room for the user's custom profiles.
- Context-based resolution (`EditorInfo`): email/URI → map with `@`, `.`, `/`, `_` on short directions.

### 6.3 Special rules

- **Shift/Case**: double tap on the center cell enables **Caps Lock**; swipe up on the center → one-off uppercase.
- **Delete**: holding the “delete” cell → auto-repeat with acceleration.
- **Space**: swipe right on the bottom bar.

---

## 7) Text and command engine (core\:text)

### 7.1 Command queue (Command)

- Serializes `InputConnection` calls, avoiding races with recomposition.
- Groups operations ( `beginBatchEdit` → commands → `endBatchEdit`).

### 7.2 Supported actions

- `CommitText(seq)`, `DeleteSurroundingText(left,right)`, `Backspace()`, `SetComposingText(seq)`, `FinishComposing()`, `SendAction(IME_ACTION_*)`, `MoveCursor(dir)`.

### 7.3 Policies

- Password fields: disable suggestions/composition.
- Number/phone: numeric layout/quick symbols.
- Emails/URIs: prioritize `@`, `.`, `/`, `_`.

---

## 8) Presentation (feature\:keyboard) – UDF with Compose

### 8.1 MVI loop

```
UserIntent → Reducer → UIState → Compose(Render)
                     ↑            ↓
                 SideEffects ←——— Effects (haptics, sound)
```

- Immutable `UIState`: cells, focus, current gesture, theme, `imeAction`.
- Pure `Reducer`; side effects in the effects layer (haptics/sound/optional telemetry).

### 8.2 UI components (Design System)

- **KeyCell**: adaptive size, configurable hitSlop, states (default/pressed/dragging/disabled).
- **GestureTrail**: optional, discreet lines/particles with an overdraw budget.
- **Bars**: space, delete, action (Enter/Next/Done).
- **HUD**: character tooltip while dragging.

---

## 9) Design System (core\:design)

### 9.1 Tokens

- **Color**: `primary`, `onPrimary`, `surface`, `onSurface`, `warning`, `success`, `highContrast`.
- **Typography**: scales for key labels, HUD, settings titles.
- **Spacing**: `xs, sm, md, lg` (dp).
- **Sizes**: base cell size (dp) with accessibility scaling.
- **Radius**: rounded keys 8–16dp; containers 12–20dp.
- **Elevation**: 0–3 levels; soft shadows.
- **Motion**: durations 40–120ms; `fastOutSlowIn` vs `linearOutSlowIn` curves.
- **Haptics**: light (tap), medium (commit), continuous (delete auto-repeat).
- **Sounds**: click, delete, enter; volume relative to the system.

### 9.2 Components and states

- States: `default`, `pressed`, `dragging`, `disabled`, `focused`.
- Minimum **WCAG AA** contrast for labels.
- Configurable `HitSlop` for corner keys; extra touch padding.

### 9.3 Theming

- Light/Dark theme + High Contrast.
- Support for token-based **packs** (skins) (swappable without recompiling).

---

## 10) Data and configuration (core\:data)

- **DataStore Preferences**: flags, theme, sensitivity, language, active pack.
- **Room (optional)**: local dictionaries, shortcuts, custom mapping profiles.
- **Migrations**: versioned schema; import/export of profiles as JSON.

---

## 11) Accessibility

- Key size adapts to the system `fontScale`.
- High contrast and strong outlines in `pressed/dragging` states.
- One-handed mode (left/right alignment) and thumb mode.

---

## 12) Security and privacy

- **No `INTERNET` permission** by default.
- No reading of contacts, SMS, etc.
- **Opt-in** telemetry, removable as a module (core\:analytics).
- Encryption of sensitive local stores (if learning data is saved).
- Clear Data Safety declaration on Play.

---

## 13) Performance

- **Baseline Profiles** and R8.
- Reuse `Path`/`Painter` in keys; avoid recomposition on pointer moves (derive minimal state).
- Compute the direction only on discrete frames (e.g. every 8–12 ms) to stay within the CPU budget.
- Use `begin/endBatchEdit` to group commits.

---

## 14) Testing and QA

- **Unit**: `ResolveSwipe`, thresholds, hysteresis, per-language mapping.
- **Property-based**: trajectory fuzzing for the gesture engine.
- **Snapshot UI**: key/theme states (Paparazzi/Compose Preview).
- **Instrumentation**: UiAutomator in host apps (Notes, Gmail, WhatsApp) for basic flows.
- **Macrobenchmark**: IME startup, commit latency, jank while dragging.
- **Golden tests**: trajectories → expected direction (fixtures).

---

## 15) Observability (optional, opt‑in)

- Local crash reporting (minidumps) or Crashlytics (removable module).
- Metrics: commit latency, direction error rate, ANR rate.
- Lightweight traces with `signpost` for critical loops.

---

## 16) Internationalization

- Initial supported locales: `es-MX`, `en-US`.
- Strategy per locale and per `inputType`/`imeOptions`.
- Pluralization/strings in `resources`.

---

## 17) System integration

- `InputMethodService` + `onCreateInputView()` → ComposeView.
- Honor `EditorInfo.imeOptions` (Done/Go/Send/Next/Search).
- Landscape/portrait mode (responsive layout).
- Direct Boot: start without the settings UI.

---

## 18) DevEx, CI/CD and quality

- **Style**: ktlint + detekt + spotless.
- **CI**: GitHub Actions → build, tests, lint, unit + instrumentation on emulators.
- **Versioning**: app SemVer + version code.
- **Signing**: Play App Signing; tracks: internal → closed → open → production.
- **Feature flags**: `core:data` (DataStore) with documented keys.

---

---

---

## 21) Suggested package structure

```
app-ime/
  └─ src/... (InputMethodService, bootstrap, permissions)
feature/keyboard/
  ├─ ui/compose (KeyCell, Bars, HUD)
  ├─ mvi (Intents, Reducer, UIState)
  └─ effects (Haptics, Sounds)
feature/settings/
  ├─ ui (screens, theming)
  └─ domain (UseCases: Get/SetSettings)
core/gesture/
  ├─ engine (resolver, thresholds)
  └─ model (Direction, Gesture)
core/mapping/
  ├─ strategy (EsLatamMapping, EnUsMapping, ...)
  ├─ model (Key, KeyCluster, Action)
  └─ parser (JSON/YAML → models)
core/text/
  ├─ ic (InputConnectionAdapter, CommandQueue)
  └─ commands (CommitText, Backspace, ...)
core/design/
  ├─ tokens (color, spacing, motion, haptics)
  └─ components (visual primitives)
core/data/
  ├─ datastore (preferences)
  └─ room (dictionaries, profiles)
core/domain/
  └─ usecases (ResolveSwipe, CommitAction, ApplyMapping)
core/testkit/
  └─ fixtures, golden tests, fakes
```

---

## 22) Key contracts (interfaces)

### 22.1 Gestures

```kotlin
enum class Direction { N, NE, E, SE, S, SW, W, NW, CENTER }

data class Gesture(
  val startCell: Int,
  val endCell: Int,
  val direction: Direction,
  val durationMs: Long,
  val speed: Float
)
```

### 22.2 Mapping

```kotlin
interface MappingStrategy {
  fun actionFor(cell: Int, direction: Direction, context: EditorContext): Action
}
```

### 22.3 Commands

```kotlin
sealed interface EditorCommand
object Backspace: EditorCommand
class CommitText(val text: CharSequence): EditorCommand
object FinishComposing: EditorCommand
class MoveCursor(val dx: Int): EditorCommand
```

---

## 23) Specific UX guidelines

- **Direction reading**: show a preview of the character in the HUD while dragging.
- **Errors**: short vibration if the direction falls in the dead zone; emit no character.
- **Discoverability**: training overlay that highlights arrows/directions per cell.
- **Consistency**: global actions (delete/space) always in the same place.

---

## 24) Ethical telemetry (if the user enables it)

- Anonymous, aggregated, on-device events by default; explicit opt‑in.
- Quality metrics: latency, direction errors, theme usage.
- "Send diagnostics" button with data redaction.\\


### Appendix A – Default metrics

- `θ_dead_zone = 10°`, `θ_sector = 45°`, angular hysteresis `±6°`.
- Minimum swipe distance = `max(12dp, 0.18 * cellSize)`.
- Delete repeat: initial 400 ms → 80 ms (with acceleration).

### Appendix B – Naming conventions

- Modules `core:*`, `feature:*`, `app-ime`.
- Packages: `com.company.keyboard.core.gesture`, etc.

### Appendix C – Suggested dependencies

- Compose BOM, Accompanist (if needed), Hilt, DataStore, Room (optional), Crashlytics (optional module), Paparazzi/Shot, Macrobenchmark.

