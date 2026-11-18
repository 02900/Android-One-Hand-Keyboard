# Arquitectura

Teclado Android de una mano: una cuadrícula 4×4 (bloque de letras 3×3 + columna de control) con swipes en 8 direcciones, dibujado en un único `KeyboardView` sobre Canvas.

## Componentes

```
OneHandKeyboardService (InputMethodService)
 ├─ KeyboardView ............ Canvas: dibuja la cuadrícula, hit-testing por puntero,
 │                            animaciones, globo de vista previa, barra de
 │                            sugerencias, accesibilidad (ExploreByTouchHelper)
 ├─ GestureIntegration ...... lógica de gestos por puntero (tap/swipe, repetición de
 │                            borrar, trackpad en espacio, resize con long-press)
 │    └─ core:gesture ....... GestureTracker + SwipeResolver (puros, testeados)
 ├─ KeyboardLayoutManager ... estado: capa, idioma, shift/caps/auto-mayúscula, tecla muerta
 │    └─ KeyboardConfigurationFactory ... layouts con DSL `keyboardGrid { cell(i) { … } }`
 ├─ KeyboardActionProcessor . acción → InputCommitter (InputConnection)
 └─ GestureHintProvider ..... etiquetas y descripciones (ActionConfig.label / spokenName)
```

### Módulos
- `app-ime` (carpeta `app/`) – el servicio IME, la vista, los layouts y los ajustes (Compose).
- `core:gesture` – resolución de swipes (`SwipeResolver`) y seguimiento por puntero (`GestureTracker`).
- `core:domain` – modelos compartidos (`Direction`, `GestureType`, `KeyAction`, `KeyboardOutput`, `CaseMode`).
- `core:design` – temas (`ThemeManager`, 5 tokens de color).

## Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: `View` propia dibujada en Canvas (teclado) y Compose (ajustes)
- **DI**: Hilt (configuración y gestores)
- **Persistencia**: SharedPreferences (`KeyboardPreferences`, `ThemeManager`)
- **Concurrencia**: Coroutines + Flow
- **Testing**: JUnit, Robolectric, MockK
- **Calidad**: ktlint, detekt, CI/CD

## Requisitos

- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 35 (Android 15)
- **Kotlin**: 2.0.21+
- **Compose**: 2024.12.01 BOM

Documento de diseño inicial (puede estar desactualizado frente al código): [DISENO_ORIGINAL.md](DISENO_ORIGINAL.md).
