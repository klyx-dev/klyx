use anyhow::Result;
use async_trait::async_trait;
use std::{collections::HashMap, fmt::Display, sync::Arc};
use crate::ExtensionRuntimeError;
use crate::github::WasmExtensionGithubHost;
use crate::http_client::WasmExtensionHttpClientHost;
use crate::types::Command;
use crate::wasm_host::wit::ToWasmtimeResult;

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ReadTextFileError {
    Internal(String),
}

impl Display for ReadTextFileError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ReadTextFileError::Internal(err) => write!(f, "failed to read text file: {}", err),
        }
    }
}

#[uniffi::export(with_foreign)]
#[async_trait::async_trait]
pub trait WorktreeDelegate: Send + Sync + 'static {
    fn id(&self) -> u64;
    fn root_path(&self) -> String;
    async fn read_text_file(&self, path: String) -> Result<String, ReadTextFileError>;
    async fn which(&self, binary_name: String) -> Option<String>;
    async fn shell_env(&self) -> HashMap<String, String>;
}

#[uniffi::export(with_foreign)]
pub trait ProjectDelegate: Send + Sync + 'static {
    fn worktree_ids(&self) -> Vec<u64>;
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum KeyValueInsertError {
    Internal(String),
}

impl Display for KeyValueInsertError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            KeyValueInsertError::Internal(err) => {
                write!(f, "failed to insert key-value pair: {}", err)
            }
        }
    }
}

#[uniffi::export(with_foreign)]
#[async_trait]
pub trait KeyValueStoreDelegate: Send + Sync + 'static {
    async fn insert(&self, key: String, docs: String) -> Result<(), KeyValueInsertError>;
}

impl ToWasmtimeResult<()> for Result<(), KeyValueInsertError> {
    fn to_wasmtime_result(self) -> wasmtime::Result<Result<(), String>> {
        Ok(self.map_err(|error| format!("{error:?}")))
    }
}

#[derive(uniffi::Object)]
pub struct WasmExtensionHost {
    pub process: Arc<dyn WasmExtensionProcessHost>,
    pub node_runtime: Arc<dyn WasmExtensionNodeRuntimeHost>,
    pub system: Arc<dyn WasmExtensionSystemHost>,
    pub http_client: Arc<dyn WasmExtensionHttpClientHost>,
    pub github: Arc<dyn WasmExtensionGithubHost>
}

#[uniffi::export]
impl WasmExtensionHost {
    #[uniffi::constructor]
    pub fn new(
        process: Arc<dyn WasmExtensionProcessHost>,
        node_runtime: Arc<dyn WasmExtensionNodeRuntimeHost>,
        system: Arc<dyn WasmExtensionSystemHost>,
        http_client: Arc<dyn WasmExtensionHttpClientHost>,
        github: Arc<dyn WasmExtensionGithubHost>
    ) -> Self {
        Self {
            process,
            node_runtime,
            system,
            http_client,
            github
        }
    }
}

#[uniffi::export(with_foreign)]
#[async_trait]
pub trait WasmExtensionProcessHost: Send + Sync + 'static {
    async fn run_command(&self, command: Command) -> Result<Output, ProcessError>;
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ProcessError {
    Internal(String),
}

impl Display for ProcessError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ProcessError::Internal(err) => {
                write!(f, "failed to run command: {}", err)
            }
        }
    }
}

#[derive(Debug, uniffi::Record)]
pub struct Output {
    pub status: Option<i32>,
    pub stdout: Vec<u8>,
    pub stderr: Vec<u8>,
}

#[uniffi::export(with_foreign)]
#[async_trait]
pub trait WasmExtensionNodeRuntimeHost: Send + Sync + 'static {
    async fn node_binary_path(&self) -> Result<String, NodeRuntimeError>;
    async fn npm_package_latest_version(&self, name: String) -> Result<String, NodeRuntimeError>;
    async fn npm_package_installed_version(
        &self,
        local_package_directory: String,
        name: String,
    ) -> Result<Option<String>, NodeRuntimeError>;
    async fn npm_install_packages(
        &self,
        directory: String,
        packages: HashMap<String, String>,
    ) -> Result<(), NodeRuntimeError>;
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum NodeRuntimeError {
    Internal(String),
}

impl Display for NodeRuntimeError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            NodeRuntimeError::Internal(err) => {
                write!(f, "{}", err)
            }
        }
    }
}

#[uniffi::export(with_foreign)]
#[async_trait]
pub trait WasmExtensionSystemHost: Send + Sync + 'static {
    async fn show_toast(&self, message: String, duration: ToastDuration);
}

#[derive(Debug, uniffi::Enum)]
pub enum ToastDuration {
    Short,
    Long,
}

#[uniffi::export(with_foreign)]
#[async_trait]
pub trait ExtensionImports: Send + Sync + 'static {
    async fn set_language_server_installation_status(
        &self,
        server_name: String,
        status: LanguageServerInstallationStatus,
    );

    async fn get_settings(
        &self,
        location: Option<SettingsLocation>,
        category: String,
        key: Option<String>,
    ) -> Result<String, SettingsImportError>;

    async fn download_file(
        &self,
        url: String,
        path: String,
        file_type: DownloadedFileType,
    ) -> Result<(), ExtensionRuntimeError>;
}

/// The type of a downloaded file.
#[derive(Debug, uniffi::Enum)]
pub enum DownloadedFileType {
    /// A gzipped file (`.gz`).
    Gzip,
    /// A gzipped tar archive (`.tar.gz`).
    GzipTar,
    /// A ZIP file (`.zip`).
    Zip,
    /// An uncompressed file.
    Uncompressed
}

#[derive(Debug, uniffi::Enum)]
pub enum LanguageServerInstallationStatus {
    None,
    Downloading,
    CheckingForUpdate,
    Failed(String),
}

#[derive(Debug, uniffi::Record)]
pub struct SettingsLocation {
    pub worktree_id: u64,
    pub path: String,
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum SettingsImportError {
    #[error("unknown settings category: {category}")]
    UnknownCategory { category: String },

    #[error("unsupported context server settings: {0}")]
    Unsupported(String),

    #[error("invalid settings location")]
    InvalidLocation,

    #[error("failed to serialize settings: {0}")]
    SerializationError(String),

    #[error("internal error: {0}")]
    Internal(String),
}

#[derive(Debug, PartialEq, Eq, Clone, uniffi::Record)]
pub struct Manifest {
    pub id: String,
    pub name: String,
    pub version: String,
    pub description: Option<String>,
    pub repository: Option<String>,
}
