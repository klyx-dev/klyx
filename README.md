# Klyx

[![License](https://img.shields.io/github/license/klyx-dev/klyx?style=for-the-badge&link=LICENSE)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white&link=https%3A%2F%2Fdiscord.gg%2FgTHQTPHNaT)](https://discord.gg/gTHQTPHNaT)
[![Telegram](https://img.shields.io/badge/Telegram-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/klyx_editor)

Klyx is a modern code editor for Android.

## Download

Download the latest release from the [Releases](https://github.com/klyx-dev/klyx/releases) page.

## Deep links

Open a local project or file, or create an empty file, by passing its absolute path in the `path` query parameter. Percent-encode special characters in the value, including spaces (`%20`), `&` (`%26`), `+` (`%2B`), `#` (`%23`), and `%` (`%25`).

- `klyx://open?path=/storage/emulated/0/Projects/My%20App` opens the directory as a project.
- `klyx://open?path=/storage/emulated/0/Projects/My%20App/README.md` opens the file in the editor.
- `klyx://new?path=/storage/emulated/0/Projects/My%20App/notes.txt` creates an empty file and opens it in the editor. The parent directory must exist and the filename must be unused.

Android and iOS register the scheme when the app is installed. Use paths accessible to Klyx on that device. On desktop, the JVM launcher accepts a URL as a single command-line argument. To enable clickable links for a desktop installation:

- **Linux:** With the `klyx` launcher on the desktop session’s PATH, copy [klyx.desktop](app/desktop/packaging/linux/klyx.desktop) to `~/.local/share/applications/`, then run `xdg-mime default klyx.desktop x-scheme-handler/klyx`.
- **Windows:** Run [register-url-handler.ps1](app/desktop/packaging/windows/register-url-handler.ps1) with `-Launcher 'C:\path\to\klyx.exe'` to register the launcher for the current user.
- **macOS:** For a bundled JVM launcher, include the `CFBundleURLTypes` entry from the [Info.plist](app/desktop/packaging/macos/Info.plist) in the bundle’s Info.plist before signing. Klyx receives the URL through the desktop Open URI handler.

## Building

Use JDK 21 and install Android SDK Platforms 37 and 37.1 through SDK Manager. Set `ANDROID_HOME` to your SDK directory, or set `sdk.dir` in the repository's `local.properties`.

Build the Classic and Material 3 (M3) debug APKs with the Gradle wrapper:

```bash
git clone https://github.com/klyx-dev/klyx.git
cd klyx
./gradlew :app:android:assembleDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`. The debug APKs are written to:

- `app/android/build/outputs/apk/classic/debug/android-classic-debug.apk`
- `app/android/build/outputs/apk/m3/debug/android-m3-debug.apk`

To install one flavor on a connected Android device or running emulator, run `./gradlew :app:android:installClassicDebug` or `./gradlew :app:android:installM3Debug`.

### Signed release APKs

Create `app/android/sign.properties` with your keystore details. Git ignores this file.

```properties
storeFile=/absolute/path/to/klyx.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

Run `./gradlew :app:android:assembleRelease` to generate both signed APKs:

- `app/android/build/outputs/apk/classic/release/android-classic-release.apk`
- `app/android/build/outputs/apk/m3/release/android-m3-release.apk`

## Status

Klyx is under active development. Features and APIs may change between releases.

## Contributing

Contributions, bug reports, and feature requests are welcome.

* Issues: https://github.com/klyx-dev/klyx/issues
* Discussions: https://github.com/klyx-dev/klyx/discussions

## Translations

Help translate Klyx! Visit [Weblate](https://hosted.weblate.org/engage/klyx/) to get started.

[![Translation status](https://hosted.weblate.org/widget/klyx/translations/matrix-blue.svg)](https://hosted.weblate.org/engage/klyx/)

## License

Licensed under the GNU General Public License v3.0 (GPL-3.0). See [LICENSE](LICENSE) for details.
