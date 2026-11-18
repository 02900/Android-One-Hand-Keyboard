# Uso del teclado

## Instalar y activar

1. En el teléfono, abre la [última release](https://github.com/02900/Android-One-Hand-Keyboard/releases/latest) e instala el APK. Si tenías una versión firmada con otra clave, desinstálala primero.
2. Ajustes de la app → **Teclado → Activar**, o **Configuración → Sistema → Idiomas e introducción de texto → Teclado en pantalla**, y activa **OneHandKeyboard**.
3. En cualquier campo de texto, toca el ícono de teclado y elige **OneHandKeyboard**.

## Teclas y gestos

Toca una celda para escribir su letra central y desliza hacia una de sus etiquetas (8 direcciones) para escribir esa. Un globo muestra lo que vas a escribir antes de soltar. Con **TalkBack**, cada tecla es un elemento navegable y los swipes están en el menú de acciones.

La columna de control:
| Tecla | Tap | Swipe / long-press |
|---|---|---|
| Capa (3) | ABC ↔ 123 | ← altura por defecto · ↓ memes · long-press + arrastrar = altura |
| Mayúsculas (7) | una vez / doble tap = bloqueo | ↑ copiar todo · ↓ pegar |
| Borrar (11) | borrar (mantener = repetir) | ← palabra · ↑ todo · → deshacer |
| Espacio (12–14) | espacio (doble = ". ") | arrastrar ↔ = cursor · ↑/↓ = idioma (con el inglés desactivado, escribe un espacio) |
| Enter (15) | acción del campo (↵ 🔍 ➤ ✓) | → enviar |

## Autocorrector

- **Cuándo corrige:** solo al cerrar una palabra (espacio o `. , ; : ? !`) y solo palabras que **no existen** ni en el diccionario ni entre las aprendidas. Las parejas válidas (`esta/está`, `como/cómo`, `si/sí`) nunca se cambian solas: aparecen en la barra.
- **Cómo corrige:** primero tildes faltantes (`cancion` → `canción`, `tenes` → `tenés`), después errores de tipeo (letras cambiadas, de más o de menos). Un swipe en la dirección equivocada dentro de la misma celda cuesta la mitad, porque es el error típico de este teclado.
- **No se impone:** `⌫` justo después de una corrección devuelve lo que escribiste y aprende esa palabra; tocar la palabra entre comillas en la barra hace lo mismo. Una palabra desconocida que escribes dos veces sin corregir también queda aprendida.
- **Dónde no actúa:** contraseñas, URL, email, números y campos que piden no sugerir. Los nombres (mayúscula en medio de la frase) y las palabras pegadas a números o símbolos no se corrigen. En campos de incógnito no aprende.
- **Espacio fallado:** dos palabras comunes pegadas se separan, tanto si el espacio no se escribió (`nopuede` → `no puede`) como si el toque cayó en la celda de arriba de la barra espaciadora y escribió su letra (t, e, s en español; z, c en inglés): `enemexico` → `en méxico`. Si separar y corregir como una sola palabra quedan parejos (`aydios`: ¿`ay dios` o `adiós`?), no se fuerza nada y las dos opciones aparecen en la barra. `⌫` deshace la separación igual que cualquier corrección.
- **Barra espaciadora:** un toque que se desliza un poco sin llegar a mover el cursor escribe el espacio (antes se perdía como un trackpad sin movimiento).
- **Barra de sugerencias:** izquierda / centro / derecha; el centro es lo que se escribirá al poner espacio (en negrita si es una corrección).
- **Privacidad:** todo es local, el autocorrector no usa la red. Las palabras aprendidas están en el almacenamiento privado de la app y se borran desde Ajustes → Escritura.
- **Diccionarios:** `app/src/main/assets/dict/` (~48k palabras por idioma), generados con `tools/build_dictionary.py` a partir de [FrequencyWords](https://github.com/hermitdave/FrequencyWords) (CC BY-SA 4.0, ver [`LICENSE.txt`](../app/src/main/assets/dict/LICENSE.txt)).

## Biblioteca de memes

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

## Modo experto

Ajustes → **Modo experto**: ninguna tecla muestra etiquetas y no aparece el globo de vista previa. Se mantienen los fondos, los bordes, el resaltado al pulsar, la versión y la barra de sugerencias. Pensado para quien se sabe el teclado de memoria; escribir funciona igual.

## Actualizaciones y compartir

- **Ajustes → Versión** busca una versión nueva al abrir y muestra **Actualizar** si la hay. Con **Actualizar sola** (Android 12+), después de la primera actualización confirmada las siguientes se instalan solas al ocultar el teclado. Detalles en [ACTUALIZACIONES.md](ACTUALIZACIONES.md).
- **Ajustes → Compartir** envía el APK instalado como archivo (Telegram, WhatsApp…). Quien lo instale también recibe las actualizaciones de GitHub.
