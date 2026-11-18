# OneHandKeyboard - Android 3×3 Gesture IME

Teclado Android de una mano: una cuadrícula 4×4 (bloque de letras 3×3 + columna de control) con swipes en 8 direcciones, dibujado en un único `KeyboardView` sobre Canvas.

## 🚀 Características Principales

- **Layout 3×3**: 9 teclas grandes con gestos en 8 direcciones
- **Offline-first**: Sin conexión a internet requerida, privacidad por diseño
- **Latencia ultra-baja**: ≤20ms desde gesto hasta texto
- **Accesibilidad completa**: Soporte TalkBack, alto contraste, tamaños adaptativos
- **Modular**: Arquitectura limpia con módulos independientes
- **Multiidioma**: Soporte para español (es-MX) e inglés (en-US)
- **Layouts Dinámicos**: Intercambio entre Letters/Symbols y modos de mayúsculas
- **Gestos Simplificados**: Tap y Swipe en 8 direcciones con detección por desplazamiento
- **Sistema de Temas**: 5 tokens esenciales con colores diferenciados para grupos de celdas
- **Autocorrector sin imponerse**: corrige palabras y tildes al cerrar la palabra, con barra de sugerencias; borrar justo después deshace la corrección y el teclado aprende la palabra
- **Biblioteca de memes**: GIFs y stickers desde una carpeta del teléfono que tú eliges, por categorías, sin Internet
- **Modo experto** (ajustes): teclas sin etiquetas ni vista previa, para quien se sabe el teclado de memoria

## 🏗️ Arquitectura

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

### Gestos de la columna de control
| Tecla | Tap | Swipe / long-press |
|---|---|---|
| Capa (3) | ABC ↔ 123 | ← altura por defecto · ↓ memes · long-press + arrastrar = altura |
| Mayúsculas (7) | una vez / doble tap = bloqueo | ↑ copiar todo · ↓ pegar |
| Borrar (11) | borrar (mantener = repetir) | ← palabra · ↑ todo · → deshacer |
| Espacio (12–14) | espacio (doble = ". ") | arrastrar ↔ = cursor · ↑/↓ = idioma |
| Enter (15) | acción del campo (↵ 🔍 ➤ ✓) | → enviar |

## ✍️ Autocorrector

- **Cuándo corrige:** solo al cerrar una palabra (espacio o `. , ; : ? !`) y solo palabras que **no existen** ni en el diccionario ni entre las aprendidas. Las parejas válidas (`esta/está`, `como/cómo`, `si/sí`) nunca se cambian solas: aparecen en la barra.
- **Cómo corrige:** primero tildes faltantes (`cancion` → `canción`, `tenes` → `tenés`), después errores de tipeo (letras cambiadas, de más o de menos). Un swipe en la dirección equivocada dentro de la misma celda cuesta la mitad, porque es el error típico de este teclado.
- **No se impone:** `⌫` justo después de una corrección devuelve lo que escribiste y aprende esa palabra; tocar la palabra entre comillas en la barra hace lo mismo. Una palabra desconocida que escribes dos veces sin corregir también queda aprendida.
- **Dónde no actúa:** contraseñas, URL, email, números y campos que piden no sugerir. Los nombres (mayúscula en medio de la frase) y las palabras pegadas a números o símbolos no se corrigen. En campos de incógnito no aprende.
- **Espacio fallado:** dos palabras comunes pegadas se separan, tanto si el espacio no se escribió (`nopuede` → `no puede`) como si el toque cayó en la celda de arriba de la barra espaciadora y escribió su letra (t, e, s en español; z, c en inglés): `enemexico` → `en méxico`. Si separar y corregir como una sola palabra quedan parejos (`aydios`: ¿`ay dios` o `adiós`?), no se fuerza nada y las dos opciones aparecen en la barra. `⌫` deshace la separación igual que cualquier corrección.
- **Barra espaciadora:** un toque que se desliza un poco sin llegar a mover el cursor escribe el espacio (antes se perdía como un trackpad sin movimiento).
- **Barra de sugerencias:** izquierda / centro / derecha; el centro es lo que se escribirá al poner espacio (en negrita si es una corrección).
- **Privacidad:** todo es local, el teclado no tiene permiso de Internet. Las palabras aprendidas están en el almacenamiento privado de la app y se borran desde Ajustes → Escritura.
- **Diccionarios:** `app/src/main/assets/dict/` (~48k palabras por idioma), generados con `tools/build_dictionary.py` a partir de [FrequencyWords](https://github.com/hermitdave/FrequencyWords) (CC BY-SA 4.0, ver `assets/dict/LICENSE.txt`).

## 🖼️ Biblioteca de memes

Los memes no vienen dentro de la app: son un pack (add-on) que copias al teléfono.

1. Copia la carpeta del pack al almacenamiento del teléfono (por ejemplo `Download/Memes`).
2. Ajustes del teclado → **Biblioteca de memes** → **Elegir carpeta**. El teclado solo puede leer esa carpeta (permiso del selector de Android, sin permiso de almacenamiento).
3. En el teclado, desliza **hacia abajo en la tecla 123/ABC**. `ABC` vuelve a las letras.

Formato del pack:

```
Memes/
├── 01-Reacciones/      ← cada subcarpeta es una pestaña; el número solo ordena y no se muestra
│   ├── aplausos.gif
│   └── no-puede-ser.webp
├── 02-Saludos/
└── suelto.png          ← los archivos sueltos van a la pestaña "General"
```

- Formatos: `gif`, `webp` (animado o no), `png`, `jpg`. El resto se ignora, igual que los archivos y carpetas ocultos (`.gitkeep`).
- Subcarpetas dentro de una categoría cuentan para esa categoría.
- La pestaña **🕘 Recientes** muestra los últimos 24 enviados.
- **Cómo se envía:** si la app acepta imágenes en ese campo (WhatsApp, Telegram, Signal…), el meme se inserta directo. Si no, queda copiado en el portapapeles y aparece un aviso para pegarlo.
- Para actualizar el pack basta con cambiar los archivos de la carpeta: se vuelve a leer cada vez que abres el panel.

## 🔄 Actualizaciones (build de debug)

La app busca el build más nuevo en dos lugares y se queda con el más reciente:

1. **GitHub Releases** (desde cualquier red). Cada push a `main`, o cada tag `v*`, ejecuta `.github/workflows/release.yml`: compila, firma con la clave fija y publica una release con el APK y `update.json`. La app lee `releases/latest/download/update.json`, que no pasa por la API de GitHub, así que no hay límite de cupo. También se puede lanzar a mano desde la pestaña Actions.
2. **Tu PC en la red local**, para probar un build antes de publicarlo:

```bash
python tools/serve_updates.py   # compila, publica y sirve; Ctrl+C para parar
```

- **Sin IP fija:** el teléfono pregunta por broadcast UDP (puerto 47800) y el script responde, así que sigue funcionando aunque el router le cambie la IP a la PC. Si el script no está corriendo, el teclado simplemente no encuentra actualizaciones.
- **Ajustes → Versión** busca al abrir y muestra **Actualizar** si hay un build más nuevo (otro commit, no más viejo). La primera vez Android pide confirmar y permitir "instalar apps de esta fuente".
- **Actualizar sola** (Android 12+): después de la primera actualización desde la app, el teclado busca cada 10 min mientras escribes, descarga en segundo plano e instala **al ocultarse**. Si Android pide confirmar, aparece "⬆ actualización" junto a la versión del teclado.
- El APK se verifica con el SHA-256 de `update.json`, y Android solo lo instala si tiene la misma firma que la app instalada.
- Solo el build de debug tiene permiso de red (`src/debug/AndroidManifest.xml`); el de release sigue sin red.
- La primera vez que corre el script, Windows puede preguntar si Python puede usar redes privadas: hay que permitirlo.

### Clave de firma

Todas las builds publicadas se firman con la misma clave, para que cada actualización se instale sobre la anterior:

- **Local:** `keystore.properties` en la raíz del repo (no está en git) con `storeFile`, `storePassword`, `keyAlias` y `keyPassword`. Sin ese archivo, Gradle usa la clave de debug de la máquina.
- **GitHub Actions:** los secretos `OHK_KEYSTORE_BASE64`, `OHK_KEYSTORE_PASSWORD`, `OHK_KEY_ALIAS` y `OHK_KEY_PASSWORD`.
- ⚠️ **Haz una copia de seguridad del `.jks` y de `keystore.properties`.** Si se pierde la clave, las apps instaladas ya no aceptan actualizaciones: hay que desinstalar y volver a instalar.

**Compartir app:** Ajustes → Compartir envía el APK instalado como archivo (Telegram, WhatsApp…).

## 🛠️ Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: `View` propia dibujada en Canvas (teclado) y Compose (ajustes)
- **DI**: Hilt (configuración y gestores)
- **Persistencia**: SharedPreferences (`KeyboardPreferences`, `ThemeManager`)
- **Concurrencia**: Coroutines + Flow
- **Testing**: JUnit, Robolectric, MockK
- **Calidad**: ktlint, detekt, CI/CD

## 📋 Requisitos

- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 35 (Android 15)
- **Kotlin**: 2.0.21+
- **Compose**: 2024.12.01 BOM

## 🚦 Estado del Proyecto

### ✅ Etapas Completadas

#### Etapa 1: Bootstrap y Fundaciones
- [x] Estructura modular creada
- [x] Herramientas de calidad configuradas
- [x] Dependencias y build system establecido

#### Etapa 2: IME Esqueleto + UI
- [x] InputMethodService base implementado
- [x] UI con Android Views (estable para IME)
- [x] Integración básica funcional

#### Etapa 3: Motor de Gestos v1
- [x] DefaultGestureDetector simplificado basado en desplazamiento
- [x] Detección de 8 direcciones + tap (eliminado long press)
- [x] Tests unitarios para motor de gestos

#### Etapa 4: Layout 3×3 y Mapping Básico
- [x] Grid 3×3 con aspect ratio cuadrado
- [x] Sistema de mapping (celda, dirección) → acción
- [x] Configuración de layouts por idioma

#### Etapa 5: Design System v1 + Funcionalidades
- [x] UI moderna con gradientes y feedback visual
- [x] Layouts intercambiables (Español, Inglés, Números)
- [x] Modos de mayúsculas (lowercase, UPPERCASE, Capitalize)
- [x] Integración completa de gestos en UI

#### Etapa 6: Refactorización Modular
- [x] Extracción de KeyboardConfigManager (Hilt singleton)
- [x] Extracción de KeyboardUiFactory (creación de UI)
- [x] Extracción de GestureIntegration (procesamiento de gestos)
- [x] Extracción de KeyboardActionProcessor (acciones de teclado)
- [x] Limpieza de código muerto y duplicado
- [x] Mejora de nombres y documentación
- [x] Validación de tests y builds exitosos

#### Etapa 7: Sistema de Temas y UI Mejorada (ACTUAL)
- [x] Sistema de temas simplificado con 5 tokens esenciales
- [x] Eliminación completa de tokens legacy
- [x] Colores HEX directos para mejor contraste
- [x] Bordes selectivos en celdas del teclado IME
- [x] Labels dinámicos para botón de cambio de layout (ABC ↔ 123)
- [x] ThemeConfigurationActivity con preview de colores diferenciados
- [x] Integración completa del ThemeManager

### 🔄 Próximas Etapas
- [ ] Etapa 8: Optimización y Performance
- [ ] Etapa 9: Configuración Avanzada y Personalización
- [ ] Etapa 10: Accesibilidad y UX Mejorada
- [ ] Etapa 11: Testing Integral y Estabilización

## 🏃‍♂️ Inicio Rápido

### Prerrequisitos
- **Android Studio**: Ladybug (2024.2.1) o superior
- **JDK**: 17 (recomendado usar el JDK incluido en Android Studio)
- **Android SDK**: API 26+ (Android 8.0) como mínimo
- **Git**: Para clonar el repositorio

### Configuración del Entorno

#### Opción 1: Usando Android Studio (Recomendado)
1. Clona el repositorio:
   ```bash
   git clone <repository-url>
   cd OneHandKeyboard
   ```

2. Abre el proyecto en Android Studio
3. Android Studio configurará automáticamente el JDK y SDK
4. Sincroniza el proyecto (Sync Project with Gradle Files)

#### Opción 2: Línea de Comandos
1. Clona el repositorio:
   ```bash
   git clone <repository-url>
   cd OneHandKeyboard
   ```

2. Configura JAVA_HOME (Windows):
   ```bash
   # Usando el JDK de Android Studio
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   
   # O usando tu JDK instalado
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
   ```

3. Verifica la configuración:
   ```bash
   java -version
   ./gradlew --version
   ```

### Compilación

#### Compilar Todo el Proyecto
```bash
# Dry run para verificar configuración
./gradlew build --dry-run

# Compilación completa
./gradlew build
```

#### Compilar Solo el APK
```bash
# APK de debug
./gradlew :app-ime:assembleDebug

# APK de release
./gradlew :app-ime:assembleRelease
```

#### Compilar Módulos Específicos
```bash
# Solo módulos core
./gradlew :core:domain:build :core:gesture:build
```

### Ejecutar Tests
```bash
# Tests unitarios
./gradlew test

# Tests de un módulo específico
./gradlew :core:gesture:test

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Tests con reporte de cobertura
./gradlew testDebugUnitTestCoverage
```

### Verificar Calidad de Código
```bash
# Verificar formato de código
./gradlew ktlintCheck

# Formatear código automáticamente
./gradlew ktlintFormat

# Análisis estático
./gradlew detekt

# Verificar todo junto
./gradlew check
```

### Solución de Problemas Comunes

#### Error: "No Java compiler found"
```bash
# Windows - Configurar JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Linux/Mac - Configurar JAVA_HOME
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

#### Error: "Unresolved reference: kapt"
- Asegúrate de que todos los módulos que usan Hilt tengan `kotlin("kapt")` en sus plugins

#### Error de Sincronización de Gradle
```bash
# Limpiar y reconstruir
./gradlew clean
./gradlew build --refresh-dependencies
```

#### Problemas de Memoria
```bash
# Aumentar memoria de Gradle (gradle.properties)
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

## 🧪 Testing

El proyecto incluye:
- **Tests unitarios**: Lógica de negocio y casos de uso
- **Tests de integración**: Interacciones entre módulos
- **Tests UI**: Componentes Compose
- **Property-based testing**: Motor de gestos
- **Golden tests**: Trayectorias de gestos

## 📦 Generar el APK

```bash
./gradlew :app-ime:assembleDebug     # app/build/outputs/apk/debug/
./gradlew :app-ime:assembleRelease   # app/build/outputs/apk/release/
```

## 📱 Instalación y Configuración

### Paso 1: Compilar el APK
```bash
# Configurar JAVA_HOME si es necesario
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Compilar APK de debug (recomendado para desarrollo)
./gradlew assembleDebug --no-daemon -x ktlintCheck -x detekt

# O compilar APK de release (para producción)
./gradlew assembleRelease --no-daemon
```

El APK se generará en: `app/build/outputs/apk/debug/app-ime-debug.apk`

### Paso 2: Instalar en Dispositivo
```bash
# Conectar dispositivo Android o iniciar emulador
adb devices

# Instalar el APK
adb install app/build/outputs/apk/debug/app-ime-debug.apk
```

### Paso 3: Activar el Teclado
1. Ve a **Configuración** → **Sistema** → **Idiomas e introducción de texto**
2. Selecciona **Teclado en pantalla** (o **Virtual keyboard**)
3. Activa **"OneHandKeyboard"** en la lista
4. Confirma la activación cuando aparezca el diálogo de seguridad

### Paso 4: Seleccionar como Teclado Activo
1. Abre cualquier aplicación de texto (WhatsApp, Notas, etc.)
2. Toca un campo de texto para que aparezca el teclado
3. Toca el ícono de teclado en la barra de navegación
4. Selecciona **"OneHandKeyboard"** de la lista

### Paso 5: ¡Probar el Teclado!
- **Toca una celda** para escribir su letra central y **desliza** hacia una etiqueta para escribirla
  (un globo muestra lo que vas a escribir antes de soltar).
- Las teclas de control de la columna derecha y la fila inferior se describen en la tabla de
  [Arquitectura](#️-arquitectura).
- **TalkBack**: cada tecla es un elemento navegable; los swipes están en el menú de acciones.

### Configuración Adicional
- **Temas**: Abre la app "OneHandKeyboard" desde el launcher para cambiar temas
- **Idiomas**: Configuración → Idiomas → Agregar idioma → Español/English
- **Colores diferenciados**: Las celdas primarias (00-02, 10-12, 20-22) y secundarias (03, 13, 23, 30-33) usan colores distintos

## 🤝 Contribuir

1. Fork el proyecto
2. Crear rama feature: `git checkout -b feature/nueva-funcionalidad`
3. Commit cambios: `git commit -am 'Agregar nueva funcionalidad'`
4. Push a la rama: `git push origin feature/nueva-funcionalidad`
5. Crear Pull Request

### Estándares de Código
- Seguir convenciones de Kotlin
- Ejecutar `./gradlew ktlintFormat` antes de commit
- Mantener cobertura de tests >80%
- Documentar APIs públicas

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

Los diccionarios del autocorrector (`app/src/main/assets/dict/*.tsv`) derivan de FrequencyWords
y se distribuyen bajo CC BY-SA 4.0 (ver [`assets/dict/LICENSE.txt`](app/src/main/assets/dict/LICENSE.txt)).

## 🔒 Privacidad

- **Sin permisos de red** en release (el build de debug solo los usa para actualizarse desde tu PC en la red local)
- **Memes:** solo lee la carpeta que eliges en ajustes; nada se descarga ni se sube
- **Sin telemetría** obligatoria
- **Datos locales** únicamente
- **Código abierto** y auditable

## 📞 Soporte

- Reportar bugs: [Issues](../../issues)
- Solicitar features: [Discussions](../../discussions)
- Documentación: [Wiki](../../wiki)

---

**Construido con ❤️ para la comunidad Android**
