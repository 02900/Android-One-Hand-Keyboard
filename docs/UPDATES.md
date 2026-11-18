# Updates and releases

Only the debug build (the one that gets published) updates itself; the release build has no network permission.

## How the app updates

The app looks for the newest build in two places and keeps the most recent one:

1. **GitHub Releases** (from any network). The app reads `releases/latest/download/update.json`, which doesn't go through the GitHub API, so there is no rate limit. How to publish: [Publishing a version](#publishing-a-version-github-actions).
2. **Your PC on the local network**, to try a build before publishing it:

```bash
python tools/serve_updates.py   # builds, publishes and serves; Ctrl+C to stop
```

- **No fixed IP:** the phone asks via UDP broadcast (port 47800) and the script answers, so it keeps working when the router gives the PC a different IP. If the script isn't running, the keyboard simply finds no updates.
- **Versión** (version) in the app's settings checks when the screen opens and shows **Actualizar** (update) if there is a newer build (a different commit, never an older one). The first time, Android asks you to confirm and to allow "install apps from this source".
- **Actualizar sola** (update by itself, Android 12+): after the first update made from the app, the keyboard checks every 10 min while you type, downloads in the background and installs **when it hides**. If Android asks for confirmation instead, "⬆ actualización" appears next to the keyboard's version label.
- The APK is checked against the SHA-256 in `update.json`, and Android only installs it if it has the same signature as the installed app.
- Only the debug build has network permission (`src/debug/AndroidManifest.xml`); the release build still has none.
- The first time the script runs, Windows may ask whether Python can use private networks: allow it.

### Signing key

Every published build is signed with the same key, so each update installs over the previous one:

- **Local:** `keystore.properties` at the repo root (not in git). See [Restore the signing key](DEVELOPMENT.md#4-restore-the-signing-key). Without that file, Gradle uses the machine's debug key.
- **GitHub Actions:** the `OHK_KEYSTORE_*` secrets. See [Publishing a version](#publishing-a-version-github-actions).
- ⚠️ **Back up the `.jks` and `keystore.properties`.** If the key is lost, installed apps no longer accept updates: they have to be uninstalled and installed again.

## Publishing a version (GitHub Actions)

The [`.github/workflows/release.yml`](../.github/workflows/release.yml) workflow builds the APK, signs it with the fixed key and publishes a **GitHub Release** with the APK and its `update.json`. Phones pick it up on their own (see [How the app updates](#how-the-app-updates)).

**When it runs**

| Event | Release name |
|---|---|
| Push to `main` (including merging a PR) | `build-<commit>` |
| Push of a `v*` tag | the tag, e.g. `v1.0.1` |
| Manual: **Actions → Release → Run workflow** | `build-<commit>` (only once the workflow is on `main`) |

**Publishing with a tag**

```bash
git tag -a v1.0.1 -m "What changes"
git push origin v1.0.1
gh run watch                       # follow the workflow (~5 min)
gh release list                    # the new one shows up as "Latest"
```

An annotated tag needs a git identity; if the machine has none, pass it per command: `git -c user.name=... -c user.email=... tag -a ...`.

**What it does, step by step:** restores the key from the secrets, builds with `assembleDebug`, writes `update.json` (`python3 tools/serve_updates.py --no-build --no-serve --dir dist`), creates the release and marks it as *Latest*. If the release already exists (e.g. on a re-run), it replaces the files. Finally it deletes the key from the runner.

**Repository secrets** (Settings → Secrets and variables → Actions)

| Secret | Content |
|---|---|
| `OHK_KEYSTORE_BASE64` | The `.jks` in base64 |
| `OHK_KEYSTORE_PASSWORD` | Keystore password |
| `OHK_KEY_ALIAS` | `onehandkeyboard` |
| `OHK_KEY_PASSWORD` | Key password |

To upload them again from a PC that has the key (Git Bash):

```bash
base64 -w0 ~/.keystores/one-hand-keyboard.jks | gh secret set OHK_KEYSTORE_BASE64
gh secret set OHK_KEYSTORE_PASSWORD   # prompts for the value (storePassword in keystore.properties)
gh secret set OHK_KEY_ALIAS --body onehandkeyboard
gh secret set OHK_KEY_PASSWORD        # prompts for the value (keyPassword)
```

**If a release went wrong:** `gh release delete <tag> --cleanup-tag` deletes it and the previous one becomes *Latest* again. Phones that already updated **don't go back**, because the app only installs newer commits. To fix it, publish a new commit with the fix.

**Cost:** free. GitHub's standard runners are unlimited on public repositories, and downloads from `releases/latest/download` don't use API quota.
