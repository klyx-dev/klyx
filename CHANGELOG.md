# Changelog

## [1.7.0-beta.2](https://github.com/klyx-dev/klyx/compare/v1.7.0-beta.1...v1.7.0-beta.2) (2025-08-19)

### Features

* add `canExecute` utility function ([abe4df2](https://github.com/klyx-dev/klyx/commit/abe4df221cab09fc8b414f03494d662c035cd5d5))
* add ability to run commands as login user ([b7330e7](https://github.com/klyx-dev/klyx/commit/b7330e70c7b283ba405f36da34db3b6de648e663))
* add extension LanguageServer API ([f347a89](https://github.com/klyx-dev/klyx/commit/f347a8989e4a8ff9972c9b766dcca1c946dc3958))
* add kmp logger ([fc2ab48](https://github.com/klyx-dev/klyx/commit/fc2ab486567df8e19287ad6d7afa894d61c63d4f))
* add wasm ksp processor ([0e03a72](https://github.com/klyx-dev/klyx/commit/0e03a72d0f9b2cf30a006156fac9e24e6a27974b))
* enhance wasm interop with complex types ([d08cd99](https://github.com/klyx-dev/klyx/commit/d08cd9909d125ab1a9f67c9c4e78bf20234dbc5f))
* **extension-api:** implement wasm primitive types ([637dc88](https://github.com/klyx-dev/klyx/commit/637dc88176a15889a71eef07ae7faab828c67752))
* implement borrow checker for worktree ([398d8fe](https://github.com/klyx-dev/klyx/commit/398d8fe4d45fe6a225d9174dc4508b4a14dcd7da))
* implement worktree for extension ([aef008f](https://github.com/klyx-dev/klyx/commit/aef008fe7483b18ddda918c655ac94f877a6b8bf))
* improve proot integration and add ubuntuProcess ([785a286](https://github.com/klyx-dev/klyx/commit/785a2860c83c9cd888f912a871b8ad7243a15b87))
* improve wasm interop and extension api ([d6cdff2](https://github.com/klyx-dev/klyx/commit/d6cdff2cd754fe68dc81490e74457d347b621485))
* show version code in about dialog ([6c20160](https://github.com/klyx-dev/klyx/commit/6c201603b2267bb9c5d6051b46a72c132287fb51))
* **terminal:** implement extra keys ([85bab04](https://github.com/klyx-dev/klyx/commit/85bab0457462e93b9e712fa59e57507d94cf21c4))
* **wasm:** implement WASI preview 1 (incomplete) ([7c30ec9](https://github.com/klyx-dev/klyx/commit/7c30ec93bb77c68457b0f5122a5903510bdd753e))

### Bug Fixes

* **wasm:** set stdio ([2d3f743](https://github.com/klyx-dev/klyx/commit/2d3f7431cd2279b18e0a90a6469c52a773a83b38))

## [1.7.0-beta.1](https://github.com/klyx-dev/klyx/compare/v1.7.0-beta.0...v1.7.0-beta.1) (2025-08-04)

### Features

* add FileDownloader ([cea1d7e](https://github.com/klyx-dev/klyx/commit/cea1d7ee0b8099d6aedf954fe8026923e6e1434f))
* add initial terminal module ([21450d3](https://github.com/klyx-dev/klyx/commit/21450d384d2ffd29a51ea0c5917ba59bab9eea2e))
* add terminal ([9d2b0ee](https://github.com/klyx-dev/klyx/commit/9d2b0ee82bfa32c7826b2f632a88234f2f875953))
* use material3 theme ([78fc67a](https://github.com/klyx-dev/klyx/commit/78fc67a86ed004df0b173d3590a4e381ff39211f))

## [1.7.0-beta.0](https://github.com/klyx-dev/klyx/compare/v1.6.0...v1.7.0-beta.0) (2025-07-28)

### Features

* add CrashActivity ([a719896](https://github.com/klyx-dev/klyx/commit/a719896f19624affd43d8e095819f3034fa6a742))
* **editor:** add status bar and language detection ([8679d7c](https://github.com/klyx-dev/klyx/commit/8679d7c156b1afa7386eaa58abc774e1e7c2a295))
* **editor:** add status bar and language detection ([4da652b](https://github.com/klyx-dev/klyx/commit/4da652b82fb650a40418e4788f921eda35179115))
* new extension loader ([9e2e4fb](https://github.com/klyx-dev/klyx/commit/9e2e4fb91947374b679ec28bfb80c5aac3c327e5))
* **wasm:** add jimfs and allow directory mapping in WASI ([b48ae0b](https://github.com/klyx-dev/klyx/commit/b48ae0b501d9da6f5c3ac30975ca54fcf8db02bf))
* **wasm:** add Long to ValType converters ([ff2fcfd](https://github.com/klyx-dev/klyx/commit/ff2fcfd727f10d16cbc707dc6c4f5317b4c3f820))
* **wasm:** add WASI support ([ecc6cde](https://github.com/klyx-dev/klyx/commit/ecc6cde2d3330c53c826e22674003a3ebf25df05))
* **wasm:** improve WASI support and refine Wasm API ([4f6c085](https://github.com/klyx-dev/klyx/commit/4f6c085da30c45ca745f32f0d4ff6c44cafdb74a))

### Bug Fixes

* **command:** display multiple shortcuts for a command ([54ddbd1](https://github.com/klyx-dev/klyx/commit/54ddbd1e0a33f9ac7398e43fc4ef1afd9c91b4e5))
* **editor:** hide keyboard when closing a tab ([5650920](https://github.com/klyx-dev/klyx/commit/56509200a3f6c376f5551013e719866b2c37d7c8))
* **editor:** update editor text when content changes ([d11b02e](https://github.com/klyx-dev/klyx/commit/d11b02e51f5252f50d12e16e110154dbc73d2285))
* **extension:** allow unknown names in toml ([51407ed](https://github.com/klyx-dev/klyx/commit/51407ed043eb3c2f54cc0071e7013bf59a1add4f))
* **extension:** fix extension install issue ([a88a4ff](https://github.com/klyx-dev/klyx/commit/a88a4ffa41e40ebf4d6b0bfb88c8c118646f3506))
* **extension:** improve error handling ([8702af5](https://github.com/klyx-dev/klyx/commit/8702af5c2008369d07e3d822f3e0294ecc3c707c))

## [1.6.0](https://github.com/klyx-dev/klyx/compare/v1.5.0...v1.6.0) (2025-07-11)

### Features

* **commands:** improve shortcut handling and menu builder ([d1d5b18](https://github.com/klyx-dev/klyx/commit/d1d5b18453857d757c49f193ed94c158e8c7f992))
* **editor:** add Ctrl+W shortcut to close active tab ([e4ffeeb](https://github.com/klyx-dev/klyx/commit/e4ffeeb637d12cc382233c9ab3a298cd46a439c8))
* **editor:** add language support to CodeEditor ([facc4f4](https://github.com/klyx-dev/klyx/commit/facc4f4c8c619a7b14c6669530a6dfc0e4a26c76))
* **rope:** add new multiplatform library ([8c8c76b](https://github.com/klyx-dev/klyx/commit/8c8c76beb0a8561b68ab887fc4ce72009b6bb768))
* **welcome:** add WelcomeScreen component ([d24c4a5](https://github.com/klyx-dev/klyx/commit/d24c4a5dfa9b14c9904ae633d10703c31ea3b879))

### Bug Fixes

* apply status bar padding to menu bar ([3a679c9](https://github.com/klyx-dev/klyx/commit/3a679c94fa8211aeea7dc47af63ab33e586a7d37))
* **editor:** use sora editor for now ([9ef3dd2](https://github.com/klyx-dev/klyx/commit/9ef3dd2c652e666b902e7f0954adb3920d9b490a))
* KeyEvent post multiple times ([e70cbcd](https://github.com/klyx-dev/klyx/commit/e70cbcdec1dd8496e9d805ac0481fb1059958f9c))
