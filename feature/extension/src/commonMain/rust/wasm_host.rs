#![allow(dead_code)]

pub(crate) mod wit;

use crate::extension::WorktreeDelegate;
use crate::types::lsp::Symbol;
use crate::types::{CodeLabel, Command};
use crate::{
    WasmRuntimeError,
    capability_granter::CapabilityGranter,
    extension::{ExtensionImports, Manifest, WasmExtensionHost},
    paths::{SanitizedPath, normalize_path},
    types::lsp::Completion,
    wasm_host::wit::Extension,
};
use anyhow::{Context as _, Result, anyhow, bail};
use futures::{
    FutureExt, StreamExt,
    channel::{
        mpsc::{self, UnboundedSender},
        oneshot,
    },
};
use futures_util::future::BoxFuture;
use moka::sync::Cache;
use once_cell::sync::Lazy;
use semver::Version;
use std::{
    borrow::Cow,
    fs,
    path::{Path, PathBuf},
    sync::{
        Arc, LazyLock, OnceLock,
        atomic::{AtomicBool, Ordering},
    },
    time::Duration,
};
use tokio::{
    runtime::{Builder, Runtime},
    sync::Mutex,
    task::JoinHandle,
    time,
};
use wasmtime::{
    CacheStore, Config, Engine, Store,
    component::{Component, ResourceTable},
};
use wasmtime_wasi::{
    DirPerms, FilePerms,
    p2::{self as wasi, IoView as _},
};

static RUNTIME: Lazy<Runtime> = Lazy::new(|| {
    Builder::new_current_thread()
        .enable_all()
        .build()
        .expect("failed to build tokio runtime")
});

std::thread_local! {
    /// Used by the crash handler to ignore panics in extension-related threads.
    pub static IS_WASM_THREAD: AtomicBool = const { AtomicBool::new(false) };
}

type ExtensionCall = Box<
    dyn Send + for<'a> FnOnce(&'a mut Extension, &'a mut Store<WasmState>) -> BoxFuture<'a, ()>,
>;

#[derive(uniffi::Object)]
pub struct WasmHost {
    engine: Engine,
    work_dir: PathBuf,
    extension_host: Arc<WasmExtensionHost>,
    imports: Arc<dyn ExtensionImports>,
}

#[derive(uniffi::Object)]
struct WasmStore {
    internal: Arc<Mutex<Store<WasmState>>>,
}

#[uniffi::export]
impl WasmStore {}

impl WasmStore {
    async fn with_store<R>(&self, f: impl FnOnce(&mut Store<WasmState>) -> R) -> R {
        let mut store = self.internal.lock().await;
        f(&mut store)
    }
}

pub struct WasmState {
    pub table: ResourceTable,
    ctx: wasi::WasiCtx,
    pub host: Arc<WasmHost>,
    pub manifest: Arc<Manifest>,
    pub(crate) capability_granter: Arc<dyn CapabilityGranter>,
}

fn wasm_engine() -> Engine {
    static WASM_ENGINE: OnceLock<Engine> = OnceLock::new();
    WASM_ENGINE
        .get_or_init(|| {
            let mut config = Config::new();
            config.wasm_component_model(true);
            config.async_support(true);
            config
                .enable_incremental_compilation(cache_store())
                .unwrap();
            // Async support introduces the issue that extension execution happens during `Future::poll`,
            // which could block an async thread.
            // https://docs.rs/wasmtime/latest/wasmtime/struct.Config.html#execution-in-poll
            //
            // Epoch interruption is a lightweight mechanism to allow the extensions to yield control
            // back to the executor at regular intervals.
            config.epoch_interruption(true);

            let engine = Engine::new(&config).unwrap();

            // It might be safer to do this on a non-async thread to make sure it makes progress
            // regardless of if extensions are blocking.
            // However, due to our current setup, this isn't a likely occurrence and we'd rather
            // not have a dedicated thread just for this. If it becomes an issue, we can consider
            // creating a separate thread for epoch interruption.
            let engine_ref = engine.weak();
            RUNTIME.spawn(async move {
                // Somewhat arbitrary interval, as it isn't a guaranteed interval.
                // But this is a rough upper bound for how long the extension execution can block on
                // `Future::poll`.
                const EPOCH_INTERVAL: Duration = Duration::from_millis(100);
                let mut interval = time::interval(EPOCH_INTERVAL);

                loop {
                    interval.tick().await;

                    // Exit the loop and thread once the engine is dropped.
                    let Some(engine) = engine_ref.upgrade() else {
                        break;
                    };

                    engine.increment_epoch();
                }
            });
            engine
        })
        .clone()
}

fn cache_store() -> Arc<IncrementalCompilationCache> {
    static CACHE_STORE: LazyLock<Arc<IncrementalCompilationCache>> =
        LazyLock::new(|| Arc::new(IncrementalCompilationCache::new()));
    CACHE_STORE.clone()
}

#[derive(Debug, uniffi::Object)]
pub struct WasmExtensionWrapper {
    extension: Arc<WasmExtension>,
}

impl WasmExtensionWrapper {
    pub async fn call<T, Fn>(&self, f: Fn) -> Result<T>
    where
        T: 'static + Send,
        Fn: 'static
            + Send
            + for<'a> FnOnce(&'a mut Extension, &'a mut Store<WasmState>) -> BoxFuture<'a, T>,
    {
        self.extension.call(f).await
    }
}

#[uniffi::export]
impl WasmExtensionWrapper {
    fn klyx_api_version(&self) -> crate::Version {
        self.extension.klyx_api_version.clone().into()
    }

    fn work_dir(&self) -> String {
        self.extension
            .work_dir
            .clone()
            .to_string_lossy()
            .to_string()
    }

    async fn language_server_command(
        &self,
        language_server_id: String,
        worktree: Arc<dyn WorktreeDelegate>,
    ) -> Result<Command, WasmRuntimeError> {
        self.call(|extension, store| {
            async move {
                let resource = store.data_mut().table().push(worktree)?;
                let command = extension
                    .call_language_server_command(store, &language_server_id, resource)
                    .await?
                    .map_err(|err| store.data().extension_error(err))?;

                Ok(command.into())
            }
            .boxed()
        })
        .await?
    }

    async fn language_server_initialization_options(
        &self,
        language_server_id: String,
        worktree: Arc<dyn WorktreeDelegate>,
    ) -> Result<Option<String>, WasmRuntimeError> {
        self.call(|extension, store| {
            async move {
                let resource = store.data_mut().table().push(worktree)?;
                let options = extension
                    .call_language_server_initialization_options(
                        store,
                        &language_server_id,
                        resource,
                    )
                    .await?
                    .map_err(|err| store.data().extension_error(err))?;
                Ok(options)
            }
            .boxed()
        })
        .await?
    }

    async fn language_server_workspace_configuration(
        &self,
        language_server_id: String,
        worktree: Arc<dyn WorktreeDelegate>,
    ) -> Result<Option<String>, WasmRuntimeError> {
        self.call(|extension, store| {
            async move {
                let resource = store.data_mut().table().push(worktree)?;
                let options = extension
                    .call_language_server_workspace_configuration(
                        store,
                        &language_server_id,
                        resource,
                    )
                    .await?
                    .map_err(|err| store.data().extension_error(err))?;
                Ok(options)
            }
            .boxed()
        })
        .await?
    }

    async fn language_server_additional_initialization_options(
        &self,
        language_server_id: String,
        target_language_server_id: String,
        worktree: Arc<dyn WorktreeDelegate>,
    ) -> Result<Option<String>, WasmRuntimeError> {
        self.call(|extension, store| {
            async move {
                let resource = store.data_mut().table().push(worktree)?;
                let options = extension
                    .call_language_server_additional_initialization_options(
                        store,
                        &language_server_id,
                        &target_language_server_id,
                        resource,
                    )
                    .await?
                    .map_err(|err| store.data().extension_error(err))?;
                Ok(options)
            }
            .boxed()
        })
        .await?
    }

    async fn language_server_additional_workspace_configuration(
        &self,
        language_server_id: String,
        target_language_server_id: String,
        worktree: Arc<dyn WorktreeDelegate>,
    ) -> Result<Option<String>, WasmRuntimeError> {
        self.call(|extension, store| {
            async move {
                let resource = store.data_mut().table().push(worktree)?;
                let options = extension
                    .call_language_server_additional_workspace_configuration(
                        store,
                        &language_server_id,
                        &target_language_server_id,
                        resource,
                    )
                    .await?
                    .map_err(|err| store.data().extension_error(err))?;
                Ok(options)
            }
            .boxed()
        })
        .await?
    }

    async fn labels_for_completions(
        &self,
        language_server_id: String,
        completions: Vec<Completion>,
    ) -> Result<Vec<Option<CodeLabel>>, WasmRuntimeError> {
        self.call(|extension, store| {
            async move {
                let labels = extension
                    .call_labels_for_completions(
                        store,
                        &language_server_id,
                        completions.into_iter().map(Into::into).collect(),
                    )
                    .await?
                    .map_err(|err| store.data().extension_error(err))?;

                Ok(labels
                    .into_iter()
                    .map(|label| label.map(Into::into))
                    .collect())
            }
            .boxed()
        })
        .await?
    }

    async fn labels_for_symbols(
        &self,
        language_server_id: String,
        symbols: Vec<Symbol>,
    ) -> Result<Vec<Option<CodeLabel>>, WasmRuntimeError> {
        self.call(|extension, store| {
            async move {
                let labels = extension
                    .call_labels_for_symbols(
                        store,
                        &language_server_id,
                        symbols.into_iter().map(Into::into).collect(),
                    )
                    .await?
                    .map_err(|err| store.data().extension_error(err))?;

                Ok(labels
                    .into_iter()
                    .map(|label| label.map(Into::into))
                    .collect())
            }
            .boxed()
        })
        .await?
    }
}

#[derive(Clone, Debug)]
pub struct WasmExtension {
    tx: UnboundedSender<ExtensionCall>,
    pub manifest: Arc<Manifest>,
    pub work_dir: Arc<Path>,
    #[allow(unused)]
    pub klyx_api_version: Version,
    _task: Arc<JoinHandle<()>>,
}

impl Drop for WasmExtension {
    fn drop(&mut self) {
        self.tx.close_channel();
    }
}

#[derive(thiserror::Error, Debug, uniffi::Error)]
pub enum ExtensionLoadError {
    #[error("task failed to run: {0}")]
    JoinError(String),

    #[error("{details}")]
    InternalError {
        details: String,
    },
}

impl From<anyhow::Error> for ExtensionLoadError {
    fn from(err: anyhow::Error) -> Self {
        Self::InternalError {
            details: format!("{:?}", err),
        }
    }
}

#[uniffi::export]
impl WasmHost {
    #[uniffi::constructor]
    pub fn new(
        work_dir: String,
        extension_host: Arc<WasmExtensionHost>,
        imports: Arc<dyn ExtensionImports>,
    ) -> Self {
        Self {
            engine: wasm_engine(),
            work_dir: PathBuf::from(work_dir),
            extension_host,
            imports,
        }
    }

    pub async fn load_extension(
        self: Arc<Self>,
        wasm_bytes: Vec<u8>,
        manifest: Manifest,
        capability_granter: Arc<dyn CapabilityGranter>,
    ) -> Result<WasmExtensionWrapper, ExtensionLoadError> {
        let handle = RUNTIME.spawn(Self::load_extension_internal(
            self,
            wasm_bytes,
            Arc::new(manifest),
            capability_granter,
        ));
        let inner = handle
            .await
            .map_err(|e| ExtensionLoadError::JoinError(e.to_string()))?;

        inner.map_err(|e| e.into())
    }

    fn work_dir(&self) -> String {
        self.work_dir.clone().to_string_lossy().to_string()
    }
}

impl WasmHost {
    async fn load_extension_internal(
        self: Arc<Self>,
        wasm_bytes: Vec<u8>,
        manifest: Arc<Manifest>,
        capability_granter: Arc<dyn CapabilityGranter>,
    ) -> Result<WasmExtensionWrapper> {
        let this = self.clone();
        let manifest = manifest.clone();
        let load_extension_future = async move {
            let klyx_api_version = parse_wasm_extension_version(&manifest.id, &wasm_bytes)?;

            let component = Component::from_binary(&this.engine, &wasm_bytes)
                .context("failed to compile wasm component")?;

            let mut store = Store::new(
                &this.engine,
                WasmState {
                    table: ResourceTable::new(),
                    ctx: this.build_wasi_ctx(&manifest.id).await?,
                    host: this.clone(),
                    manifest: manifest.clone(),
                    capability_granter,
                },
            );
            // Store will yield after 1 tick, and get a new deadline of 1 tick after each yield.
            store.set_epoch_deadline(1);
            store.epoch_deadline_async_yield_and_update(1);

            let mut extension =
                Extension::instantiate_async(&mut store, klyx_api_version.clone(), &component)
                    .await?;

            extension
                .call_init_extension(&mut store)
                .await
                .context("failed to initialize wasm extension")?;

            let (tx, mut rx) = mpsc::unbounded::<ExtensionCall>();
            let extension_task = async move {
                IS_WASM_THREAD.with(|v| v.store(true, Ordering::Release));
                while let Some(call) = rx.next().await {
                    call(&mut extension, &mut store).await;
                }
            };

            anyhow::Ok((
                extension_task,
                manifest.clone(),
                this.work_dir.join(&manifest.id).into(),
                tx,
                klyx_api_version,
            ))
        };

        let (extension_task, manifest, work_dir, tx, klyx_api_version) =
            RUNTIME.spawn(load_extension_future).await??;

        // we need to run run the task in a tokio context as wasmtime_wasi may
        // call into tokio, accessing its runtime handle when we trigger the `engine.increment_epoch()` above.
        let task = Arc::new(RUNTIME.spawn(extension_task));

        Ok(WasmExtensionWrapper {
            extension: Arc::new(WasmExtension {
                manifest,
                work_dir,
                tx,
                klyx_api_version,
                _task: task,
            }),
        })
    }

    async fn build_wasi_ctx(&self, extension_id: &str) -> Result<wasi::WasiCtx> {
        let extension_work_dir = self.work_dir.join(extension_id);
        if !fs::exists(&extension_work_dir)? {
            fs::create_dir(&extension_work_dir).context("failed to create extension work dir")?;
        }

        let file_perms = FilePerms::all();
        let dir_perms = DirPerms::all();
        let path = SanitizedPath::new(&extension_work_dir).to_string();
        #[cfg(target_os = "windows")]
        let path = path.replace('\\', "/");

        let mut ctx = wasi::WasiCtxBuilder::new();
        ctx.inherit_stdio()
            .env("PWD", &path)
            .env("RUST_BACKTRACE", "full");

        ctx.preopened_dir(&path, ".", dir_perms, file_perms)?;
        ctx.preopened_dir(&path, &path, dir_perms, file_perms)?;

        Ok(ctx.build())
    }

    pub fn writeable_path_from_extension(&self, id: &str, path: &Path) -> Result<PathBuf> {
        let extension_work_dir = self.work_dir.join(id);
        let path = normalize_path(&extension_work_dir.join(path));
        anyhow::ensure!(
            path.starts_with(&extension_work_dir),
            "cannot write to path {path:?}",
        );
        Ok(path)
    }
}

pub fn parse_wasm_extension_version(extension_id: &str, wasm_bytes: &[u8]) -> Result<Version> {
    let mut version = None;

    for part in wasmparser::Parser::new(0).parse_all(wasm_bytes) {
        if let wasmparser::Payload::CustomSection(s) =
            part.context("error parsing wasm extension")?
            && s.name() == "klyx:api-version"
        {
            version = parse_wasm_extension_version_custom_section(s.data());
            if version.is_none() {
                bail!(
                    "extension {} has invalid klyx:api-version section: {:?}",
                    extension_id,
                    s.data()
                );
            }
        }
    }

    // The reason we wait until we're done parsing all of the Wasm bytes to return the version
    // is to work around a panic that can happen inside of Wasmtime when the bytes are invalid.
    //
    // By parsing the entirety of the Wasm bytes before we return, we're able to detect this problem
    // earlier as an `Err` rather than as a panic.
    version.with_context(|| format!("extension {extension_id} has no klyx:api-version section"))
}

fn parse_wasm_extension_version_custom_section(data: &[u8]) -> Option<Version> {
    if data.len() == 6 {
        Some(Version::new(
            u16::from_be_bytes([data[0], data[1]]) as _,
            u16::from_be_bytes([data[2], data[3]]) as _,
            u16::from_be_bytes([data[4], data[5]]) as _,
        ))
    } else {
        None
    }
}

impl WasmExtension {
    pub async fn call<T, Fn>(&self, f: Fn) -> Result<T>
    where
        T: 'static + Send,
        Fn: 'static
            + Send
            + for<'a> FnOnce(&'a mut Extension, &'a mut Store<WasmState>) -> BoxFuture<'a, T>,
    {
        let (return_tx, return_rx) = oneshot::channel();
        self.tx
            .unbounded_send(Box::new(move |extension, store| {
                async {
                    let result = f(extension, store).await;
                    return_tx.send(result).ok();
                }
                .boxed()
            }))
            .map_err(|_| {
                anyhow!(
                    "wasm extension channel should not be closed yet, extension {} (id {})",
                    self.manifest.name,
                    self.manifest.id,
                )
            })?;
        return_rx.await.with_context(|| {
            format!(
                "wasm extension channel, extension {} (id {})",
                self.manifest.name, self.manifest.id,
            )
        })
    }
}

impl wasi::IoView for WasmState {
    fn table(&mut self) -> &mut ResourceTable {
        &mut self.table
    }
}

impl wasi::WasiView for WasmState {
    fn ctx(&mut self) -> &mut wasi::WasiCtx {
        &mut self.ctx
    }
}

impl WasmState {
    fn work_dir(&self) -> PathBuf {
        self.host.work_dir.join(&self.manifest.id)
    }

    fn extension_error(&self, message: String) -> anyhow::Error {
        anyhow!(
            "from extension \"{}\" version {}: {}",
            self.manifest.name,
            self.manifest.version,
            message
        )
    }
}

/// Wrapper around a mini-moka bounded cache for storing incremental compilation artifacts.
/// Since wasm modules have many similar elements, this can save us a lot of work at the
/// cost of a small memory footprint. However, we don't want this to be unbounded, so we use
/// a LFU/LRU cache to evict less used cache entries.
#[derive(Debug)]
struct IncrementalCompilationCache {
    cache: Cache<Vec<u8>, Vec<u8>>,
}

impl IncrementalCompilationCache {
    fn new() -> Self {
        let cache = Cache::builder()
            // Cap this at 32 MB for now. Our extensions turn into roughly 512kb in the cache,
            // which means we could store 64 completely novel extensions in the cache, but in
            // practice we will more than that, which is more than enough for our use case.
            .max_capacity(32 * 1024 * 1024)
            .weigher(|k: &Vec<u8>, v: &Vec<u8>| (k.len() + v.len()).try_into().unwrap_or(u32::MAX))
            .build();
        Self { cache }
    }
}

impl CacheStore for IncrementalCompilationCache {
    fn get(&self, key: &[u8]) -> Option<Cow<'_, [u8]>> {
        self.cache.get(key).map(|v| v.into())
    }

    fn insert(&self, key: &[u8], value: Vec<u8>) -> bool {
        self.cache.insert(key.to_vec(), value);
        true
    }
}
