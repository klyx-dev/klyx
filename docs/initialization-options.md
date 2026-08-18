# Provider initialization options

## The problem

Language servers are configured in two very different ways:

- some settings arrive through the `workspace/didChangeConfiguration` notification, and
- server-specific options can only be passed **once**, inside the `initializationOptions`
  field of the `initialize` request.

`initializationOptions` is where a server expects the parameters that shape its whole
lifetime: which features to enable, which targets to analyze, which diagnostics to compute.
For a long time Klyx had no way for a plugin to provide them — `LanguageServerProvider`
only got to spawn the process, and the host always sent the same generic `initialize`
request. Plugins were effectively blind: they could start a server, but not tell it how to
behave.

That gap was not theoretical. Consider rust-analyzer with default options:

- diagnostics are **semantic-only** — macro-expansion errors are never reported unless
  `diagnostics.experimental.enable` is set;
- full cargo-check results (`checkOnSave.enable`) are off by default, so the editor misses
  an entire class of errors and warnings;
- inlay hints such as binding-mode hints (`mut x`, `ref x`) are disabled by default.

None of these can be turned on without `initializationOptions`. The host could not send
them, and the plugin could not supply them. The editor silently showed less than the
compiler knew.

## The fix

[`LanguageServerProvider`][provider] gained an optional hook:

```kotlin
fun interface LanguageServerProvider {
    ...
    fun initializationOptions(): LSPAny? = null
}
```

`LspManager` forwards whatever the provider returns into `createInitializeParams()` on
every server start (including restarts), so the value reaches the server's `initialize`
request without the plugin touching the wire protocol.

A plugin enables full rust-analyzer diagnostics with a few lines:

```kotlin
override fun initializationOptions(): LSPAny = JsonObject(
    mapOf(
        "diagnostics" to JsonObject(
            mapOf(
                "enable" to JsonPrimitive(true),
                "experimental" to JsonObject(mapOf("enable" to JsonPrimitive(true)))
            )
        ),
        "checkOnSave" to JsonObject(mapOf("enable" to JsonPrimitive(true)))
    )
)
```

## Who benefits

Every language server plugin, not just Rust:

- **rust-analyzer** — macro diagnostics, check-on-save, target selection, inlay hints
  (this was the original motivation: macro-expansion errors and full cargo-check results
  never reached the editor);
- **pyright / pylsp** — type-checking modes, stub paths, language-level options;
- **gopls** — build flags, static analysis settings, symbol style;
- **kotlin-language-server**, **typescript-language-server**, **clangd**, ... — each of
  them exposes server-specific behavior that only `initializationOptions` can activate.

The hook itself is server-agnostic: a plain JSON value that the provider owns.

## The story

This feature was born out of real frustration with a real editor. A user needed macro
diagnostics and full compiler diagnostics in the editor; the server was running, the
protocol was healthy, and yet the diagnostics panel stayed empty. The root cause was not
the server and not the protocol — it was that the SDK offered plugins no way to say "start
this server with these options". The fix is deliberately small: one default method, one
line in the startup path, and plugins suddenly have full control over how their servers
initialize.

[provider]: ../klyx-api/src/main/kotlin/com/klyx/api/lsp/LanguageServerProvider.kt