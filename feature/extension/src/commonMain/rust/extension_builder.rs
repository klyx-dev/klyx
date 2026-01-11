#![allow(dead_code)]

use anyhow::{Context, Result, anyhow, bail};
use std::sync::Arc;
use wasm_encoder::{ComponentSectionId, Encode, RawSection, Section};
use wasmparser::Parser;

use crate::Version;

/// Represents an error that occurred while stripping custom sections from a WebAssembly module.
#[derive(Debug, thiserror::Error, uniffi::Object)]
#[error("{e:?}")]
#[uniffi::export(Debug)]
pub struct StripError {
    e: anyhow::Error,
}

impl From<anyhow::Error> for StripError {
    fn from(e: anyhow::Error) -> Self {
        Self { e }
    }
}

/// This was adapted from:
/// https://github.com/zed-industries/zed/blob/392b6184bfd9368e3db0faec69735d3955b10cbd/crates/extension/src/extension_builder.rs#L483
#[uniffi::export]
fn strip_custom_sections(input: &Vec<u8>) -> Result<Vec<u8>, Arc<StripError>> {
    use wasmparser::Payload::*;
    let strip_custom_section = |name: &str| {
        // Default strip everything but:
        // * the `name` section
        // * any `component-type` sections
        // * the `dylink.0` section
        // * our custom version section
        name != "name"
            && !name.starts_with("component-type:")
            && name != "dylink.0"
            && name != "klyx:api-version"
    };

    let mut output = Vec::new();
    let mut stack = Vec::new();

    for payload in Parser::new(0).parse_all(input) {
        let payload = payload.map_err(|e| Arc::new(StripError::from(anyhow!(e))))?;

        // Track nesting depth, so that we don't mess with inner producer sections:
        match payload {
            Version { encoding, .. } => {
                output.extend_from_slice(match encoding {
                    wasmparser::Encoding::Component => &wasm_encoder::Component::HEADER,
                    wasmparser::Encoding::Module => &wasm_encoder::Module::HEADER,
                });
            }
            ModuleSection { .. } | ComponentSection { .. } => {
                stack.push(std::mem::take(&mut output));
                continue;
            }
            End { .. } => {
                let mut parent = match stack.pop() {
                    Some(c) => c,
                    None => break,
                };
                if output.starts_with(&wasm_encoder::Component::HEADER) {
                    parent.push(ComponentSectionId::Component as u8);
                    output.encode(&mut parent);
                } else {
                    parent.push(ComponentSectionId::CoreModule as u8);
                    output.encode(&mut parent);
                }
                output = parent;
            }
            _ => {}
        }

        if let CustomSection(c) = &payload
            && strip_custom_section(c.name())
        {
            continue;
        }
        if let Some((id, range)) = payload.as_section() {
            RawSection {
                id,
                data: &input[range],
            }
            .append_to(&mut output);
        }
    }

    Ok(output)
}

#[derive(Debug, thiserror::Error, uniffi::Object)]
#[error("{internal:?}")]
#[uniffi::export(Debug)]
pub struct ParseExtensionVersionError {
    internal: anyhow::Error,
}

impl From<anyhow::Error> for ParseExtensionVersionError {
    fn from(error: anyhow::Error) -> Self {
        Self { internal: error }
    }
}

#[uniffi::export]
pub fn parse_wasm_extension_version(
    extension_id: &str,
    wasm_bytes: &[u8],
) -> Result<Version, Arc<ParseExtensionVersionError>> {
    parse_wasm_extension_version_impl(extension_id, wasm_bytes)
        .map_err(|e| Arc::new(e.into()))
        .map(Version)
}

fn parse_wasm_extension_version_impl(extension_id: &str, wasm_bytes: &[u8]) -> Result<String> {
    let mut version = None;

    for part in wasmparser::Parser::new(0).parse_all(wasm_bytes) {
        if let wasmparser::Payload::CustomSection(s) =
            part.context("failed to parse wasm extension")?
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

fn parse_wasm_extension_version_custom_section(data: &[u8]) -> Option<String> {
    if data.len() == 6 {
        Some(format!(
            "{}.{}.{}",
            u16::from_be_bytes([data[0], data[1]]),
            u16::from_be_bytes([data[2], data[3]]),
            u16::from_be_bytes([data[4], data[5]]),
        ))
    } else {
        None
    }
}
