# Using the keyboard

The app's screens are in Spanish, so on-screen labels are quoted in Spanish below, with their meaning in parentheses.

## Install and enable

1. On the phone, open the [latest release](https://github.com/02900/Android-One-Hand-Keyboard/releases/latest) and install one of the APKs: **debug** updates itself but needs internet permission, **release** has no network access and is updated by hand, **online** is like release plus the meme search (see [UPDATES.md](UPDATES.md)). If you had a build signed with a different key, uninstall it first.
2. In the app, tap **Teclado → Activar** (Keyboard → Enable), or go to **Settings → System → Languages & input → On-screen keyboard**, and enable **OneHandKeyboard**.
3. In any text field, tap the keyboard icon and choose **OneHandKeyboard**.

## Keys and gestures

Tap a cell to type its center letter, or swipe towards one of its labels (8 directions) to type that one. A bubble shows what you are about to type before you lift your finger. With **TalkBack**, every key is a focusable element and the swipes are in the actions menu.

The control column:

| Key | Tap | Swipe |
|---|---|---|
| Layer (3) | ABC ↔ 123 | ← voice typing · ↓ memes · → emojis |
| Shift (7) | once / double tap = caps lock | ↑ copy all · ↓ paste (links without tracking, see below) |
| Backspace (11) | delete (hold = repeat) | ← word · ↑ everything · → undo |
| Space (12–14) | space (double = ". ") | drag ↔ = cursor · ↑/↓ = language (types a space while English is disabled) |
| Enter (15) | the field's action (↵ 🔍 ➤ ✓) | → send |

**Clean links:** pasting with the keyboard removes tracking from links. Instagram, X, TikTok, Facebook, YouTube, Threads, Reddit, LinkedIn, Spotify and similar sites lose everything after `?`, except what names the post (YouTube's `v` and `t`, Facebook's `story_fbid`…). Other sites only lose known trackers (`utm_*`, `fbclid`, `gclid`…). Pasting from the app's own long-press menu doesn't go through the keyboard, so those links stay as they are. Turn it off in settings with **Limpiar enlaces al pegar** (on by default).

## Autocorrect

The examples are Spanish, the keyboard's language.

- **When it corrects:** only when a word is closed (space or `. , ; : ? !`), and only words that **don't exist** in the dictionary or among the learned ones. Valid pairs (`esta/está`, `como/cómo`, `si/sí`) are never changed on their own: they show up in the suggestion strip.
- **How it corrects:** first missing accents (`cancion` → `canción`, `tenes` → `tenés`), then typos (swapped, extra or missing letters). A swipe in the wrong direction within the same cell costs half, because it is this keyboard's typical mistake.
- **It doesn't insist:** `⌫` right after a correction restores what you typed and learns that word; tapping the quoted word in the strip does the same. An unknown word you type twice without correcting it is learned too.
- **Where it stays out:** passwords, URLs, email, numbers and fields that ask for no suggestions. Names (capitalized mid-sentence) and words attached to digits or symbols are not corrected. Incognito fields don't learn.
- **Rare real words:** the dictionary has the ~90k most used forms; a word outside it that still exists (a full list of 639k Spanish forms says so, e.g. `chilango`, `levitamos`) is only corrected when it is almost surely a typo of a very common word. Missing accents are still added.
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
├── 01-Reacciones/      ← each subfolder is a category; the number only sets the order and isn't shown
│   ├── aplausos.gif
│   └── no-puede-ser.webp
├── 02-Saludos/
└── suelto.png          ← loose files go to the "General" category
```

- Formats: `gif`, `webp` (animated or not), `png`, `jpg`. Anything else is ignored, as are hidden files and folders (`.gitkeep`).
- Subfolders inside a category count for that category.
- **The panel:** one list with every category (its title, then its memes in a grid), from top to bottom: **🕘 Recientes** (the last 24 sent) and your folders. The tabs at the top jump to a category, and the tab of the category you are browsing stays highlighted.
- **Fast scroll:** drag the thin bar on the right edge to move through the list quickly; the tab of the category you're passing lights up.
- **See a meme big:** hold your finger on it; slide over others to see them too. It floats above the keyboard, where your finger doesn't cover it (over the list when there is no room above, as in landscape, or with TalkBack). Lifting the finger just closes it; a tap sends.
- **Panel size:** drag the grab bar on the panel's top edge up to make it taller (up to 70% of the screen) or down to make it shorter, in a single drag; the panel follows your finger and the bar shows its size (0% = the keyboard's height). It is remembered for next time, and the keyboard goes back to its normal height when you return to the keys.
- **How it's sent:** if the app accepts images in that field (WhatsApp, Telegram, Signal…), the meme is inserted directly. Otherwise it is copied to the clipboard and a message asks you to paste it.
- **Stickers:** still images (png, jpg, webp) are made square with black bars on the short side and sent in the format the app takes: a 512 px WebP sticker in WhatsApp (and Telegram, which receives a WebP), a square JPEG in Instagram. GIFs and animated WebP are sent as they are.
- To update the pack, just change the files in the folder: it is read again every time you open the panel.

### 🔍 Search Know Your Meme (online and debug APKs)

Tap **🔍** in the panel's bar: the keys come back with a search box above them, and what you type goes into the box, not into the app. Press **Enter** (or 🔍 in the box) to search [Know Your Meme](https://knowyourmeme.com)'s images; **✕** goes back to the panel.

- The results replace the categories, and more load as you scroll near the end (infinite scroll). The bar shows **✕ <your search>**: tap it to go back to your folders, or 🔍 to change the search.
- Results are sent like any other meme (sticker, square JPEG or GIF); the full image is downloaded only when you send it. The previews are small (~15 KB each) and load as they scroll into view.
- Images the site marks as sensitive, NSFW or spoilers are skipped. The rest is whatever the site returns, unfiltered.
- Without a connection the panel says so. If Know Your Meme changes its site, the search may stop working until the app is updated. Seen big, results use the small preview, so they look softer.
- It works even without a meme folder. Only the query and the images go over the network: no account or cookies.

## Emojis and Emoji Kitchen

Swipe **→ on the 123 / ABC key** to open the emoji panel; **ABC** goes back to the keys.

- **The panel:** every emoji by group (🕘 Recientes, the last 32 you typed, then faces, people, animals, food, travel, activities, objects, symbols and flags). The tabs at the top jump to a group; **⌫** deletes. Emojis your phone can't draw (too new for its font) are hidden. Skin tones are not offered yet (the default one is typed).
- **Mix two emojis (online and debug APKs):** like Gboard's Emoji Kitchen. Tap two emojis and, if Google made a mix of that pair, the strip under the bar shows it; tap it to send it as a sticker, and the two emojis you typed are replaced by it. The strip mixes the last two emojis you tapped and tells you when a pair doesn't mix (about 600 emojis mix, in 147,000 pairs).
- **How the mix is sent:** like a meme: inserted as a sticker where the app accepts images (WhatsApp, Telegram…), otherwise copied to the clipboard (and the emojis stay).
- **Where the mixes come from:** the images are Google's, downloaded from its servers (`www.gstatic.com`) when they are shown; only the list of which pairs exist ships in the app (`app/src/main/assets/emoji/`, built with `tools/build_emoji.py`, see its [`LICENSE.txt`](../app/src/main/assets/emoji/LICENSE.txt)). The release APK has no network access, so it has the emoji panel without the mixes.

## Voice typing

Swipe **← on the 123 / ABC key** and speak: the text appears as you talk and is typed when you stop. The strip shows "🎤 Escuchando…"; tap it, or any key, to finish earlier.

- The first time, the app opens its settings to ask for the **microphone** permission (**Dictado por voz → Permitir**).
- It uses Android's speech recognition service (the one Gboard uses). On-device recognition is preferred (Android 12+, with the language downloaded); otherwise that service may send the audio to its provider. The keyboard itself sends nothing.
- A space is added before the dictated text when needed, and it starts with a capital letter at the beginning of a sentence.

## Expert mode

**Modo experto** (expert mode) in the app: no key shows labels and the preview bubble doesn't appear. Key backgrounds, borders, press highlight, the version label and the suggestion strip stay. Meant for people who know the layout by heart; typing works the same.

## Updates and sharing

- Debug APK only (the release APK has no network access, so these rows don't appear): **Versión → Actualizar** (version → update) checks for a new build when the settings open. With **Actualizar sola** (update by itself, Android 12+), after the first confirmed update the next ones install on their own when the keyboard hides. Details in [UPDATES.md](UPDATES.md).
- **Compartir app → Compartir** (share app) sends the installed APK as a file (Telegram, WhatsApp…). If it is the debug APK, whoever installs it also gets the GitHub updates.
