use std::fmt::Display;

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum CapabilityGrantError {
    Inner(String),
}

impl Display for CapabilityGrantError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            CapabilityGrantError::Inner(err) => {
                write!(f, "{}", err)
            }
        }
    }
}

#[uniffi::export(with_foreign)]
pub trait CapabilityGranter: Send + Sync + 'static {
    fn grant_exec(
        &self,
        desired_command: String,
        desired_args: Vec<String>,
    ) -> Result<(), CapabilityGrantError>;

    fn grant_download_file(&self, desired_url: String) -> Result<(), CapabilityGrantError>;

    fn grant_npm_install_package(&self, package_name: String) -> Result<(), CapabilityGrantError>;
}
