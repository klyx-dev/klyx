uniffi::setup_scaffolding!();

use anyhow::anyhow;
use async_fs as fs;
use std::fmt::{Display, Formatter};
use std::path::Path;
use wasmtime::component::ResourceTableError;

mod capability_granter;
mod extension;
mod extension_builder;
mod github;
mod http_client;
mod paths;
mod rel_path;
mod wasm_host;
mod types;

#[derive(Debug)]
pub struct Version(String);
uniffi::custom_newtype!(Version, String);

impl From<semver::Version> for Version {
    fn from(value: semver::Version) -> Self {
        Version(value.to_string())
    }
}

/// Expands to an immediately-invoked function expression. Good for using the ? operator
/// in functions which do not return an Option or Result.
///
/// Accepts a normal block, an async block, or an async move block.
#[macro_export]
macro_rules! maybe {
    ($block:block) => {
        (|| $block)()
    };
    (async $block:block) => {
        (async || $block)()
    };
    (async move $block:block) => {
        (async move || $block)()
    };
}

#[cfg(unix)]
/// Set the permissions for the given path so that the file becomes executable.
/// This is a noop for non-unix platforms.
pub async fn make_file_executable(path: &Path) -> std::io::Result<()> {
    fs::set_permissions(
        path,
        <fs::Permissions as fs::unix::PermissionsExt>::from_mode(0o755),
    )
    .await
}

#[cfg(not(unix))]
#[allow(clippy::unused_async)]
/// Set the permissions for the given path so that the file becomes executable.
/// This is a noop for non-unix platforms.
pub async fn make_file_executable(_path: &Path) -> std::io::Result<()> {
    Ok(())
}

/// Represents an error that occurred while stripping custom sections from a WebAssembly module.
#[derive(Debug, thiserror::Error, uniffi::Object)]
#[error("{e:?}")]
#[uniffi::export(Debug)]
pub struct WasmRuntimeError {
    e: anyhow::Error,
}

impl From<anyhow::Error> for WasmRuntimeError {
    fn from(e: anyhow::Error) -> Self {
        Self { e }
    }
}

impl From<ResourceTableError> for WasmRuntimeError {
    fn from(e: ResourceTableError) -> Self {
        match e {
            ResourceTableError::Full => Self {
                e: anyhow::Error::msg("ResourceTable has no free keys").context(anyhow!(e)),
            },
            ResourceTableError::NotPresent => Self {
                e: anyhow::Error::msg("Resource not present in table").context(anyhow!(e)),
            },
            ResourceTableError::WrongType => Self {
                e: anyhow::Error::msg("Resource present in table, but with a different type").context(anyhow!(e)),
            },
            ResourceTableError::HasChildren => Self {
                e: anyhow::Error::msg("Resource cannot be deleted because child resources exist in the table. Consult wit docs for the particular resource to see which methods may return child resources.").context(anyhow!(e)),
            }
        }
    }
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ExtensionRuntimeError {
    Inner(String),
}

impl From<anyhow::Error> for ExtensionRuntimeError {
    fn from(e: anyhow::Error) -> Self {
        ExtensionRuntimeError::Inner(e.to_string())
    }
}

impl Display for ExtensionRuntimeError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            ExtensionRuntimeError::Inner(err) => {
                write!(f, "{err}")
            }
        }
    }
}
