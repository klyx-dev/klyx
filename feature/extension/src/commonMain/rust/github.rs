use crate::ExtensionRuntimeError;
use async_trait::async_trait;

#[derive(Debug, uniffi::Record)]
pub struct GitHubLspBinaryVersion {
    pub name: String,
    pub url: String,
    pub digest: Option<String>,
}

#[derive(Debug, uniffi::Record)]
pub struct GithubRelease {
    pub tag_name: String,
    pub pre_release: bool,
    pub assets: Vec<GithubReleaseAsset>,
    pub tarball_url: String,
    pub zipball_url: String,
}

#[derive(Debug, uniffi::Record)]
pub struct GithubReleaseAsset {
    pub name: String,
    pub browser_download_url: String,
    pub digest: Option<String>,
}

#[uniffi::export(with_foreign)]
#[async_trait]
pub trait WasmExtensionGithubHost: Send + Sync + 'static {
    /// Returns the latest release for the given GitHub repository.
    ///
    /// Takes repo as a string in the form "<owner-name>/<repo-name>", for example: "klyx-dev/klyx".
    async fn latest_github_release(
        &self,
        repo: String,
        require_assets: bool,
        pre_release: bool,
    ) -> Result<GithubRelease, ExtensionRuntimeError>;

    /// Returns the GitHub release with the specified tag name for the given GitHub repository.
    ///
    /// Returns an error if a release with the given tag name does not exist.
    async fn github_release_by_tag_name(
        &self,
        repo: String,
        tag: String,
    ) -> Result<GithubRelease, ExtensionRuntimeError>;
}
