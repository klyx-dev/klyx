# Klyx Extension API

A Rust-based API for building extensions for the Klyx platform. This API provides a WebAssembly-compatible interface for creating custom extensions that can interact with Android functionality and UI components.

## Overview

The Klyx Extension API allows developers to create extensions using Rust that compile to WebAssembly (WASM).

## Getting Started

### Prerequisites

- Rust toolchain (latest stable)
- `wasm32-wasip1` target installed
- Cargo

### Installing the WASM Target

```bash
rustup target add wasm32-wasip1
```

### Creating an Extension

1. Create a new Rust project:
```bash
cargo new --lib my_klyx_extension
cd my_klyx_extension
```

2. Add the Klyx Extension API to your `Cargo.toml`:
```toml
[package]
name = "my_klyx_extension"
version = "0.1.0"
edition = "2024"

[lib]
crate-type = ["cdylib"]

[dependencies]
klyx_extension_api = "1.2.0"
```

3. Implement your extension in `src/lib.rs`:
```rust
use klyx_extension_api::{self as klyx};

pub struct MyExtension;

impl klyx::Extension for MyExtension {
    fn new() -> Self {
        klyx::show_toast("Hello, World!", klyx::ToastDuration::Short);
        Self
    }

    ...
}

klyx::register_extension!(MyExtension);
```

4. Build your extension:
```bash
cargo build --target wasm32-wasip1 --release
```

## Building Extensions

### Development Build
```bash
cargo build --target wasm32-wasip1
```

### Release Build
```bash
cargo build --target wasm32-wasip1 --release
```

The compiled WASM file will be located at:
```
target/wasm32-wasip1/release/your_extension_name.wasm
```

## Example Extension

Here's a complete example of a simple extension:

```rust
use klyx_extension_api::{self as klyx};

struct TestExtension;

impl klyx::Extension for TestExtension {
    fn new() -> Self {
        klyx::show_toast("Hello, I am a test extension", klyx::ToastDuration::Long);
        Self
    }

    fn uninstall(&self) {
        klyx::show_toast("Uninstalling...", klyx::ToastDuration::Short);
    }
}

klyx::register_extension!(TestExtension);
```

## Limitations

- Extensions run in a sandboxed WASM environment
- Limited access to system resources
- No direct file system access
- Network requests must go through provided APIs

## Contributing

Contributions to the Klyx Extension API are welcome! Please ensure your code:

- Follows Rust best practices
- Includes appropriate documentation
- Passes all tests

## License

This project is licensed under the terms specified in the main Klyx project.

## Support

For questions, issues, or feature requests, please refer to the main Klyx project repository or documentation.
