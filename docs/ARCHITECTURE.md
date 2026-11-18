# Architecture

One-handed Android keyboard: a 4×4 grid (3×3 letter block + control column) with 8-direction swipes, drawn in a single Canvas-based `KeyboardView`.

## Components

```
OneHandKeyboardService (InputMethodService)
 ├─ KeyboardView ............ Canvas: draws the grid, per-pointer hit testing,
 │                            animations, preview bubble, suggestion strip,
 │                            accessibility (ExploreByTouchHelper)
 ├─ GestureIntegration ...... per-pointer gesture logic (tap/swipe, backspace
 │                            repeat, space-bar trackpad)
 │    └─ core:gesture ....... GestureTracker + SwipeResolver (pure, tested)
 ├─ KeyboardLayoutManager ... state: layer, language, shift/caps/auto-capitalize, dead key
 │    └─ KeyboardConfigurationFactory ... layouts with the DSL `keyboardGrid { cell(i) { … } }`
 ├─ KeyboardActionProcessor . action → InputCommitter (InputConnection)
 └─ GestureHintProvider ..... labels and descriptions (ActionConfig.label / spokenName)
```

Other packages in `app/`:

- `spell/` – autocorrect: dictionaries, suggester, learned words, `AutoCorrector`.
- `memes/` + `ui/MemePanelView` – the meme library (folder scan, thumbnails, sending).
- `update/` – self-update: LAN discovery, GitHub/LAN download, installer, APK sharing (see [UPDATES.md](UPDATES.md)).

### Modules
- `app-ime` (folder `app/`) – the IME service, the view, the layouts and the settings (Compose).
- `core:gesture` – swipe resolution (`SwipeResolver`) and per-pointer tracking (`GestureTracker`).
- `core:domain` – shared models (`Direction`, `GestureType`, `KeyAction`, `KeyboardOutput`, `CaseMode`).
- `core:design` – themes (`ThemeManager`, 5 color tokens).

## Tech stack

- **Language**: Kotlin
- **UI**: custom Canvas `View` (keyboard) and Compose (settings)
- **DI**: Hilt (configuration and managers)
- **Persistence**: SharedPreferences (`KeyboardPreferences`, `ThemeManager`)
- **Concurrency**: Coroutines + Flow, single-thread executors for background work
- **Testing**: JUnit, Robolectric, MockK
- **Quality**: ktlint, detekt, CI on GitHub Actions

## Requirements

- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 35 (Android 15)
- **Kotlin**: 2.0.21+
- **Compose**: 2024.12.01 BOM

Initial design document (may be out of date compared with the code): [ORIGINAL_DESIGN.md](ORIGINAL_DESIGN.md).
