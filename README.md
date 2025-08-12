# OneHandKeyboard - Android 3×3 Gesture IME

Un teclado Android innovador basado en gestos con diseño 3×3, inspirado en MessagEase, construido con Clean Architecture y Android Views para máxima compatibilidad.

## 🚀 Características Principales

- **Layout 3×3**: 9 teclas grandes con gestos en 8 direcciones
- **Offline-first**: Sin conexión a internet requerida, privacidad por diseño
- **Latencia ultra-baja**: ≤20ms desde gesto hasta texto
- **Accesibilidad completa**: Soporte TalkBack, alto contraste, tamaños adaptativos
- **Modular**: Arquitectura limpia con módulos independientes
- **Multiidioma**: Soporte para español (es-MX) e inglés (en-US)
- **Layouts Dinámicos**: Intercambio entre Letters/Symbols y modos de mayúsculas
- **Gestos Avanzados**: Tap, Long Press y Swipe en 8 direcciones

## 🏗️ Arquitectura

El proyecto sigue Clean Architecture con arquitectura modular y está organizado en:

### Módulos Core
- `core:domain` - Casos de uso y modelos de dominio
- `core:gesture` - Motor de detección de gestos con dead zones e histeresis
- `core:mapping` - Sistema de mapeo (celda, dirección) → acción
- `core:text` - Adaptadores InputConnection y comandos
- `core:design` - Design System y componentes UI
- `core:data` - Repositorios, DataStore y Room
- `core:analytics` - Telemetría ética opcional
- `core:testkit` - Utilidades de testing

### Módulos Feature
- `feature:keyboard` - UI del teclado y lógica MVI
- `feature:settings` - Pantallas de configuración

### App Principal
- `app-ime` - InputMethodService refactorizado en componentes modulares:
  - **OneHandKeyboardService**: Servicio principal coordinador
  - **KeyboardConfigManager**: Gestión de configuración y estado (Hilt singleton)
  - **KeyboardUiFactory**: Creación de UI con Android Views
  - **GestureIntegration**: Integración de gestos y detección de eventos
  - **KeyboardActionProcessor**: Procesamiento de acciones y entrada de texto

## 🛠️ Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Android Views (estable para IME)
- **DI**: Hilt (configuración y gestores)
- **Persistencia**: DataStore + Room (opcional)
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
- [x] DefaultGestureDetector con dead zones e histeresis
- [x] Detección de 8 direcciones + tap + long press
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

#### Etapa 6: Refactorización Modular (ACTUAL)
- [x] Extracción de KeyboardConfigManager (Hilt singleton)
- [x] Extracción de KeyboardUiFactory (creación de UI)
- [x] Extracción de GestureIntegration (procesamiento de gestos)
- [x] Extracción de KeyboardActionProcessor (acciones de teclado)
- [x] Limpieza de código muerto y duplicado
- [x] Mejora de nombres y documentación
- [x] Validación de tests y builds exitosos

### 🔄 Próximas Etapas
- [ ] Etapa 7: Optimización y Performance
- [ ] Etapa 8: Configuración Avanzada y Personalización
- [ ] Etapa 9: Accesibilidad y UX Mejorada
- [ ] Etapa 10: Testing Integral y Estabilización

## 🏃‍♂️ Inicio Rápido

### Prerrequisitos
- **Android Studio**: Ladybug (2024.2.1) o superior
- **JDK**: 11+ (recomendado usar el JDK incluido en Android Studio)
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
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-11"
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

# Solo módulos feature
./gradlew :feature:keyboard:build :feature:settings:build
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

## 📱 Instalación y Configuración

### Paso 1: Compilar el APK
```bash
# Configurar JAVA_HOME si es necesario
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Compilar APK de debug
./gradlew :app-ime:assembleDebug
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
- **Toca las letras A-I** para escribir
- **Usa el botón ⌫** para borrar
- **Usa "Space"** para espacios
- **Usa ↵** para Enter/Done

### Configuración Adicional
- Para acceder a configuraciones: Abre la app "OneHandKeyboard" desde el launcher
- Para cambiar idioma: Configuración → Idiomas → Agregar idioma → Español/English

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

## 🔒 Privacidad

- **Sin permisos de red** por defecto
- **Sin telemetría** obligatoria
- **Datos locales** únicamente
- **Código abierto** y auditable

## 📞 Soporte

- Reportar bugs: [Issues](../../issues)
- Solicitar features: [Discussions](../../discussions)
- Documentación: [Wiki](../../wiki)

---

**Construido con ❤️ para la comunidad Android**



Como puedo hacer el build del apk? con que comando?

¡Excelente pregunta! Para generar el APK del teclado, puedes usar los siguientes comandos de Gradle:

🔨 Comandos para Build del APK

1. APK de Debug (Desarrollo):
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; ./gradlew assembleDebug --no-daemon

$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; ./gradlew assembleDebug --no-daemon -x ktlintCheck -x detekt

2. APK de Release (Producción):
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; ./gradlew assembleRelease --no-daemon

3. Build Completo (Debug + Release):
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; ./gradlew build --no-daemon -x ktlintCheck -x detekt


📍 Ubicación de los APKs generados:
Después del build exitoso, encontrarás los APKs en:

Debug APK: app/build/outputs/apk/debug/app-debug.apk
Release APK: app/build/outputs/apk/release/app-release.apk

