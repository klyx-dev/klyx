# Klyx Extension API (C++)

This project provides a C++ API for writing Klyx extensions.

## Building an Extension

1. Install WASI SDK (x86_64 or ARM64):

```bash
wget https://github.com/WebAssembly/wasi-sdk/releases/download/wasi-sdk-27/wasi-sdk-27.0-x86_64-linux.tar.gz
tar -xzf wasi-sdk-27.0-x86_64-linux.tar.gz
sudo mv wasi-sdk-27.0-x86_64-linux /opt/wasi-sdk
```

2. Build the extension:

```bash
cd example
./build.sh
```

This will compile the extension to WebAssembly, and package it into a `.zip` in the `output/` directory.

## Example Extension

* `main.cpp` contains a sample extension for C++ LSP in Klyx.

## Using the API

* Include the headers from `extension_api/` to implement your own extensions.

* The build script automatically packages the extension into a zip file named `<id>-<version>.zip` based on `extension.toml`.
* This package can be loaded by Klyx as an extension.

[Limitations](https://github.com/webassembly/wasi-sdk?tab=readme-ov-file#notable-limitations)
