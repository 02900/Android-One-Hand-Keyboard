# Arquitectura del proyecto

**Producto**: Teclado Android tipo 3×3 con deslizamientos (inspirado en MessagEase)\
**Plataforma**: Android IME (Input Method Editor)\
**Lenguaje**: Kotlin (minSdk 26, targetSdk latest)\
**UI**: Jetpack Compose dentro de `InputMethodService`\
**Paradigma**: Clean Architecture + UDF (MVI) + modularización por features\
**Principios**: Offline-first, privacidad por diseño, rendimiento táctil, accesibilidad

---

## 1) Resumen ejecutivo

Construiremos un IME liviano, privado (sin red por defecto) y rápido, con **nueve teclas grandes** y **gestos en 8 direcciones** por celda para emitir caracteres, símbolos y comandos. La UX prioriza precisión táctil, feedback inmediato (háptico/sonoro opcional), y adaptabilidad por idioma.

**Objetivos clave**

- Latencia de entrada → **≤ 20 ms** desde `ACTION_UP` a `commitText`.
- Precisión de gestos → alto recall con **umbrales adaptativos** por densidad de píxeles.
- Accesibilidad: alto contraste, tamaños escalables, soporte TalkBack y navegación por switch.
- Seguridad: sin telemetría obligatoria; aprendizaje local opcional y exportable.

**No objetivos (MVP)**

- Modelos ML de predicción complejos en la v1 (se deja hook).
- Sin clipboard manager propio.
- Sin nube/backup remoto (salvo backup del sistema).

---

## 2) Requisitos

### 2.1 Funcionales

- IME seleccionable del sistema; soporte `EditorInfo.inputType` (texto, email, URI, password, numérico).
- **Layout 3×3** configurable: mapa (celda, dirección) → símbolo/acción.
- Gestos: tap, long-press, swipe en 8 direcciones, repetición en hold para borrar/espacio.
- Barra auxiliar: borrar, espacio, Enter/Done/Next (derivado de `imeOptions`).
- Temas: claro/oscuro + alto contraste.
- Ajustes: sensibilidad de gestos, intensidades hápticas, sonidos, idioma/mapping.

### 2.2 No funcionales

- Rendimiento: 60 fps sostenidos al arrastrar; **recomposición mínima** en Compose.
- Memoria: huella en foreground **< 40 MB** en dispositivos medios.
- Batería: sin trabajos en background; throttling de hápticos/sonido.
- Privacidad: **sin permisos de red** por defecto.
- Confiabilidad: ANR-free; cobertura de pruebas unitaria **> 80%** en core de gestos y mapping.

---

## 3) Arquitectura general (Clean + UDF)

```
app-ime (Android app, InputMethodService)
│
├─ feature:keyboard         (Presentación MVI + Compose)
├─ feature:settings         (Pantallas y lógica de ajustes)
├─ core:gesture             (Motor de gestos, thresholds, normalización)
├─ core:mapping             (Estrategias de mapeo por idioma, símbolos, reglas)
├─ core:text                (Adaptadores InputConnection, comandos)
├─ core:design              (Design System: tokens, componentes base)
├─ core:analytics           (Opcional; apagado por defecto)
├─ core:data                (DataStore/Room, repositorios)
├─ core:domain              (Casos de uso, modelos de dominio)
└─ core:testkit             (Fakes, generadores, golden tests)
```

**Capa Presentación (MVI/UDF)**: Intent → Reducer → Estado → Render (Compose).\
**Capa Dominio**: Casos de uso puros (p. ej., `ResolveSwipe`, `CommitAction`).\
**Capa Datos**: Repositorios (ajustes, tablas de mapeo, diccionarios locales).

**DI**: Hilt; **concurrencia**: Coroutines + Flow; **routing IME**: eventos hacia `InputConnection` vía comandos.

---

## 4) Patrones de diseño

- **Strategy**: mapeo por idioma/tema (`MappingStrategy` por locale; intercambiable).
- **State**: estados del IME (idle, touching, dragging, composing, selecting).
- **Command**: acciones hacia el editor (`CommitText`, `Backspace`, `SetComposing`, `FinishComposing`, `MoveCursor`).
- **Adapter**: `InputConnectionAdapter` para encapsular peculiaridades por app host.
- **Composite**: modelo de layout (KeyCluster 3×3 → Key → Variants).
- **Decorator**: enriquecimiento de gestos (filtros: snap a 8 direcciones, frenado, aceleración).
- **Observer (Flow)**: UDF y settings reactivos.
- **Builder**: generación declarativa de layouts/temas a partir de tokens.
- **Flyweight** (opcional): cache de glifos/paths SVG para íconos de teclas.

---

## 5) Motor de gestos (core\:gesture)

### 5.1 Pipeline

1. **Pointer Down** → captura celda origen.
2. **Move** → agrega muestras; filtro de ruido (Kalman/simple EMA); cálculo de vector promedio.
3. **Direction Resolver** → particiona el círculo en 8 sectores (N, NE, E, SE, S, SW, W, NW) con histéresis angular.
4. **Thresholds**: distancia mínima dependiente de DPI y tamaño de celda; umbral dinámico según velocidad (flick vs. drag).
5. **Up** → emite `Gesture(endCell, direction, duration, speed)`.

### 5.2 Umbrales y tolerancias

- Ángulo muerto alrededor de 0°±θ para distinguir **tap** vs. **swipe corto**.
- Histéresis para evitar rebotes entre sectores al pasar por bordes.
- Curvatura: si la trayectoria cambia > φ, priorizar el **último segmento estable**.

### 5.3 Errores y recuperación

- Si la celda destino ≠ origen y dirección ≈ recta, priorizar **dirección**.
- Cancelación segura: salir del área del teclado → descartar evento y restaurar UI.

---

## 6) Sistema de layout 3×3 (core\:mapping)

### 6.1 Modelo

```kotlin
data class Key(val id: KeyId, val label: String?, val actions: Map<Direction, Action>)
data class KeyCluster(val cells: List<Key>) // 9 celdas
```

**Action** puede ser símbolo, secuencia, o comando (p. ej., `Backspace`).

### 6.2 Estrategias de mapeo (Strategy)

- `EsLatamMapping`, `EnUsMapping`, `SymbolicMapping`, `NumericMapping`.
- Carga desde recursos JSON/YAML o Room para perfiles custom del usuario.
- Resolución por contexto (`EditorInfo`): email/URI → mapa con `@`, `.`, `/`, `_` en direcciones cortas.

### 6.3 Reglas especiales

- **Shift/Case**: doble tap en celda central activa **Caps Lock**; swipe arriba en centro → mayúscula puntual.
- **Borrar**: mantener en celda “borrar” → auto-repeat con aceleración.
- **Espacio**: swipe derecha en barra inferior.

---

## 7) Motor de texto y comandos (core\:text)

### 7.1 Cola de comandos (Command)

- Serializa llamadas a `InputConnection` evitando race con recomposición.
- Agrupa operaciones ( `beginBatchEdit` → comandos → `endBatchEdit`).

### 7.2 Acciones soportadas

- `CommitText(seq)`, `DeleteSurroundingText(left,right)`, `Backspace()`, `SetComposingText(seq)`, `FinishComposing()`, `SendAction(IME_ACTION_*)`, `MoveCursor(dir)`.

### 7.3 Políticas

- Campos password: desactiva sugerencias/composición.
- Número/teléfono: layout numérico/símbolos rápidos.
- Emails/URIs: prioriza `@`, `.`, `/`, `_`.

---

## 8) Presentación (feature\:keyboard) – UDF con Compose

### 8.1 Bucle MVI

```
UserIntent → Reducer → UIState → Compose(Render)
                     ↑            ↓
                 SideEffects ←——— Effects (haptics, sonido)
```

- `UIState` inmutable: celdas, foco, gesto actual, tema, `imeAction`.
- `Reducer` puro; side-effects en capa efectos (haptics/sonido/telemetry opcional).

### 8.2 Componentes UI (Design System)

- **KeyCell**: tamaño adaptable, hitSlop configurable, estados (default/pressed/dragging/disabled).
- **GestureTrail**: opcional, líneas/partículas discretas con budget de overdraw.
- **Bars**: espacio, borrar, acción (Enter/Next/Done).
- **HUD**: tooltip de carácter mientras se arrastra.

---

## 9) Design System (core\:design)

### 9.1 Tokens

- **Color**: `primary`, `onPrimary`, `surface`, `onSurface`, `warning`, `success`, `highContrast`.
- **Tipografía**: escalas para labels de tecla, HUD, títulos de ajustes.
- **Espaciado**: `xs, sm, md, lg` (dp).
- **Tamaños**: base de celda (dp) con escalado por accesibilidad.
- **Radio**: teclas redondeadas 8–16dp; contenedores 12–20dp.
- **Elevación**: 0–3 niveles; sombras suaves.
- **Motion**: duraciones 40–120ms; curvas `fastOutSlowIn` vs `linearOutSlowIn`.
- **Haptics**: leve (tap), medio (commit), continuo (auto-repeat borrar).
- **Sonidos**: clic, borrar, enter; volumen relativo al sistema.

### 9.2 Componentes y estados

- Estados: `default`, `pressed`, `dragging`, `disabled`, `focused`.
- Contraste mínimo **WCAG AA** para labels.
- `HitSlop` configurable para teclas de esquina; padding táctil extra.

### 9.3 Tematización

- Tema Claro/Oscuro + Alto Contraste.
- Soporte de **packs** (skins) basados en tokens (intercambiables sin recompilar).

---

## 10) Datos y configuración (core\:data)

- **DataStore Preferences**: flags, tema, sensibilidad, idioma, pack activo.
- **Room (opcional)**: diccionarios locales, atajos, perfiles de mapeo custom.
- **Migraciones**: schema versionado; import/export de perfiles como JSON.

---

## 11) Accesibilidad

- Tamaño de tecla adaptable a `fontScale` del sistema.
- Alto contraste y outlines fuertes en estados `pressed/dragging`.
- Modo una mano (alineación izquierda/derecha) y modo pulgar.

---

## 12) Seguridad y privacidad

- \*\*Sin \*\*`` por defecto.
- Sin lectura de contactos, SMS, etc.
- Telemetría **opt-in** y desinstalable como módulo (core\:analytics).
- Cifrado en repos locales sensibles (si se guarda aprendizaje).
- Declaración de Data Safety clara en Play.

---

## 13) Rendimiento

- **Baseline Profiles** y R8.
- Reutilización de `Path`/`Painter` en teclas; evitar recomposición por pointer moves (derive state minimal).
- Calcular dirección solo en frames discretos (p. ej., cada 8–12 ms) para budget CPU.
- Uso de `begin/endBatchEdit` para agrupar commits.

---

## 14) Pruebas y QA

- **Unit**: `ResolveSwipe`, thresholds, histéresis, mapping por idioma.
- **Property-based**: fuzzing de trayectorias para motor de gestos.
- **Snapshot UI**: estados de teclas/temas (Paparazzi/Compose Preview).
- **Instrumentation**: UiAutomator en apps host (Notas, Gmail, WhatsApp) para flujos básicos.
- **Macrobenchmark**: inicio de IME, latencia de commit, jank en arrastre.
- **Golden tests**: trayectorias → dirección esperada (fixtures).

---

## 15) Observabilidad (opcional, opt‑in)

- Crash reporting local (minidumps) o Crashlytics (módulo removible).
- Métricas: latencia de commit, tasa de errores de dirección, ANR rate.
- Trazas ligeras con `signpost` para loops críticos.

---

## 16) Internacionalización

- Locales soportados iniciales: `es-MX`, `en-US`.
- Strategy por locale y por `inputType`/`imeOptions`.
- Pluralización/strings en `resources`.

---

## 17) Integración con el sistema

- `InputMethodService` + `onCreateInputView()` → ComposeView.
- Respeto a `EditorInfo.imeOptions` (Done/Go/Send/Next/Search).
- Modo landscape/portrait (layout responsivo).
- Direct Boot: iniciar sin UI de ajustes.

---

## 18) DevEx, CI/CD y calidad

- **Estilo**: ktlint + detekt + spotless.
- **CI**: GitHub Actions → build, tests, lint, unit + instrumentation en emuladores.
- **Versionado**: SemVer app + code version.
- **Firmas**: Play App Signing; tracks: internal → closed → open → production.
- **Feature flags**: `core:data` (DataStore) con llaves documentadas.

---

---

---

## 21) Estructura de paquetes sugerida

```
app-ime/
  └─ src/... (InputMethodService, bootstrap, permisos)
feature/keyboard/
  ├─ ui/compose (KeyCell, Bars, HUD)
  ├─ mvi (Intents, Reducer, UIState)
  └─ effects (Haptics, Sounds)
feature/settings/
  ├─ ui (pantallas, theming)
  └─ domain (UseCases: Get/SetSettings)
core/gesture/
  ├─ engine (resolver, thresholds)
  └─ model (Direction, Gesture)
core/mapping/
  ├─ strategy (EsLatamMapping, EnUsMapping, ...)
  ├─ model (Key, KeyCluster, Action)
  └─ parser (JSON/YAML → modelos)
core/text/
  ├─ ic (InputConnectionAdapter, CommandQueue)
  └─ commands (CommitText, Backspace, ...)
core/design/
  ├─ tokens (color, spacing, motion, haptics)
  └─ components (primitivos visuales)
core/data/
  ├─ datastore (preferences)
  └─ room (diccionarios, perfiles)
core/domain/
  └─ usecases (ResolveSwipe, CommitAction, ApplyMapping)
core/testkit/
  └─ fixtures, golden tests, fakes
```

---

## 22) Contratos clave (interfaces)

### 22.1 Gestos

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

### 22.3 Comandos

```kotlin
sealed interface EditorCommand
object Backspace: EditorCommand
class CommitText(val text: CharSequence): EditorCommand
object FinishComposing: EditorCommand
class MoveCursor(val dx: Int): EditorCommand
```

---

## 23) Guías de UX específicas

- **Lectura de dirección**: mostrar preview del carácter en HUD durante el arrastre.
- **Errores**: vibrar breve si la dirección cae en zona muerta; no emitir carácter.
- **Descubribilidad**: overlay de entrenamiento que resalta flechas/direcciones por celda.
- **Consistencia**: acciones globales (borrar/espacio) siempre en el mismo lugar.

---

## 24) Telemetría ética (si el usuario la habilita)

- Eventos anónimos, agregados y en dispositivo por defecto; opt‑in explícito.
- Métricas de calidad: latencia, errores de dirección, uso de temas.
- Botón "Enviar diagnóstico" con redactado de datos.\\


### Apéndice A – Métricas por defecto

- `θ_dead_zone = 10°`, `θ_sector = 45°`, histéresis angular `±6°`.
- Distancia mínima swipe = `max(12dp, 0.18 * cellSize)`.
- Repetición borrar: inicial 400 ms → 80 ms (con aceleración).

### Apéndice B – Convenciones de nombres

- Módulos `core:*`, `feature:*`, `app-ime`.
- Paquetes: `com.company.keyboard.core.gesture`, etc.

### Apéndice C – Dependencias sugeridas

- Compose BOM, Accompanist (si hiciera falta), Hilt, DataStore, Room (opcional), Crashlytics (módulo opcional), Paparazzi/Shot, Macrobenchmark.

