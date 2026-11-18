# One Hand Keyboard

Teclado Android para escribir con una mano: una cuadrícula 4×4 (bloque de letras 3×3 + columna de control) donde cada tecla escribe con un toque o con un deslizamiento en 8 direcciones.

## Características

- **Toques y swipes en 8 direcciones**, con globo de vista previa y columna de control (123, mayúsculas, borrar, espacio/cursor, enter).
- **Autocorrector que no se impone**: corrige tildes y errores al cerrar la palabra, `⌫` lo deshace y aprende la palabra.
- **Biblioteca de memes**: GIFs y stickers desde una carpeta del teléfono que tú eliges.
- **Modo experto**: teclas sin etiquetas, para escribir de memoria.
- **Temas** y soporte de **TalkBack**.
- **Se actualiza solo** desde GitHub Releases y se puede **compartir** como APK.
- Español. El inglés está desactivado hasta que su distribución de letras sea la definitiva.

## Instalar

1. En el teléfono, descarga el APK de la [última release](https://github.com/02900/Android-One-Hand-Keyboard/releases/latest) e instálalo.
2. Abre la app → **Teclado → Activar**, y actívalo en la lista de teclados.
3. En cualquier campo de texto, toca el ícono de teclado y elige **OneHandKeyboard**.

## Documentación

| Documento | Contenido |
|---|---|
| [Uso](docs/USO.md) | Gestos, autocorrector, memes, modo experto, actualizar y compartir |
| [Desarrollo](docs/DESARROLLO.md) | Preparar una PC desde cero, compilar, tests, problemas comunes y flujo de trabajo |
| [Actualizaciones y releases](docs/ACTUALIZACIONES.md) | Cómo se actualiza la app, publicar una versión, clave de firma y servidor local |
| [Arquitectura](docs/ARQUITECTURA.md) | Componentes, módulos y stack |
| [MEJORAS.md](MEJORAS.md) | Hoja de ruta y cambios hechos |

## Privacidad

- Todo se procesa en el teléfono: sin telemetría y sin enviar lo que escribes.
- El build de release no tiene permiso de red. El de debug (el que se publica) solo lo usa para buscar actualizaciones en GitHub y en tu PC de la red local.
- Los memes se leen solo de la carpeta que eliges en Ajustes.

## Licencia

MIT. Los diccionarios del autocorrector (`app/src/main/assets/dict/*.tsv`) derivan de [FrequencyWords](https://github.com/hermitdave/FrequencyWords) y se distribuyen bajo CC BY-SA 4.0 (ver [LICENSE.txt](app/src/main/assets/dict/LICENSE.txt)).

¿Un bug o una idea? Abre un [issue](../../issues).
