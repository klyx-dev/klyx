use crate::http_client::{HttpResponse, HttpResponseStream};
use crate::types::lsp::{CompletionKind, InsertTextFormat, SymbolKind};
use crate::wasm_host::wit::latest::http_client::{HttpMethod, RedirectPolicy};
use crate::wasm_host::wit::latest::lsp::CompletionLabelDetails;
use crate::{
    extension::{KeyValueStoreDelegate, Output, ProjectDelegate, WorktreeDelegate},
    make_file_executable, maybe,
    paths::PathStyle,
    rel_path::RelPath,
    wasm_host::{WasmState, wit::ToWasmtimeResult},
};
use anyhow::{Context, Result, anyhow};
use async_trait::async_trait;
use futures_util::TryFutureExt;
use semver::Version;
use std::{
    env,
    path::{Path, PathBuf},
    sync::{Arc, OnceLock},
};
use wasmtime::component::{Linker, Resource};
use zip::ZipArchive;

mod settings {
    #![allow(dead_code)]
    include!(concat!(env!("OUT_DIR"), "/since_v1.3.6/settings.rs"));
}

pub const MIN_VERSION: Version = Version::new(1, 3, 6);
pub const MAX_VERSION: Version = Version::new(1, 3, 6);

wasmtime::component::bindgen!({
    async: true,
    trappable_imports: true,
    path: "../../crates/extension_api/wit/since_v1.3.6",
    with: {
        "worktree": ExtensionWorktree,
        "project": ExtensionProject,
        "key-value-store": ExtensionKeyValueStore,
        "klyx:extension/http-client/http-response-stream": ExtensionHttpResponseStream
    },
});

pub use self::klyx::extension::*;

pub type ExtensionWorktree = Arc<dyn WorktreeDelegate>;
pub type ExtensionProject = Arc<dyn ProjectDelegate>;
pub type ExtensionKeyValueStore = Arc<dyn KeyValueStoreDelegate>;
pub type ExtensionHttpResponseStream = Arc<dyn HttpResponseStream>;

pub fn linker() -> &'static Linker<WasmState> {
    static LINKER: OnceLock<Linker<WasmState>> = OnceLock::new();
    LINKER.get_or_init(|| super::new_linker(Extension::add_to_linker))
}

impl From<LanguageServerInstallationStatus> for crate::extension::LanguageServerInstallationStatus {
    fn from(status: LanguageServerInstallationStatus) -> Self {
        match status {
            LanguageServerInstallationStatus::None => {
                crate::extension::LanguageServerInstallationStatus::None
            }
            LanguageServerInstallationStatus::Downloading => {
                crate::extension::LanguageServerInstallationStatus::Downloading
            }
            LanguageServerInstallationStatus::CheckingForUpdate => {
                crate::extension::LanguageServerInstallationStatus::CheckingForUpdate
            }
            LanguageServerInstallationStatus::Failed(err) => {
                crate::extension::LanguageServerInstallationStatus::Failed(err)
            }
        }
    }
}

impl From<SettingsLocation> for crate::extension::SettingsLocation {
    fn from(location: SettingsLocation) -> Self {
        Self {
            worktree_id: location.worktree_id,
            path: location.path,
        }
    }
}

impl From<DownloadedFileType> for crate::extension::DownloadedFileType {
    fn from(file_type: DownloadedFileType) -> Self {
        match file_type {
            DownloadedFileType::Gzip => crate::extension::DownloadedFileType::Gzip,
            DownloadedFileType::GzipTar => crate::extension::DownloadedFileType::Gzip,
            DownloadedFileType::Zip => crate::extension::DownloadedFileType::Zip,
            DownloadedFileType::Uncompressed => crate::extension::DownloadedFileType::Uncompressed,
        }
    }
}

impl ExtensionImports for WasmState {
    async fn download_file(
        &mut self,
        url: String,
        path: String,
        file_type: DownloadedFileType,
    ) -> Result<Result<(), String>> {
        self.capability_granter.grant_download_file(url.clone())?;

        self.host
            .imports
            .download_file(url, path, file_type.into())
            .await
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }

    async fn unzip_file(
        &mut self,
        file_path: String,
        destination_path: String,
    ) -> wasmtime::Result<Result<(), String>> {
        maybe!(async {
            let zip_path = PathBuf::from(file_path);
            let dest_path = PathBuf::from(destination_path);

            tokio::fs::create_dir_all(&dest_path)
                .await
                .map_err(|e| anyhow::anyhow!("creating destination dir failed: {e}"))?;

            tokio::task::spawn_blocking(move || {
                use std::fs::File;

                let file = File::open(&zip_path)?;
                let mut archive = ZipArchive::new(file)?;
                archive.extract(&dest_path)?;
                Ok::<_, anyhow::Error>(())
            })
            .await
            .map_err(|e| anyhow::anyhow!("zip extraction task panicked: {e}"))??;

            Ok::<_, anyhow::Error>(())
        })
        .await
        .to_wasmtime_result()
    }

    async fn get_settings(
        &mut self,
        location: Option<self::SettingsLocation>,
        category: String,
        key: Option<String>,
    ) -> wasmtime::Result<Result<String, String>> {
        self.host
            .imports
            .get_settings(location.map(|l| l.into()), category, key)
            .map_err(|e| anyhow!(e))
            .await
            .to_wasmtime_result()
    }

    async fn make_file_executable(&mut self, path: String) -> wasmtime::Result<Result<(), String>> {
        let path = self
            .host
            .writeable_path_from_extension(&self.manifest.id, Path::new(&path))?;

        make_file_executable(&path)
            .await
            .with_context(|| format!("setting permissions for path {path:?}"))
            .to_wasmtime_result()
    }

    async fn set_language_server_installation_status(
        &mut self,
        server_name: String,
        status: LanguageServerInstallationStatus,
    ) -> wasmtime::Result<()> {
        self.host
            .imports
            .set_language_server_installation_status(server_name, status.into())
            .await;
        Ok(())
    }
}

impl HostKeyValueStore for WasmState {
    async fn insert(
        &mut self,
        kv_store: Resource<ExtensionKeyValueStore>,
        key: String,
        value: String,
    ) -> wasmtime::Result<Result<(), String>> {
        let kv_store = self.table.get(&kv_store)?;
        kv_store.insert(key, value).await.to_wasmtime_result()
    }

    async fn drop(&mut self, _worktree: Resource<ExtensionKeyValueStore>) -> Result<()> {
        // We only ever hand out borrows of key-value stores.
        Ok(())
    }
}

impl HostProject for WasmState {
    async fn worktree_ids(
        &mut self,
        project: Resource<ExtensionProject>,
    ) -> wasmtime::Result<Vec<u64>> {
        let project = self.table.get(&project)?;
        Ok(project.worktree_ids())
    }

    async fn drop(&mut self, _project: Resource<Project>) -> Result<()> {
        // We only ever hand out borrows of projects.
        Ok(())
    }
}

impl HostWorktree for WasmState {
    async fn id(&mut self, delegate: Resource<Arc<dyn WorktreeDelegate>>) -> wasmtime::Result<u64> {
        let delegate = self.table.get(&delegate)?;
        Ok(delegate.id())
    }

    async fn root_path(
        &mut self,
        delegate: Resource<Arc<dyn WorktreeDelegate>>,
    ) -> wasmtime::Result<String> {
        let delegate = self.table.get(&delegate)?;
        Ok(delegate.root_path())
    }

    async fn read_text_file(
        &mut self,
        delegate: Resource<Arc<dyn WorktreeDelegate>>,
        path: String,
    ) -> wasmtime::Result<Result<String, String>> {
        let delegate = self.table.get(&delegate)?;
        Ok(delegate
            .read_text_file(RelPath::new(Path::new(&path), PathStyle::Posix)?.to_proto())
            .await
            .map_err(|error| error.to_string()))
    }

    async fn which(
        &mut self,
        delegate: Resource<Arc<dyn WorktreeDelegate>>,
        binary_name: String,
    ) -> wasmtime::Result<Option<String>> {
        let delegate = self.table.get(&delegate)?;
        Ok(delegate.which(binary_name).await)
    }

    async fn shell_env(
        &mut self,
        delegate: Resource<Arc<dyn WorktreeDelegate>>,
    ) -> wasmtime::Result<EnvVars> {
        let delegate = self.table.get(&delegate)?;
        Ok(delegate.shell_env().await.into_iter().collect())
    }

    async fn drop(&mut self, _worktree: Resource<Worktree>) -> Result<()> {
        // We only ever hand out borrows of worktrees.
        Ok(())
    }
}

impl common::Host for WasmState {}

impl From<crate::github::GithubRelease> for github::GithubRelease {
    fn from(value: crate::github::GithubRelease) -> Self {
        Self {
            version: value.tag_name,
            assets: value.assets.into_iter().map(Into::into).collect(),
        }
    }
}

impl From<crate::github::GithubReleaseAsset> for github::GithubReleaseAsset {
    fn from(value: crate::github::GithubReleaseAsset) -> Self {
        Self {
            name: value.name,
            download_url: value.browser_download_url,
        }
    }
}

impl github::Host for WasmState {
    async fn latest_github_release(
        &mut self,
        repo: String,
        options: github::GithubReleaseOptions,
    ) -> wasmtime::Result<Result<github::GithubRelease, String>> {
        self.host
            .extension_host
            .github
            .latest_github_release(repo, options.require_assets, options.pre_release)
            .await
            .map(Into::into)
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }

    async fn github_release_by_tag_name(
        &mut self,
        repo: String,
        tag: String,
    ) -> wasmtime::Result<Result<github::GithubRelease, String>> {
        self.host
            .extension_host
            .github
            .github_release_by_tag_name(repo, tag)
            .await
            .map(Into::into)
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }
}

#[async_trait]
impl lsp::Host for WasmState {}

impl From<HttpResponse> for http_client::HttpResponse {
    fn from(value: HttpResponse) -> Self {
        Self {
            headers: value.headers.into_iter().collect(),
            body: value.body,
        }
    }
}

impl From<http_client::HttpRequest> for crate::http_client::HttpRequest {
    fn from(request: http_client::HttpRequest) -> Self {
        Self {
            method: request.method.into(),
            url: request.url,
            headers: request.headers.into_iter().collect(),
            body: request.body,
            redirect_policy: request.redirect_policy.into(),
        }
    }
}

impl From<HttpMethod> for crate::http_client::HttpMethod {
    fn from(method: HttpMethod) -> Self {
        match method {
            HttpMethod::Get => crate::http_client::HttpMethod::Get,
            HttpMethod::Head => crate::http_client::HttpMethod::Head,
            HttpMethod::Post => crate::http_client::HttpMethod::Post,
            HttpMethod::Put => crate::http_client::HttpMethod::Put,
            HttpMethod::Delete => crate::http_client::HttpMethod::Delete,
            HttpMethod::Options => crate::http_client::HttpMethod::Options,
            HttpMethod::Patch => crate::http_client::HttpMethod::Patch,
        }
    }
}

impl From<RedirectPolicy> for crate::http_client::RedirectPolicy {
    fn from(policy: RedirectPolicy) -> Self {
        match policy {
            RedirectPolicy::NoFollow => crate::http_client::RedirectPolicy::NoFollow,
            RedirectPolicy::FollowLimit(limit) => {
                crate::http_client::RedirectPolicy::FollowLimit(limit)
            }
            RedirectPolicy::FollowAll => crate::http_client::RedirectPolicy::FollowAll,
        }
    }
}

impl http_client::Host for WasmState {
    async fn fetch(
        &mut self,
        request: http_client::HttpRequest,
    ) -> wasmtime::Result<Result<http_client::HttpResponse, String>> {
        self.host
            .extension_host
            .http_client
            .fetch(request.into())
            .await
            .map(Into::into)
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }

    async fn fetch_stream(
        &mut self,
        request: http_client::HttpRequest,
    ) -> wasmtime::Result<Result<Resource<ExtensionHttpResponseStream>, String>> {
        maybe!(async {
            let response = self
                .host
                .extension_host
                .http_client
                .fetch_stream(request.into())?;

            let resource = self.table.push(response)?;
            Ok(resource)
        })
        .await
        .to_wasmtime_result()
    }
}

impl http_client::HostHttpResponseStream for WasmState {
    async fn next_chunk(
        &mut self,
        resource: Resource<ExtensionHttpResponseStream>,
    ) -> wasmtime::Result<Result<Option<Vec<u8>>, String>> {
        self.table
            .get(&resource)?
            .clone()
            .next_chunk()
            .await
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }

    async fn drop(&mut self, _resource: Resource<ExtensionHttpResponseStream>) -> Result<()> {
        Ok(())
    }
}

impl platform::Host for WasmState {
    async fn current_platform(&mut self) -> Result<(platform::Os, platform::Architecture)> {
        Ok((
            match env::consts::OS {
                "macos" => platform::Os::Mac,
                "linux" => platform::Os::Linux,
                "windows" => platform::Os::Windows,
                "android" => platform::Os::Android,
                "ios" => platform::Os::Ios,
                _ => panic!("unsupported os"),
            },
            match env::consts::ARCH {
                "aarch64" => platform::Architecture::Aarch64,
                "x86" => platform::Architecture::X86,
                "x86_64" => platform::Architecture::X8664,
                _ => panic!("unsupported architecture"),
            },
        ))
    }
}

impl From<Output> for process::Output {
    fn from(output: Output) -> Self {
        Self {
            status: output.status,
            stdout: output.stdout,
            stderr: output.stderr,
        }
    }
}

impl From<process::Command> for crate::types::Command {
    fn from(command: process::Command) -> Self {
        Self {
            command: command.command,
            args: command.args,
            env: command.env.into_iter().collect(),
        }
    }
}

impl process::Host for WasmState {
    async fn run_command(
        &mut self,
        command: process::Command,
    ) -> wasmtime::Result<Result<process::Output, String>> {
        maybe!(async {
            self.capability_granter
                .grant_exec(command.command.clone(), command.args.clone())?;

            let output = self
                .host
                .extension_host
                .process
                .run_command(command.into())
                .await?;

            Ok(output.into())
        })
        .await
        .to_wasmtime_result()
    }
}

impl nodejs::Host for WasmState {
    async fn node_binary_path(&mut self) -> wasmtime::Result<Result<String, String>> {
        self.host
            .extension_host
            .node_runtime
            .node_binary_path()
            .await
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }

    async fn npm_package_latest_version(
        &mut self,
        package_name: String,
    ) -> wasmtime::Result<Result<String, String>> {
        self.host
            .extension_host
            .node_runtime
            .npm_package_latest_version(package_name)
            .await
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }

    async fn npm_package_installed_version(
        &mut self,
        package_name: String,
    ) -> wasmtime::Result<Result<Option<String>, String>> {
        self.host
            .extension_host
            .node_runtime
            .npm_package_installed_version(
                self.work_dir().to_string_lossy().to_string(),
                package_name,
            )
            .await
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }

    async fn npm_install_package(
        &mut self,
        package_name: String,
        version: String,
    ) -> wasmtime::Result<Result<(), String>> {
        self.capability_granter
            .grant_npm_install_package(package_name.clone())?;

        self.host
            .extension_host
            .node_runtime
            .npm_install_packages(
                self.work_dir().to_string_lossy().to_string(),
                [(package_name, version)].into_iter().collect(),
            )
            .await
            .map_err(|e| anyhow!(e))
            .to_wasmtime_result()
    }
}

impl system::Host for WasmState {
    async fn show_toast(
        &mut self,
        message: String,
        duration: system::ToastDuration,
    ) -> wasmtime::Result<()> {
        self.host
            .extension_host
            .system
            .show_toast(message, duration.into())
            .await;
        Ok(())
    }
}

impl From<system::ToastDuration> for crate::extension::ToastDuration {
    fn from(duration: system::ToastDuration) -> Self {
        match duration {
            system::ToastDuration::Short => crate::extension::ToastDuration::Short,
            system::ToastDuration::Long => crate::extension::ToastDuration::Long,
        }
    }
}

impl From<CodeLabel> for crate::types::CodeLabel {
    fn from(value: CodeLabel) -> Self {
        Self {
            code: value.code,
            spans: value.spans.into_iter().map(Into::into).collect(),
            filter_range: value.filter_range.into(),
        }
    }
}

impl From<Range> for crate::types::UIntSpan {
    fn from(range: Range) -> Self {
        Self {
            start: range.start,
            end: range.end
        }
    }
}

impl From<CodeLabelSpan> for crate::types::CodeLabelSpan {
    fn from(value: CodeLabelSpan) -> Self {
        match value {
            CodeLabelSpan::CodeRange(range) => Self::CodeRange(range.into()),
            CodeLabelSpan::Literal(literal) => Self::Literal(literal.into()),
        }
    }
}

impl From<CodeLabelSpanLiteral> for crate::types::CodeLabelSpanLiteral {
    fn from(value: CodeLabelSpanLiteral) -> Self {
        Self {
            text: value.text,
            highlight_name: value.highlight_name,
        }
    }
}

impl From<crate::types::lsp::Completion> for Completion {
    fn from(value: crate::types::lsp::Completion) -> Self {
        Self {
            label: value.label,
            label_details: value.label_details.map(Into::into),
            detail: value.detail,
            kind: value.kind.map(Into::into),
            insert_text_format: value.insert_text_format.map(Into::into),
        }
    }
}

impl From<crate::types::lsp::CompletionLabelDetails> for CompletionLabelDetails {
    fn from(value: crate::types::lsp::CompletionLabelDetails) -> Self {
        Self {
            detail: value.detail,
            description: value.description,
        }
    }
}

impl From<CompletionKind> for lsp::CompletionKind {
    fn from(value: CompletionKind) -> Self {
        match value {
            CompletionKind::Text => lsp::CompletionKind::Text,
            CompletionKind::Method => lsp::CompletionKind::Method,
            CompletionKind::Function => lsp::CompletionKind::Function,
            CompletionKind::Constructor => lsp::CompletionKind::Constructor,
            CompletionKind::Field => lsp::CompletionKind::Field,
            CompletionKind::Variable => lsp::CompletionKind::Variable,
            CompletionKind::Class => lsp::CompletionKind::Class,
            CompletionKind::Interface => lsp::CompletionKind::Interface,
            CompletionKind::Module => lsp::CompletionKind::Module,
            CompletionKind::Property => lsp::CompletionKind::Property,
            CompletionKind::Unit => lsp::CompletionKind::Unit,
            CompletionKind::Value => lsp::CompletionKind::Value,
            CompletionKind::Enum => lsp::CompletionKind::Enum,
            CompletionKind::Keyword => lsp::CompletionKind::Keyword,
            CompletionKind::Snippet => lsp::CompletionKind::Snippet,
            CompletionKind::Color => lsp::CompletionKind::Color,
            CompletionKind::File => lsp::CompletionKind::File,
            CompletionKind::Reference => lsp::CompletionKind::Reference,
            CompletionKind::Folder => lsp::CompletionKind::Folder,
            CompletionKind::EnumMember => lsp::CompletionKind::EnumMember,
            CompletionKind::Constant => lsp::CompletionKind::Constant,
            CompletionKind::Struct => lsp::CompletionKind::Struct,
            CompletionKind::Event => lsp::CompletionKind::Event,
            CompletionKind::Operator => lsp::CompletionKind::Operator,
            CompletionKind::TypeParameter => lsp::CompletionKind::TypeParameter,
            CompletionKind::Other(value) => lsp::CompletionKind::Other(value),
        }
    }
}

impl From<InsertTextFormat> for lsp::InsertTextFormat {
    fn from(value: InsertTextFormat) -> Self {
        match value {
            InsertTextFormat::PlainText => lsp::InsertTextFormat::PlainText,
            InsertTextFormat::Snippet => lsp::InsertTextFormat::Snippet,
            InsertTextFormat::Other(value) => lsp::InsertTextFormat::Other(value),
        }
    }
}

impl From<crate::types::lsp::Symbol> for Symbol {
    fn from(value: crate::types::lsp::Symbol) -> Self {
        Self {
            kind: value.kind.into(),
            name: value.name,
        }
    }
}

impl From<SymbolKind> for lsp::SymbolKind {
    fn from(value: SymbolKind) -> Self {
        match value {
            SymbolKind::File => lsp::SymbolKind::File,
            SymbolKind::Module => lsp::SymbolKind::Module,
            SymbolKind::Namespace => lsp::SymbolKind::Namespace,
            SymbolKind::Package => lsp::SymbolKind::Package,
            SymbolKind::Class => lsp::SymbolKind::Class,
            SymbolKind::Method => lsp::SymbolKind::Method,
            SymbolKind::Property => lsp::SymbolKind::Property,
            SymbolKind::Field => lsp::SymbolKind::Field,
            SymbolKind::Constructor => lsp::SymbolKind::Constructor,
            SymbolKind::Enum => lsp::SymbolKind::Enum,
            SymbolKind::Interface => lsp::SymbolKind::Interface,
            SymbolKind::Function => lsp::SymbolKind::Function,
            SymbolKind::Variable => lsp::SymbolKind::Variable,
            SymbolKind::Constant => lsp::SymbolKind::Constant,
            SymbolKind::String => lsp::SymbolKind::String,
            SymbolKind::Number => lsp::SymbolKind::Number,
            SymbolKind::Boolean => lsp::SymbolKind::Boolean,
            SymbolKind::Array => lsp::SymbolKind::Array,
            SymbolKind::Object => lsp::SymbolKind::Object,
            SymbolKind::Key => lsp::SymbolKind::Key,
            SymbolKind::Null => lsp::SymbolKind::Null,
            SymbolKind::EnumMember => lsp::SymbolKind::EnumMember,
            SymbolKind::Struct => lsp::SymbolKind::Struct,
            SymbolKind::Event => lsp::SymbolKind::Event,
            SymbolKind::Operator => lsp::SymbolKind::Operator,
            SymbolKind::TypeParameter => lsp::SymbolKind::TypeParameter,
            SymbolKind::Other(value) => lsp::SymbolKind::Other(value),
        }
    }
}
