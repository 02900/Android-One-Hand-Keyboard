# Desarrollo

## Preparar una PC desde cero

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
3. **Toca una celda** para escribir su letra central y **desliza** hacia una etiqueta para escribirla. Los gestos se describen en [USO.md](USO.md#teclas-y-gestos). Con **TalkBack**, cada tecla es un elemento navegable y los swipes están en el menú de acciones.

### Solución de problemas

| Problema | Solución |
|---|---|
| `No Java compiler found` / `JAVA_HOME` | Apunta `JAVA_HOME` a un JDK 17+ (paso 2.4) |
| `SDK location not found` | Crea `local.properties` (paso 3) |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | La app instalada tiene otra firma: desinstálala, o restaura la clave (paso 4) |
| Gradle se queda sin memoria | Corre tests, lint y build por separado; o sube `org.gradle.jvmargs` en `gradle.properties` |
| El teléfono no encuentra el servidor local | Misma Wi-Fi, Python permitido en el firewall (UDP 47800 y TCP 8000), y el script corriendo |

## Flujo de trabajo

- **`main` está protegida:** todo entra por PR, y solo se puede mergear si pasan los checks `test` e `instrumented-test` del workflow de CI ([`ci.yml`](../.github/workflows/ci.yml)). Nadie hace push directo, tampoco los admins.
- El CI corre en PRs contra `main` o `develop`. Un PR apilado sobre otra rama no tiene CI hasta que su base pase a ser `main`.
- Cada merge a `main` publica una release (ver [ACTUALIZACIONES.md](ACTUALIZACIONES.md#publicar-una-versión-github-actions)).
- Antes de hacer commit: `./gradlew ktlintFormat`, y que pasen `testDebugUnitTest`, `ktlintCheck` y `detekt`.
- Hoja de ruta y cambios hechos: [MEJORAS.md](../MEJORAS.md).
