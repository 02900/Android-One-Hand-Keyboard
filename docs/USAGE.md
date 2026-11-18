# Using the keyboard

The app's screens are in Spanish, so on-screen labels are quoted in Spanish below, with their meaning in parentheses.

## Install and enable

1. On the phone, open the [latest release](https://github.com/02900/Android-One-Hand-Keyboard/releases/latest) and install one of the APKs: **debug** updates itself but needs internet permission, **release** has no network access and is updated by hand (see [UPDATES.md](UPDATES.md)). If you had a build signed with a different key, uninstall it first.
2. In the app, tap **Teclado → Activar** (Keyboard → Enable), or go to **Settings → System → Languages & input → On-screen keyboard**, and enable **OneHandKeyboard**.
3. In any text field, tap the keyboard icon and choose **OneHandKeyboard**.

## Keys and gestures

Tap a cell to type its center letter, or swipe towards one of its labels (8 directions) to type that one. A bubble shows what you are about to type before you lift your finger. With **TalkBack**, every key is a focusable element and the swipes are in the actions menu.

The control column:

| Key | Tap | Swipe |
|---|---|---|
| Layer (3) | ABC ↔ 123 | ↓ memes |
| Shift (7) | once / double tap = caps lock | ↑ copy all · ↓ paste |
| Backspace (11) | delete (hold = repeat) | ← word · ↑ everything · → undo |
| Space (12–14) | space (double = ". ") | drag ↔ = cursor · ↑/↓ = language (types a space while English is disabled) |
| Enter (15) | the field's action (↵ 🔍 ➤ ✓) | → send |

## Autocorrect

The examples are Spanish, the keyboard's language.

- **When it corrects:** only when a word is closed (space or `. , ; : ? !`), and only words that **don't exist** in the dictionary or among the learned ones. Valid pairs (`esta/está`, `como/cómo`, `si/sí`) are never changed on their own: they show up in the suggestion strip.
- **How it corrects:** first missing accents (`cancion` → `canción`, `tenes` → `tenés`), then typos (swapped, extra or missing letters). A swipe in the wrong direction within the same cell costs half, because it is this keyboard's typical mistake.
- **It doesn't insist:** `⌫` right after a correction restores what you typed and learns that word; tapping the quoted word in the strip does the same. An unknown word you type twice without correcting it is learned too.
- **Where it stays out:** passwords, URLs, email, numbers and fields that ask for no suggestions. Names (capitalized mid-sentence) and words attached to digits or symbols are not corrected. Incognito fields don't learn.
- **Missed space:** two common words typed together get split, whether the space was missing (`nopuede` → `no puede`) or the tap landed on the cell above the space bar and typed its letter (t, e, s in Spanish; z, c in English): `enemexico` → `en méxico`. When splitting and correcting as one word are equally likely (`aydios`: `ay dios` or `adiós`?), nothing is forced and both options appear in the strip. `⌫` undoes the split like any correction.
- **Space bar:** a tap that slides a little without moving the cursor still types the space (it used to be lost as a trackpad move).
- **Suggestion strip:** left / center / right; the center is what space will type (bold when it is a correction).
- **Privacy:** everything is local, autocorrect doesn't use the network. Learned words live in the app's private storage and are cleared with **Palabras aprendidas → Borrar** (learned words → clear).
- **Dictionaries:** `app/src/main/assets/dict/` (~48k words per language), built with `tools/build_dictionary.py` from [FrequencyWords](https://github.com/hermitdave/FrequencyWords) (CC BY-SA 4.0, see [`LICENSE.txt`](../app/src/main/assets/dict/LICENSE.txt)).

## Meme library

Memes don't ship with the app: they are a pack (add-on) you copy to the phone.

1. Copy the pack folder to the phone's storage (for example `Download/Memes`).
2. In the app, tap **Memes → Elegir carpeta** (choose folder) and pick it. The keyboard can only read that folder (permission from Android's folder picker, no storage permission).
3. On the keyboard, swipe **down on the 123/ABC key**. `ABC` goes back to the letters.

Pack format:

```
Memes/
├── 01-Reacciones/      ← each subfolder is a tab; the number only sets the order and isn't shown
│   ├── aplausos.gif
│   └── no-puede-ser.webp
├── 02-Saludos/
└── suelto.png          ← loose files go to the "General" tab
```

- Formats: `gif`, `webp` (animated or not), `png`, `jpg`. Anything else is ignored, as are hidden files and folders (`.gitkeep`).
- Subfolders inside a category count for that category.
- The **🕘 Recientes** (recent) tab shows the last 24 sent.
- **How it's sent:** if the app accepts images in that field (WhatsApp, Telegram, Signal…), the meme is inserted directly. Otherwise it is copied to the clipboard and a message asks you to paste it.
- To update the pack, just change the files in the folder: it is read again every time you open the panel.

## Expert mode

**Modo experto** (expert mode) in the app: no key shows labels and the preview bubble doesn't appear. Key backgrounds, borders, press highlight, the version label and the suggestion strip stay. Meant for people who know the layout by heart; typing works the same.

## Updates and sharing

- Debug APK only (the release APK has no network access, so these rows don't appear): **Versión → Actualizar** (version → update) checks for a new build when the settings open. With **Actualizar sola** (update by itself, Android 12+), after the first confirmed update the next ones install on their own when the keyboard hides. Details in [UPDATES.md](UPDATES.md).
- **Compartir app → Compartir** (share app) sends the installed APK as a file (Telegram, WhatsApp…). If it is the debug APK, whoever installs it also gets the GitHub updates.
