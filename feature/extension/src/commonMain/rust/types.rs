pub(crate) mod lsp;

use std::collections::HashMap;

/// A list of environment variables.
pub type EnvVars = HashMap<String, String>;

/// A command.
#[derive(uniffi::Record)]
pub struct Command {
    /// The command to execute.
    pub command: String,
    /// The arguments to pass to the command.
    pub args: Vec<String>,
    /// The environment variables to set for the command.
    pub env: EnvVars,
}

impl std::fmt::Debug for Command {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let filtered_env = self
            .env
            .iter()
            .map(|(k, v)| (k, if should_redact(k) { "[REDACTED]" } else { v }))
            .collect::<Vec<_>>();

        f.debug_struct("Command")
            .field("command", &self.command)
            .field("args", &self.args)
            .field("env", &filtered_env)
            .finish()
    }
}

/// A label containing some code.
#[derive(Debug, Clone, uniffi::Record)]
pub struct CodeLabel {
    /// The source code to parse with Tree-sitter.
    pub code: String,
    /// The spans to display in the label.
    pub spans: Vec<CodeLabelSpan>,
    /// The range of the displayed label to include when filtering.
    pub filter_range: UIntSpan,
}

/// A span within a code label.
#[derive(uniffi::Enum, Debug, Clone)]
pub enum CodeLabelSpan {
    /// A range into the parsed code.
    CodeRange(UIntSpan),
    /// A span containing a code literal.
    Literal(CodeLabelSpanLiteral),
}

#[derive(Debug, Default, uniffi::Record, Clone)]
pub struct UIntSpan {
    #[uniffi(default = 0)]
    pub start: u32,
    #[uniffi(default = 0)]
    pub end: u32,
}

/// A span containing a code literal.
#[derive(Debug, Clone, uniffi::Record)]
pub struct CodeLabelSpanLiteral {
    /// The literal text.
    pub text: String,
    /// The name of the highlight to use for this literal.
    pub highlight_name: Option<String>,
}

/// Whether a given environment variable name should have its value redacted
pub fn should_redact(env_var_name: &str) -> bool {
    const REDACTED_SUFFIXES: &[&str] = &[
        "KEY",
        "TOKEN",
        "PASSWORD",
        "SECRET",
        "PASS",
        "CREDENTIALS",
        "LICENSE",
    ];
    REDACTED_SUFFIXES
        .iter()
        .any(|suffix| env_var_name.ends_with(suffix))
}
