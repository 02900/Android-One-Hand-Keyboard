# One Hand Keyboard

An Android keyboard for typing with one hand: a 4×4 grid (3×3 letter block + control column) where every key types with a tap or with a swipe in one of 8 directions.

## Features

- **Taps and 8-direction swipes**, with a preview bubble and a control column (123, shift, backspace, space/cursor, enter).
- **Autocorrect that doesn't insist**: fixes accents and typos when a word is closed; `⌫` undoes it and learns the word.
- **Meme library**: GIFs and stickers from a phone folder you choose, sent as stickers where the app supports them; the online and debug APKs can also search Know Your Meme's images.
- **Voice typing**: swipe ← on the 123 key and speak.
- **Expert mode**: keys without labels, for typing from memory.
- **Themes** and **TalkBack** support.
- **Updates itself** from GitHub Releases and can be **shared** as an APK.
- Spanish. English is disabled until its letter layout is final. The app's screens are in Spanish.

## Install

1. On the phone, download one of the three APKs from the [latest release](https://github.com/02900/Android-One-Hand-Keyboard/releases/latest) and install it:

   | APK | Updates | Internet permission |
   |---|---|---|
   | `OneHandKeyboard-debug-<commit>.apk` (recommended) | **Automatic**: checks GitHub (and your PC on the local network) and installs new versions by itself | **Required**, only to look for and download updates |
   | `OneHandKeyboard-release-<commit>.apk` | Manual: download and install a newer release by hand | Not used: the app has no network access at all |
   | `OneHandKeyboard-online-<commit>.apk` | Manual, like release | **Required**, only for the **🔍** meme search (Know Your Meme's images) |

   They are separate apps and can be installed side by side. The debug APK also has the meme search.

2. Open the app → **Teclado → Activar** (Keyboard → Enable), and enable it in the list of keyboards.
3. In any text field, tap the keyboard icon and choose **OneHandKeyboard**.

## Documentation

| Document | Contents |
|---|---|
| [Usage](docs/USAGE.md) | Gestures, autocorrect, memes, expert mode, updating and sharing |
| [Development](docs/DEVELOPMENT.md) | Setting up a fresh PC, building, tests, troubleshooting and workflow |
| [Updates and releases](docs/UPDATES.md) | How the app updates, publishing a version, signing key and local server |
| [Architecture](docs/ARCHITECTURE.md) | Components, modules and stack |
| [Improvements](docs/IMPROVEMENTS.md) | Audit, roadmap and completed work |

## Privacy

- Everything is processed on the phone: no telemetry, and what you type is never sent anywhere (except a meme search you type in the panel's search box, which goes to Know Your Meme).
- The release APK has no network permission. The debug APK only uses it to look for updates on GitHub and on your PC on the local network, and for the meme search.
- The online APK only uses the network for the meme search: when you search from the meme panel it sends that query to knowyourmeme.com, loads the results' previews and downloads a meme from Know Your Meme's servers when you send it. No account or cookies, and nothing you type in the app's fields is sent.
- Memes are only read from the folder you choose in the settings.
- Voice typing uses Android's speech recognition service: on-device when the phone supports it, otherwise that service may send the audio to its provider (as with Gboard). The microphone is only used while you dictate.

## License

MIT. The autocorrect dictionaries (`app/src/main/assets/dict/*.tsv`) are derived from [FrequencyWords](https://github.com/hermitdave/FrequencyWords) and distributed under CC BY-SA 4.0 (see [LICENSE.txt](app/src/main/assets/dict/LICENSE.txt)).

Found a bug or have an idea? Open an [issue](../../issues).
