# Klyx

[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/klyx-dev/klyx/ci.yml?branch=main&style=for-the-badge&logo=android&link=https%3A%2F%2Fgithub.com%2Fklyx-dev%2Fklyx%2Factions%2Fworkflows%2Fci.yml)](https://github.com/klyx-dev/klyx/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/klyx-dev/klyx?style=for-the-badge&link=LICENSE)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white&link=https%3A%2F%2Fdiscord.gg%2FgTHQTPHNaT)](https://discord.gg/gTHQTPHNaT)

Klyx is a modern code editor for Android built with Jetpack Compose and Material 3 Expressive.

## Screenshots

<p align="center">
  <img src="images/screenshot_1.jpg" width="32%" alt="Screenshot 1" />
  <img src="images/screenshot_2.jpg" width="32%" alt="Screenshot 2" />
  <img src="images/screenshot_3.jpg" width="32%" alt="Screenshot 3" />
</p>

## Download

Download the latest release from the [Releases](https://github.com/klyx-dev/klyx/releases/latest) page.

## Building

```bash
git clone --recurse-submodules https://github.com/klyx-dev/klyx.git
cd klyx
./gradlew generateGrammarFiles
./gradlew assembleDebug
```

## Requirements

* Android 8.0 (API 26) or later
* Android Studio Meerkat or newer
* Android SDK and NDK

## Status

Klyx is under active development. Features and APIs may change between releases.

## Contributing

Contributions, bug reports, and feature requests are welcome.

* Issues: https://github.com/klyx-dev/klyx/issues
* Discussions: https://github.com/klyx-dev/klyx/discussions

## License

Licensed under the GNU General Public License v3.0 (GPL-3.0). See [LICENSE](LICENSE) for details.
