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

1. **GitHub Releases** (desde cualquier red). La app lee `releases/latest/download/update.json`, que no pasa por la API de GitHub, así que no hay límite de cupo. Cómo publicar: [Publicar una versión](#-publicar-una-versión-github-actions).
2. **Tu PC en la red local**, para probar un build antes de publicarlo:

```bash
python tools/serve_updates.py   # compila, publica y sirve; Ctrl+C para parar
```

- **Sin IP fija:** el teléfono pregunta por broadcast UDP (puerto 47800) y el script responde, así que sigue funcionando aunque el router le cambie la IP a la PC. Si el script no está corriendo, el teclado simplemente no encuentra actualizaciones.
- **Ajustes → Versión** busca al abrir y muestra **Actualizar** si hay un build más nuevo (otro commit, no más viejo). La primera vez Android pide confirmar y permitir "instalar apps de esta fuente".
- **Actualizar sola** (Android 12+): después de la primera actualización desde la app, el teclado busca cada 10 min mientras escribes, descarga en segundo plano e instala **al ocultarse**. Si Android pide confirmar, aparece "⬆ actualización" junto a la versión del teclado.
- El APK se verifica con el SHA-256 de `update.json`, y Android solo lo instala si tiene la misma firma que la app instalada.
- Solo el build de debug (el que se publica) tiene permiso de red (`src/debug/AndroidManifest.xml`); el de release sigue sin red.
- La primera vez que corre el script, Windows puede preguntar si Python puede usar redes privadas: hay que permitirlo.

### Clave de firma

Todas las builds publicadas se firman con la misma clave, para que cada actualización se instale sobre la anterior:

- **Local:** `keystore.properties` en la raíz del repo (no está en git). Ver [Restaurar la clave de firma](#4-restaurar-la-clave-de-firma). Sin ese archivo, Gradle usa la clave de debug de la máquina.
- **GitHub Actions:** los secretos `OHK_KEYSTORE_*`. Ver [Publicar una versión](#-publicar-una-versión-github-actions).
- ⚠️ **Haz una copia de seguridad del `.jks` y de `keystore.properties`.** Si se pierde la clave, las apps instaladas ya no aceptan actualizaciones: hay que desinstalar y volver a instalar.

**Compartir app:** Ajustes → Compartir envía el APK instalado como archivo (Telegram, WhatsApp…). Si viene de una release firmada con la clave fija, el teléfono de tu amigo también se actualiza solo desde GitHub.

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

## 🏃‍♂️ Inicio Rápido: PC con instalación limpia

Pasos para dejar una PC nueva (Windows) lista para compilar, probar y publicar. En macOS o Linux los pasos son los mismos con el gestor de paquetes de cada sistema.

### 1. Instalar las herramientas

| Herramienta | Para qué | Notas |
|---|---|---|
| **Git** | Clonar y hacer commits | |
| **Android Studio** | SDK de Android, `adb` y un JDK incluido | Ladybug (2024.2) o superior |
| **JDK 17+** | Gradle | Sirve el JBR de Android Studio o Temurin 17 |
| **Python 3.10+** | `tools/serve_updates.py` (servidor local) | Solo usa la librería estándar, no hay que instalar paquetes con pip |
| **GitHub CLI (`gh`)** | PRs, releases y secretos | Opcional, pero los comandos de esta guía lo usan |

Con winget (PowerShell):

```powershell
winget install Git.Git Google.AndroidStudio EclipseAdoptium.Temurin.17.JDK Python.Python.3.12 GitHub.cli
```

### 2. Preparar el SDK de Android

1. Abre Android Studio una vez y completa el asistente. Instala el SDK en la ruta por defecto: `C:\Users\<usuario>\AppData\Local\Android\Sdk`.
2. En **SDK Manager**, instala **Android SDK Platform 35**, **Build-Tools** y **Platform-Tools**.
3. Agrega `adb` al `PATH`: `C:\Users\<usuario>\AppData\Local\Android\Sdk\platform-tools`.
4. Si `java -version` no muestra la versión 17 o mayor, configura `JAVA_HOME`:
   ```powershell
   setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"
   ```

### 3. Clonar el repositorio

```bash
gh auth login                     # una vez: cuenta de GitHub con acceso al repo
git clone https://github.com/02900/Android-One-Hand-Keyboard.git
cd Android-One-Hand-Keyboard
```

Si abres el proyecto en Android Studio, se crea solo `local.properties` con la ruta del SDK. Si solo usas la terminal, créalo a mano en la raíz del repo:

```properties
sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
```

### 4. Restaurar la clave de firma

Sin la clave fija, Gradle firma con la clave de debug de **esa** PC. Ese APK sirve para desarrollar, pero **no actualiza** la app que ya está instalada en los teléfonos.

1. Copia el `.jks` de tu copia de seguridad a `C:\Users\<usuario>\.keystores\one-hand-keyboard.jks`.
2. Copia `keystore.properties` a la raíz del repo y corrige `storeFile` si la ruta cambió:
   ```properties
   storeFile=C:/Users/<usuario>/.keystores/one-hand-keyboard.jks
   storePassword=...
   keyAlias=onehandkeyboard
   keyPassword=...
   ```
3. Comprueba que el APK salga con la firma correcta. La huella SHA-256 debe ser `44e84cd552db74c14fda088fb8210d8643a556367ec551d3e627883c8a6fcf41`:
   ```bash
   ./gradlew assembleDebug
   "$ANDROID_HOME/build-tools/<versión>/apksigner" verify --print-certs app/build/outputs/apk/debug/app-ime-debug.apk
   ```

Los dos archivos están en `.gitignore` y nunca se suben al repo.

### 5. Compilar y verificar

Con poca memoria, conviene correr los pasos por separado:

```bash
./gradlew testDebugUnitTest      # tests unitarios (JVM + Robolectric)
./gradlew ktlintCheck detekt     # estilo y análisis estático (ktlintFormat arregla el formato)
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-ime-debug.apk
```

En PowerShell usa `.\gradlew`. La primera compilación descarga Gradle 8.13 y las dependencias, y tarda varios minutos.

### 6. Instalar en el teléfono

- **Desde GitHub** (recomendado): abre en el teléfono https://github.com/02900/Android-One-Hand-Keyboard/releases/latest e instala el APK.
- **Por cable:** activa la depuración USB y ejecuta `adb install -r app/build/outputs/apk/debug/app-ime-debug.apk`.
- **Por Wi-Fi:** ejecuta `python tools/serve_updates.py` y abre en el teléfono la URL que imprime. La primera vez, Windows pregunta si Python puede usar **redes privadas**: hay que permitirlo.

Si el teléfono tiene una versión firmada con otra clave, Android rechaza la instalación: desinstálala primero.

Después, activa el teclado:

1. **Configuración** → **Sistema** → **Idiomas e introducción de texto** → **Teclado en pantalla**, y activa **OneHandKeyboard**. También puedes usar el botón **Activar** en los ajustes de la app.
2. En cualquier campo de texto, toca el ícono de teclado y elige **OneHandKeyboard**.
3. **Toca una celda** para escribir su letra central y **desliza** hacia una etiqueta para escribirla. Las teclas de control se describen en [Arquitectura](#️-arquitectura). Con **TalkBack**, cada tecla es un elemento navegable y los swipes están en el menú de acciones.

### Solución de problemas

| Problema | Solución |
|---|---|
| `No Java compiler found` / `JAVA_HOME` | Apunta `JAVA_HOME` a un JDK 17+ (paso 2.4) |
| `SDK location not found` | Crea `local.properties` (paso 3) |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | La app instalada tiene otra firma: desinstálala, o restaura la clave (paso 4) |
| Gradle se queda sin memoria | Corre tests, lint y build por separado; o sube `org.gradle.jvmargs` en `gradle.properties` |
| El teléfono no encuentra el servidor local | Misma Wi-Fi, Python permitido en el firewall (UDP 47800 y TCP 8000), y el script corriendo |

## 🚀 Publicar una versión (GitHub Actions)

El workflow [`.github/workflows/release.yml`](.github/workflows/release.yml) compila el APK, lo firma con la clave fija y publica una **GitHub Release** con el APK y su `update.json`. Los teléfonos lo detectan solos (ver [Actualizaciones](#-actualizaciones-build-de-debug)).

**Cuándo se ejecuta**

| Evento | Nombre de la release |
|---|---|
| Push a `main` (incluye mergear un PR) | `build-<commit>` |
| Push de un tag `v*` | el tag, por ejemplo `v1.0.1` |
| Manual: **Actions → Release → Run workflow** | `build-<commit>` (solo cuando el workflow ya está en `main`) |

**Publicar con un tag**

```bash
git tag -a v1.0.1 -m "Qué cambia"
git push origin v1.0.1
gh run watch                       # sigue el workflow (~5 min)
gh release list                    # la nueva aparece como "Latest"
```

**Qué hace, paso a paso:** restaura la clave desde los secretos, compila con `assembleDebug`, escribe `update.json` (`python3 tools/serve_updates.py --no-build --no-serve --dir dist`), crea la release y la marca como *Latest*. Si la release ya existía (por ejemplo, al reintentar), reemplaza los archivos. Al final borra la clave del runner.

**Secretos del repo** (Settings → Secrets and variables → Actions)

| Secreto | Contenido |
|---|---|
| `OHK_KEYSTORE_BASE64` | El `.jks` en base64 |
| `OHK_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `OHK_KEY_ALIAS` | `onehandkeyboard` |
| `OHK_KEY_PASSWORD` | Contraseña de la clave |

Para volver a cargarlos desde una PC que tenga la clave (Git Bash):

```bash
base64 -w0 ~/.keystores/one-hand-keyboard.jks | gh secret set OHK_KEYSTORE_BASE64
gh secret set OHK_KEYSTORE_PASSWORD   # pide el valor (storePassword de keystore.properties)
gh secret set OHK_KEY_ALIAS --body onehandkeyboard
gh secret set OHK_KEY_PASSWORD        # pide el valor (keyPassword)
```

**Si una release salió mal:** `gh release delete <tag> --cleanup-tag` la borra y la anterior vuelve a ser la *Latest*. Los teléfonos que ya se actualizaron **no vuelven atrás**, porque la app solo instala commits más nuevos. Para corregir, publica un commit nuevo con el arreglo.

**Costo:** gratis. Los runners estándar de GitHub no tienen límite en repos públicos, y las descargas desde `releases/latest/download` no consumen cupo de la API.

## 🧪 Testing

El proyecto incluye:
- **Tests unitarios**: Lógica de negocio y casos de uso
- **Tests de integración**: Interacciones entre módulos
- **Tests UI**: Componentes Compose
- **Property-based testing**: Motor de gestos
- **Golden tests**: Trayectorias de gestos

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

- **Sin permisos de red** en release. El build de debug solo los usa para buscar actualizaciones en GitHub Releases y en tu PC de la red local; no envía datos.
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
