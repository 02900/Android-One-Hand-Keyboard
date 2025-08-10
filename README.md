# OneHandKeyboard - Android 3×3 Gesture IME

Un teclado Android innovador basado en gestos con diseño 3×3, inspirado en MessagEase, construido con Clean Architecture y Jetpack Compose.

## 🚀 Características Principales

- **Layout 3×3**: 9 teclas grandes con gestos en 8 direcciones
- **Offline-first**: Sin conexión a internet requerida, privacidad por diseño
- **Latencia ultra-baja**: ≤20ms desde gesto hasta texto
- **Accesibilidad completa**: Soporte TalkBack, alto contraste, tamaños adaptativos
- **Modular**: Arquitectura limpia con módulos independientes
- **Multiidioma**: Soporte inicial para español (es-MX) e inglés (en-US)

## 🏗️ Arquitectura

El proyecto sigue Clean Architecture con patrón MVI/UDF y está organizado en módulos:

### Módulos Core
- `core:domain` - Casos de uso y modelos de dominio
- `core:gesture` - Motor de detección de gestos
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
- `app-ime` - InputMethodService y bootstrap

## 🛠️ Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **DI**: Hilt
- **Persistencia**: DataStore + Room (opcional)
- **Concurrencia**: Coroutines + Flow
- **Testing**: JUnit, MockK, Turbine, Compose Testing
- **Calidad**: ktlint, detekt, CI/CD

## 📋 Requisitos

- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 35 (Android 15)
- **Kotlin**: 2.0.21+
- **Compose**: 2024.12.01 BOM

## 🚦 Estado del Proyecto

### ✅ Etapa 1: Bootstrap y Fundaciones (Completada)
- [x] Estructura modular creada
- [x] Herramientas de calidad configuradas
- [x] Dependencias y build system establecido

### 🔄 Próximas Etapas
- [ ] Etapa 2: IME Esqueleto + Compose
- [ ] Etapa 3: Motor de Gestos v1
- [ ] Etapa 4: Layout 3×3 y Mapping Básico
- [ ] Etapa 5: Design System v1 + Temas

## 🏃‍♂️ Inicio Rápido

### Prerrequisitos
- Android Studio Ladybug o superior
- JDK 11+

### Configuración
```bash
git clone <repository-url>
cd OneHandKeyboard
./gradlew build
```

### Ejecutar Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Verificar Calidad de Código
```bash
./gradlew ktlintCheck
./gradlew detekt
```

## 🧪 Testing

El proyecto incluye:
- **Tests unitarios**: Lógica de negocio y casos de uso
- **Tests de integración**: Interacciones entre módulos
- **Tests UI**: Componentes Compose
- **Property-based testing**: Motor de gestos
- **Golden tests**: Trayectorias de gestos

## 📱 Instalación

1. Compilar el APK: `./gradlew assembleDebug`
2. Instalar en dispositivo: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. Ir a Configuración > Sistema > Idiomas e introducción de texto > Teclado en pantalla
4. Activar "OneHandKeyboard"
5. Seleccionar como teclado predeterminado

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
