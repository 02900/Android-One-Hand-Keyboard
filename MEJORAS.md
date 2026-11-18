# Auditoría y plan de mejoras — OneHandKeyboard

> Auditoría del estado actual (commit `2e0801d`). Objetivo: corregir fallas y lograr que escribir se sienta **fluido, inmediato y tolerante**, no rígido.

---

## 0. Diagnóstico en una frase

El teclado **no le da ninguna respuesta al usuario mientras escribe**: no resalta la tecla al presionar, no vibra, no muestra qué carácter va a salir durante el swipe y no perdona un gesto impreciso. Además, **reconstruye toda la vista** en cada cambio de modo (mayúsculas, capa o altura). Esas dos cosas explican casi toda la sensación de rigidez.

---

## 1. Fallas (bugs) — prioridad P0

| # | Falla | Dónde | Efecto |
|---|-------|-------|--------|
| 1.1 | **El estado del gesto es global y no distingue punteros.** `gestureStartX/Y` y `gestureOriginCellIndex` se comparten entre todas las celdas, se usa `event.action` en vez de `actionMasked` y no se maneja `ACTION_CANCEL`. | `GestureIntegration.kt:306-309, 475` | Al escribir rápido con roll-over (un dedo baja antes de que suba el anterior), el segundo `DOWN` sobrescribe el origen y el `UP` del primer dedo calcula el delta contra otro punto: salen **caracteres equivocados o swipes fantasma**. |
| 1.2 | **El umbral del swipe está en píxeles fijos** (`swipeThreshold = 50f`). | `GestureIntegration.kt:310` | En pantallas xxxhdpi son unos 12 dp (demasiado sensible) y en hdpi unos 33 dp (demasiado duro). La sensación cambia según el teléfono. |
| 1.3 | **Un swipe en una dirección sin acción no hace nada.** | `GestureIntegration.kt:575-579` | En celdas con 1 o 2 direcciones (p. ej. la celda 0 solo tiene `SE`), un swipe un poco torcido o un "tap con temblor" de más de 50 px se pierde en silencio. Es la causa principal de que el teclado se sienta "duro". |
| 1.4 | **Se reconstruye todo el árbol de vistas** con `setInputView()` en cada cambio de mayúsculas, capa o altura. | `OneHandKeyboardService.kt:140-144` | Parpadeo, unos 100 `View` creados de nuevo, pérdida del toque en curso y jank. Escribir **una sola mayúscula** obliga a 2 reconstrucciones. |
| 1.5 | **El receiver de tema se registra en cada `onCreateInputView`**, y además se crea un `CoroutineScope` nuevo cada vez. | `OneHandKeyboardService.kt:109-135`, `GestureIntegration.kt:316` | `onCreateInputView` se llama otra vez al rotar o cambiar la configuración. Los receivers y scopes anteriores nunca se liberan (fuga), y ante un cambio de tema se reconstruye N veces. |
| 1.6 | **Registro de pulsaciones en logcat**: se loguean cada carácter, el texto pegado y el texto copiado. No hay reglas de ProGuard que eliminen los `Log`. | `KeyboardActionProcessor.kt:56, 254, 281`, `GestureIntegration.kt:588, 597` | **Problema de privacidad grave para un IME**: contraseñas y mensajes quedan en logcat, también en el build de release. |
| 1.7 | **Enter siempre hace `commitText("\n")`** y Submit siempre envía `IME_ACTION_SEND`. Se ignora `EditorInfo.imeOptions`. | `KeyboardActionProcessor.kt:179-200` | En campos de una sola línea (buscador, URL, login), Enter no hace nada, y "Submit" no dispara la acción que el campo espera (`SEARCH`, `GO`, `NEXT`, `DONE`). |
| 1.8 | **Backspace usa `deleteSurroundingText(1, 0)`.** | `KeyboardActionProcessor.kt:155` | Parte los emojis y los pares surrogate (queda un carácter roto) y no funciona en apps que esperan eventos de tecla (terminales, algunos WebViews). |
| 1.9 | **Clave de swipe duplicada**: en la celda 8 de símbolos, `SWIPE W` está definido dos veces (`*` y `<`). El `Map` se queda con el último. | `KeyboardConfigurationFactory.kt` (`createSymbolsGrid4x4`, celda 8) | **El `*` es inaccesible.** La validación de `CellConfiguration` no lo detecta porque el `Map` ya eliminó el duplicado. |
| 1.10 | **El layout inglés es inalcanzable e incompleto**: `switchLanguage()` nunca se llama y faltan las letras `u`, `i`, `o`, `p`. | `KeyboardLayoutManager.kt:221`, `createEnglishGrid4x4` | El subtipo `en_US` declarado en `method.xml` no sirve. |
| 1.11 | **La altura de celda no persiste**: `LayoutCalculator` vive dentro de `KeyboardUiFactory`, que se vuelve a crear en cada `onCreateInputView`. | `KeyboardUiFactory.kt:405` | El usuario ajusta la altura y la pierde al rotar o reiniciar el servicio. Además el ajuste es de **1 dp por swipe**, con una reconstrucción completa en cada paso. |
| 1.12 | **No hay tildes (á é í ó ú ü) ni ¿ ¡** en el layout español, que es el idioma por defecto (es-MX). | `createLettersGrid4x4` | No se puede escribir español correctamente. |
| 1.13 | **Falta `/`** en símbolos (además de `\`, `~` y `` ` ``). | `createSymbolsGrid4x4` | No se pueden escribir URLs, fechas ni rutas. |
| 1.14 | **"Borrar todo" está en un swipe NW de la tecla de mayúsculas, sin confirmación ni deshacer.** | Celda 7 de todos los layouts | Un roce accidental borra el mensaje entero. |
| 1.15 | `calculateCellBounds` busca `getChildAt(1)` en un `LinearLayout` que solo tiene un hijo (`GridLayout`). | `GestureIntegration.kt:369` | Siempre falla ("Cannot find mainRow"). Es código muerto que corre en cada `onStartInputView`. |
| 1.16 | `onComputeInsets` fuerza `contentTopInsets = 0`, `visibleTopInsets = 0` y `TOUCHABLE_INSETS_VISIBLE`. | `OneHandKeyboardService.kt:241-251` | Hay que verificarlo en un dispositivo: puede hacer que el área táctil del IME tape la app por encima del teclado o que la app calcule mal su espacio. Lo más probable es que convenga dejar el comportamiento por defecto. |
| 1.17 | `onEvaluateInputViewShown()` devuelve `true` siempre. | `OneHandKeyboardService.kt:253` | El teclado en pantalla aparece aunque haya un teclado físico conectado. |
| 1.18 | `runBlocking { currentTheme.first() }` se llama varias veces **por celda** en el hilo principal. | `ButtonStyleProvider.kt:479`, `UnifiedGridKeyboardBuilder.kt:49` | Es un `StateFlow`: basta con `.value`. `runBlocking` en el hilo de UI es innecesario y arriesgado. |
| 1.19 | Los bordes de celda usan `setStroke(1px)` en los 4 lados de cada celda. | `ButtonStyleProvider.kt:526-544` | Las líneas interiores quedan dobles (2 px) y las exteriores simples. El grosor tampoco escala con la densidad. |
| 1.20 | Inconsistencias menores: el tema por defecto se llama `"Default Light"` pero carga `MonokaiDark`; en símbolos la tecla de mayúsculas no hace nada útil; `ActionConfig.Text` pasa a minúsculas los textos con mayúsculas mezcladas. | `ThemeManager.kt:30, 37`, etc. | Confunde al usuario y al mantenedor. |

---

## 2. Fluidez: qué hace que se sienta rígido y cómo arreglarlo

### 2.1 Respuesta inmediata al tocar (lo más importante)
Hoy no pasa nada visible hasta `ACTION_UP` (el propio código dice *"Sin feedback visual al presionar para apariencia plana"*). El cerebro necesita una confirmación en menos de 20 ms.

- **Estado presionado en `ACTION_DOWN`**: cambiar el fondo de la celda a un tono `pressed` (agregar el token `backgroundPressed` a `ColorTokens`, o calcularlo con el `adjustBrightness` que ya existe y no se usa). Volver al tono normal en el `UP` o el `CANCEL` con un fade corto (~80 ms).
- **Háptica**: el permiso `VIBRATE` ya está declarado pero no se usa.
  - `DOWN`: `view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)`.
  - Al cruzar el umbral del swipe o cambiar de dirección: `HapticFeedbackConstants.CLOCK_TICK` (o `SEGMENT_FREQUENT_TICK` en API 34+).
  - Que sea configurable (on/off e intensidad).
- **Sonido opcional**: `AudioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD / FX_KEYPRESS_DELETE / FX_KEYPRESS_SPACEBAR)`, respetando la configuración del sistema.

### 2.2 Vista previa del swipe en vivo
El usuario hoy **adivina** qué carácter va a salir. En `ACTION_MOVE`:
1. Cuando el desplazamiento supera el umbral, calcular la dirección y **resaltar la etiqueta destino** (escala ~1.4x, color de acento) y atenuar las demás.
2. Mostrar un **globo de vista previa** sobre la celda con el carácter que se va a escribir (un `PopupWindow` o un `TextView` superpuesto en el root).
3. Si el dedo vuelve al centro, la vista previa vuelve al carácter del tap: el usuario puede **corregir antes de soltar**.
4. Si la dirección no tiene acción, mostrar el destino "fantasma" al que se va a ajustar (ver 2.3).

### 2.3 Gestos tolerantes
- **Umbral en dp y relativo a la celda**: `threshold = max(ViewConfiguration.scaledTouchSlop * 1.5, 0.22 × min(anchoCelda, altoCelda))`. Con celdas de 60 dp eso da unos 13 a 18 dp.
- **Ajuste a la dirección configurada más cercana**: si la dirección calculada no tiene acción, buscar entre las direcciones configuradas de esa celda la más cercana en ángulo, hasta ±67°. Si no hay ninguna, tratarlo como **tap**. Con esto se elimina la falla 1.3.
- **Sectores angulares uniformes de 45°** con `atan2` en vez de la heurística `absX > absY * 2`. Hoy los cardinales cubren unos 53° y las diagonales unos 37°, por eso las diagonales son difíciles.
- **Histéresis** de ~8° para que la dirección no parpadee en los bordes de sector (`GestureConfig.directionHysteresis` ya existe pero no se usa).
- Usar `event.eventTime` en vez de `System.currentTimeMillis()`.

### 2.4 Multitouch y roll-over correctos
Llevar el estado **por `pointerId`** (o, como mínimo, por celda) y procesar `ACTION_POINTER_DOWN/UP` y `ACTION_CANCEL`. Así se puede escribir rápido con el pulgar sin esperar a levantar el dedo anterior. Boceto:

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
            val newDir = resolveDirection(t, e.getX(p) - t.x0, e.getY(p) - t.y0) // con umbral, snap e histéresis
            if (newDir != t.dir) { t.dir = newDir; showPreview(t); view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> tracks[id]?.let { commit(it); tracks.remove(id); view.isPressed = false; hidePreview() }
        MotionEvent.ACTION_CANCEL -> { tracks.clear(); view.isPressed = false; hidePreview() }
    }
    return true
}
```

> Nota: con un `OnTouchListener` por celda, Android ya divide los eventos entre vistas (`splitMotionEvents`), así que llevar el estado por celda alcanza. Con una vista única (ver 4.1) hace falta el seguimiento por `pointerId` del boceto.

### 2.5 Cero reconstrucciones: actualizar en el lugar
Construir la vista **una vez**. Guardar referencias a los `TextView` de cada celda y, al cambiar de estado:
- **Mayúsculas**: solo cambiar `text` de las etiquetas, sin crear vistas nuevas.
- **Letras ↔ Símbolos**: rellenar las mismas celdas con la nueva configuración. Si cambian los `colSpan`, tener los dos grids precreados y alternar con `visibility` más un crossfade de 100 ms (`animate().alpha()`).
- **Altura**: cambiar `layoutParams.height` de las celdas y llamar a `requestLayout()`, sin `setInputView`.
- **Tema**: volver a aplicar colores a las vistas existentes.

### 2.6 Teclas que se sienten "vivas"
- **Backspace con repetición**: mantener pulsado → espera de 350 ms y luego repetición acelerada (de 80 ms a 30 ms). **Swipe a la izquierda** sobre Backspace borra la palabra anterior; swipe a la derecha deshace el último borrado.
- **Barra espaciadora como trackpad**: arrastrar en horizontal mueve el cursor (1 carácter cada ~12 dp, con `sendDownUpKeyEvents(KEYCODE_DPAD_LEFT/RIGHT)` o `setSelection`). Es lo que más ayuda a corregir con una sola mano.
- **Doble espacio → ". "** con mayúscula automática después.
- **Shift inteligente**:
  - 1 tap = mayúscula **solo para la próxima letra** (activar de verdad el `CAPITALIZE` / `isCapitalizeNext`, que hoy existen pero nunca se activan).
  - Doble tap = bloqueo de mayúsculas.
  - Mayúscula automática al empezar un campo y después de `. ? !` usando `currentInputConnection.getCursorCapsMode(editorInfo.inputType)`.
  - Mostrar el estado en la tecla (flecha vacía / llena / subrayada).

### 2.7 Ajuste de altura continuo
Reemplazar "1 dp por swipe" por un **modo de redimensionado**: un long-press en la tecla de capa activa un handle y se arrastra en vertical con la altura en vivo. Persistir el valor en `SharedPreferences`/DataStore. Agregar también una opción de **modo una mano**: ancho al 75–85 % alineado a la izquierda o derecha, con un botón para cambiar de lado. Es irónico que un "One Hand Keyboard" ocupe todo el ancho en un teléfono grande.

### 2.8 Micro-animaciones (sutiles, < 120 ms)
- Escala de 0.96 en `DOWN` y vuelta a 1.0 en `UP` (`animate().scaleX/Y().setDuration(60)`).
- Etiqueta destino del swipe: escala y color con interpolación.
- Cambio de capa: crossfade del contenido, no un salto brusco.
- Usar `ViewPropertyAnimator` o `ValueAnimator`, nunca recrear vistas.

---

## 3. Funcionalidad que falta para uso real

1. **Tildes y signos del español**: agregarlos como swipes sobre las vocales (p. ej. `a` + swipe N = `á`), o con una **tecla muerta** `´` que acentúe la siguiente vocal. Agregar `¿`, `¡` y `ü`.
2. **Respetar `EditorInfo`** en `onStartInputView`:
   - `TYPE_CLASS_NUMBER` / `PHONE` → abrir directamente en símbolos.
   - `TYPE_TEXT_VARIATION_EMAIL_ADDRESS` / `URI` → poner `@` `.` `/` a mano.
   - Contraseñas (`TYPE_TEXT_VARIATION_PASSWORD`, `VISIBLE_PASSWORD`, `WEB_PASSWORD`) → sin vista previa en globo y **sin ningún log**.
   - Etiqueta e ícono de Enter según `imeOptions` (↵, 🔍, →, ✓) y `IME_FLAG_NO_ENTER_ACTION`.
   - Reiniciar mayúsculas y capa al cambiar de campo.
3. **Enter correcto**:
   ```kotlin
   val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
   val noEnterAction = info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
   if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action)
   else ic.sendKeyEvent(...KEYCODE_ENTER down/up...)
   ```
4. **Backspace robusto**: si no hay selección, usar `sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)`, o calcular la longitud del code point anterior con `Character.charCount(Character.codePointBefore(...))` para no partir emojis.
5. **Cambio de idioma** accesible (swipe en la barra espaciadora, o seguir el subtipo del sistema con `onCurrentInputMethodSubtypeChanged`) y completar el layout inglés.
6. **Borrar todo / copiar todo** con confirmación o deshacer, y usar `ExtractedText` (`getExtractedText`) en vez del límite de 10 000 caracteres.
7. **Persistencia** del idioma, la altura, el modo una mano, la háptica y el sonido.
8. **Tecla para cambiar de IME** (`switchToNextInputMethod`): `supportsSwitchingToNextInputMethod="true"` ya está declarado pero no hay ninguna tecla que lo use.
9. **Accesibilidad** (el README la promete): `contentDescription` por celda, `AccessibilityDelegate` que anuncie el carácter de destino y soporte para explorar con TalkBack.

---

## 4. Rendimiento y arquitectura

### 4.1 Una sola vista dibujada en Canvas (fase 2, la mejora de rendimiento más grande)
Hoy hay unas 16 `FrameLayout` con hasta 9 `TextView` cada una: más de 100 vistas, medidas y layout en cada reconstrucción. Un `KeyboardView : View` propio que:
- dibuje celdas y etiquetas en `onDraw` con `Paint` y `StaticLayout` precalculados,
- maneje todos los toques en un solo `onTouchEvent` con `pointerId`,
- invalide solo el rect de la celda que cambia (`invalidate(rect)`),

da latencia y consumo mínimos, y animaciones más fáciles (todo es un valor interpolado en el draw). Es el enfoque de los IME de producción.

### 4.2 Logging
- Envolver todos los logs en `if (BuildConfig.DEBUG)` o usar Timber con un `Tree` solo en debug.
- Agregar a `proguard-rules.pro`:
  ```
  -assumenosideeffects class android.util.Log { public static int v(...); public static int d(...); public static int i(...); }
  ```
- **Nunca** loguear el texto del usuario, ni siquiera en debug.
- Quitar los `println` de `DefaultGestureDetector` y los logs en cada `ACTION_MOVE`.

### 4.3 Ciclo de vida
- Crear los componentes **una vez** en `onCreate()` y en `onCreateInputView()` solo crear o reusar la vista.
- Registrar el receiver de tema en `onCreate()` y desregistrarlo en `onDestroy()`. Mejor aún, observar `themeManager.currentTheme` (es un `StateFlow`) con un scope del servicio y eliminar el broadcast.
- Cancelar `gestureScope` en `cleanup()` (hoy solo se cancela el job).

### 4.4 Código muerto o duplicado que conviene borrar
- `DefaultGestureDetector` / `core:gesture`: se inyecta y se recolecta, pero nada lo consume (`TODO`). Hay que integrarlo de verdad (ya tiene `CANCEL`, velocidad e histéresis) o eliminarlo.
- `GestureIntegration.createDomainKeyboardLayout()` (layout falso de 9 celdas con `'a'`), `calculateCellBounds`, `processGestureDirectly` (wrapper sin valor).
- `actions/ActionProcessor.kt` (no se usa y duplica la lógica de `GestureIntegration.handleGestureAction`).
- `KeyboardLayoutManager.getCurrentKeyData()` y sus 3 data providers (legacy 3x3).
- `ui/SimpleKeyboardGrid.kt` (Compose, sin uso), `createLegacyButton`, `getLegacyGestureHints`, `createTitleView`, `calculateSquareCellSize`.
- Módulos vacíos: `core:text`, `core:data`, `core:analytics`, `feature:settings` (o empezar a usarlos).
- Los tres `when` idénticos de etiquetas en `GestureHintProvider` (`getConfigurationHints`, `getDirectionalLabel`, `getCenterLabel`): centralizarlos en una sola función `ActionConfig.label()`.
- El mapeo `ActionConfig → KeyboardOutput` en `GestureIntegration`: moverlo a una función pura y testeable.

### 4.5 Configuración más segura
- Construir las celdas con un DSL o builder que **lance error ante direcciones duplicadas** (falla 1.9), por ejemplo `cell(8) { tap('7'); swipe(W, '*'); swipe(W, '<') /* → error */ }`.
- Test unitario que verifique que cada layout contiene el alfabeto completo y los símbolos imprescindibles (habría detectado las fallas 1.10, 1.12 y 1.13).
- Cachear las configuraciones: hoy `getConfiguration()` crea objetos nuevos en cada cambio de capa.

### 4.6 Tests y CI
- Los tests actuales del servicio solo comprueban que la clase existe. Agregar:
  - tests de `resolveDirection` (umbral, sectores, ajuste a la dirección más cercana, histéresis),
  - tests de mapeo de acciones,
  - tests de Robolectric de `onStartInputView` con distintos `EditorInfo`,
  - un test de roll-over con dos punteros.
- Pasar el CI a JDK 17 (AGP 8 lo requiere).

---

## 5. Plan de aplicación por fases

### Fase 1 — Correcciones críticas (1–2 días)
- [x] 1.6 Eliminar los logs de texto del usuario y agregar `-assumenosideeffects` a ProGuard.
- [x] 1.9 Corregir la clave duplicada (`*`/`<`) y agregar 1.13 (`/`).
- [x] 1.5 Mover la inicialización y el receiver a `onCreate`/`onDestroy`.
- [x] 1.7 Enter y Submit según `imeOptions`.
- [x] 1.8 Backspace seguro para emojis.
- [x] 1.18 Reemplazar `runBlocking` por `.value`.
- [x] 1.14 Sacar "Borrar todo" del swipe accidental o agregar deshacer.

### Fase 2 — Fluidez básica (2–4 días): el mayor impacto en la sensación
- [x] 2.1 Estado presionado, háptica y sonido opcional.
- [x] 2.3 Umbral en dp, sectores de 45° con `atan2`, ajuste a la dirección configurada más cercana o fallback a tap, histéresis.
- [x] 2.4 Estado de gesto por puntero/celda y `ACTION_CANCEL`.
- [x] 2.5 Actualizar en el lugar en vez de llamar a `setInputView`.
- [x] 2.6 Backspace con repetición, shift de una sola vez y mayúscula automática.

### Fase 3 — Sensación premium (3–5 días)
- [x] 2.2 Vista previa del swipe en vivo y globo.
- [x] 2.6 Barra espaciadora como trackpad, doble espacio → punto, swipe para borrar palabra.
- [x] 2.7 Redimensionado continuo, persistencia y modo una mano izquierda/derecha.
- [x] 2.8 Micro-animaciones.
- [x] 3.1 Tildes y signos del español.
- [x] 3.2 Comportamiento según el tipo de campo.

### Fase 4 — Arquitectura y calidad
- [ ] 4.1 `KeyboardView` en Canvas.
- [ ] 4.4 Eliminar el código muerto y unificar etiquetas y mapeo.
- [ ] 4.5 DSL de configuración con validación y tests de cobertura de alfabeto.
- [ ] 3.5, 3.8 y 3.9: idioma, cambio de IME y accesibilidad.

---

## 6. Métricas para saber si mejoró

| Métrica | Hoy (estimado) | Objetivo |
|---------|----------------|----------|
| Respuesta visual o háptica al tocar | Ninguna hasta soltar | < 16 ms en `DOWN` |
| Gestos perdidos (swipe sin acción) | Frecuentes en celdas con 1–2 direcciones | ~0 (ajuste a la dirección más cercana o tap) |
| Costo de cambiar mayúsculas o capa | Reconstrucción completa (unas 100 vistas) | Solo actualizar texto o `invalidate()` |
| Caracteres erróneos al escribir rápido | Posibles por estado global | 0 (estado por puntero) |
| Texto del usuario en logcat | Sí, también en release | Nunca |

Medir con: *Profile GPU Rendering* o Perfetto (frames > 16 ms al cambiar de capa), un log de debug con la tasa de gestos sin acción y pruebas de escritura cronometradas.
