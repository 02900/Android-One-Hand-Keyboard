# Development

## Setting up a fresh PC

Steps to get a new (Windows) PC ready to build, test and publish. On macOS or Linux the steps are the same with each system's package manager.

### 1. Install the tools

| Tool | What for | Notes |
|---|---|---|
| **Git** | Clone and commit | |
| **Android Studio** | Android SDK, `adb` and a bundled JDK | Ladybug (2024.2) or newer |
| **JDK 17+** | Gradle | Android Studio's JBR or Temurin 17 both work |
| **Python 3.10+** | `tools/serve_updates.py` (local server) | Standard library only, no pip packages needed |
| **GitHub CLI (`gh`)** | PRs, releases and secrets | Optional, but the commands in this guide use it |

With winget (PowerShell):

```powershell
winget install Git.Git Google.AndroidStudio EclipseAdoptium.Temurin.17.JDK Python.Python.3.12 GitHub.cli
```

### 2. Set up the Android SDK

1. Open Android Studio once and finish the setup wizard. Install the SDK in the default location: `C:\Users\<user>\AppData\Local\Android\Sdk`.
2. In **SDK Manager**, install **Android SDK Platform 35**, **Build-Tools** and **Platform-Tools**.
3. Add `adb` to the `PATH`: `C:\Users\<user>\AppData\Local\Android\Sdk\platform-tools`.
4. If `java -version` doesn't show 17 or newer, set `JAVA_HOME`:
   ```powershell
   setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"
   ```

### 3. Clone the repository

```bash
gh auth login                     # once: a GitHub account with access to the repo
git clone https://github.com/02900/Android-One-Hand-Keyboard.git
cd Android-One-Hand-Keyboard
```

Opening the project in Android Studio creates `local.properties` with the SDK path. If you only use the terminal, create it by hand at the repo root:

```properties
sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```

### 4. Restore the signing key

Without the fixed key, Gradle signs with **that** PC's debug key. Such an APK is fine for development, but it **won't update** the app already installed on phones.

1. Copy the `.jks` from your backup to `C:\Users\<user>\.keystores\one-hand-keyboard.jks`.
2. Copy `keystore.properties` to the repo root and fix `storeFile` if the path changed:
   ```properties
   storeFile=C:/Users/<user>/.keystores/one-hand-keyboard.jks
   storePassword=...
   keyAlias=onehandkeyboard
   keyPassword=...
   ```
3. Check that the APK gets the right signature. The SHA-256 fingerprint must be `44e84cd552db74c14fda088fb8210d8643a556367ec551d3e627883c8a6fcf41`:
   ```bash
   ./gradlew assembleDebug
   "$ANDROID_HOME/build-tools/<version>/apksigner" verify --print-certs app/build/outputs/apk/debug/app-ime-debug.apk
   ```

Both files are in `.gitignore` and are never pushed to the repo.

### 5. Build and check

On machines with little memory, run the steps separately:

```bash
./gradlew testDebugUnitTest      # unit tests (JVM + Robolectric)
./gradlew ktlintCheck detekt     # style and static analysis (ktlintFormat fixes formatting)
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-ime-debug.apk
```

In PowerShell use `.\gradlew`. The first build downloads Gradle 8.13 and the dependencies and takes several minutes.

### 6. Install on the phone

- **From GitHub** (recommended): open https://github.com/02900/Android-One-Hand-Keyboard/releases/latest on the phone and install the APK.
- **Over USB:** enable USB debugging and run `adb install -r app/build/outputs/apk/debug/app-ime-debug.apk`.
- **Over Wi-Fi:** run `python tools/serve_updates.py` and open the URL it prints on the phone. The first time, Windows asks whether Python can use **private networks**: allow it.

If the phone has a build signed with a different key, Android refuses the install: uninstall it first.

Then enable the keyboard as described in [USAGE.md](USAGE.md#install-and-enable).

### Troubleshooting

| Problem | Fix |
|---|---|
| `No Java compiler found` / `JAVA_HOME` | Point `JAVA_HOME` to a JDK 17+ (step 2.4) |
| `SDK location not found` | Create `local.properties` (step 3) |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | The installed app has a different signature: uninstall it, or restore the key (step 4) |
| Gradle runs out of memory | Run tests, lint and build separately, or raise `org.gradle.jvmargs` in `gradle.properties` |
| The phone doesn't find the local server | Same Wi-Fi, Python allowed through the firewall (UDP 47800 and TCP 8000), and the script running |

## Workflow

- **`main` is protected:** everything goes through a PR, and it can only be merged when the `test` and `instrumented-test` checks of the CI workflow ([`ci.yml`](../.github/workflows/ci.yml)) pass. Nobody pushes directly, admins included.
- CI runs on PRs against `main` or `develop`. A PR stacked on another branch gets no CI until its base becomes `main`.
- Every merge to `main` publishes a release (see [UPDATES.md](UPDATES.md#publishing-a-version-github-actions)).
- Before committing: `./gradlew ktlintFormat`, and make sure `testDebugUnitTest`, `ktlintCheck` and `detekt` pass.
- Roadmap and completed work: [IMPROVEMENTS.md](IMPROVEMENTS.md).
