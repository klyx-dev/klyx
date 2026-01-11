pub mod since_v1_3_6;

use crate::{
    extension::WorktreeDelegate,
    wasm_host::{
        WasmState, wasm_engine,
        wit::since_v1_3_6::{CodeLabel, Command},
    },
};
use anyhow::{Context as _, Result, bail};
use semver::Version;
use since_v1_3_6 as latest;
use std::sync::Arc;
use wasmtime::{
    Store,
    component::{Component, Linker, Resource},
};

pub fn new_linker(
    f: impl Fn(&mut Linker<WasmState>, fn(&mut WasmState) -> &mut WasmState) -> Result<()>,
) -> Linker<WasmState> {
    let mut linker = Linker::new(&wasm_engine());
    wasmtime_wasi::p2::add_to_linker_async(&mut linker).unwrap();
    f(&mut linker, wasi_view).unwrap();
    linker
}

fn wasi_view(state: &mut WasmState) -> &mut WasmState {
    state
}

pub enum Extension {
    V1_3_6(since_v1_3_6::Extension),
}

impl Extension {
    pub async fn instantiate_async(
        store: &mut Store<WasmState>,
        version: Version,
        component: &Component,
    ) -> Result<Self> {
        if version >= latest::MIN_VERSION {
            let extension =
                latest::Extension::instantiate_async(store, component, latest::linker())
                    .await
                    .context("failed to instantiate wasm extension")?;
            Ok(Self::V1_3_6(extension))
        } else {
            bail!(
                "unsupported extension API version {version}; supported versions are >= {}",
                latest::MIN_VERSION
            )
        }
    }

    pub async fn call_init_extension(&self, store: &mut Store<WasmState>) -> Result<()> {
        match self {
            Extension::V1_3_6(ext) => ext.call_init_extension(store).await,
        }
    }

    pub async fn call_language_server_command(
        &self,
        store: &mut Store<WasmState>,
        language_server_id: &str,
        resource: Resource<Arc<dyn WorktreeDelegate>>,
    ) -> Result<Result<Command, String>> {
        match self {
            Extension::V1_3_6(ext) => {
                ext.call_language_server_command(store, language_server_id, resource)
                    .await
            }
        }
    }

    pub async fn call_language_server_initialization_options(
        &self,
        store: &mut Store<WasmState>,
        language_server_id: &str,
        resource: Resource<Arc<dyn WorktreeDelegate>>,
    ) -> Result<Result<Option<String>, String>> {
        match self {
            Extension::V1_3_6(ext) => {
                ext.call_language_server_initialization_options(store, language_server_id, resource)
                    .await
            }
        }
    }

    pub async fn call_language_server_workspace_configuration(
        &self,
        store: &mut Store<WasmState>,
        language_server_id: &str,
        resource: Resource<Arc<dyn WorktreeDelegate>>,
    ) -> Result<Result<Option<String>, String>> {
        match self {
            Extension::V1_3_6(ext) => {
                ext.call_language_server_workspace_configuration(
                    store,
                    language_server_id,
                    resource,
                )
                .await
            }
        }
    }

    pub async fn call_language_server_additional_initialization_options(
        &self,
        store: &mut Store<WasmState>,
        language_server_id: &str,
        target_language_server_id: &str,
        resource: Resource<Arc<dyn WorktreeDelegate>>,
    ) -> Result<Result<Option<String>, String>> {
        match self {
            Extension::V1_3_6(ext) => {
                ext.call_language_server_additional_initialization_options(
                    store,
                    language_server_id,
                    target_language_server_id,
                    resource,
                )
                .await
            }
        }
    }

    pub async fn call_language_server_additional_workspace_configuration(
        &self,
        store: &mut Store<WasmState>,
        language_server_id: &str,
        target_language_server_id: &str,
        resource: Resource<Arc<dyn WorktreeDelegate>>,
    ) -> Result<Result<Option<String>, String>> {
        match self {
            Extension::V1_3_6(ext) => {
                ext.call_language_server_additional_workspace_configuration(
                    store,
                    language_server_id,
                    target_language_server_id,
                    resource,
                )
                .await
            }
        }
    }

    pub async fn call_labels_for_completions(
        &self,
        store: &mut Store<WasmState>,
        language_server_id: &str,
        completions: Vec<latest::Completion>,
    ) -> Result<Result<Vec<Option<CodeLabel>>, String>> {
        match self {
            Extension::V1_3_6(ext) => {
                ext.call_labels_for_completions(store, language_server_id, &completions)
                    .await
            }
        }
    }

    pub async fn call_labels_for_symbols(
        &self,
        store: &mut Store<WasmState>,
        language_server_id: &str,
        symbols: Vec<latest::Symbol>,
    ) -> Result<Result<Vec<Option<CodeLabel>>, String>> {
        match self {
            Extension::V1_3_6(ext) => {
                ext.call_labels_for_symbols(store, language_server_id, &symbols)
                    .await
            }
        }
    }
}

pub trait ToWasmtimeResult<T> {
    fn to_wasmtime_result(self) -> wasmtime::Result<Result<T, String>>;
}

impl<T> ToWasmtimeResult<T> for Result<T> {
    fn to_wasmtime_result(self) -> wasmtime::Result<Result<T, String>> {
        Ok(self.map_err(|error| format!("{error:?}")))
    }
}
