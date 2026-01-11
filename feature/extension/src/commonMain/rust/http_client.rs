use async_trait::async_trait;
use std::{collections::HashMap, fmt::Display, sync::Arc};

#[uniffi::export(with_foreign)]
#[async_trait]
pub trait WasmExtensionHttpClientHost: Send + Sync + 'static {
    /// Performs an HTTP request and returns the response.
    async fn fetch(&self, request: HttpRequest) -> Result<HttpResponse, HttpResponseError>;

    /// Performs an HTTP request and returns a response stream.
    /* async */ fn fetch_stream(
        &self,
        request: HttpRequest,
    ) -> Result<Arc<dyn HttpResponseStream>, HttpResponseError>;
}

/// An HTTP response stream.
#[uniffi::export(with_foreign)]
#[async_trait]
pub trait HttpResponseStream: Send + Sync + 'static {
    /// Retrieves the next chunk of data from the response stream.
    ///
    /// Returns `Ok(None)` if the stream has ended.
    async fn next_chunk(&self) -> Result<Option<Vec<u8>>, HttpResponseError>;
}

/// HTTP methods.
#[derive(Debug, uniffi::Enum)]
pub enum HttpMethod {
    Get,
    Head,
    Post,
    Put,
    Delete,
    Options,
    Patch,
}

/// The policy for dealing with redirects received from the server.
#[derive(Debug, uniffi::Enum)]
pub enum RedirectPolicy {
    /// Redirects from the server will not be followed.
    ///
    /// This is the default behavior.
    NoFollow,
    /// Redirects from the server will be followed up to the specified limit.
    FollowLimit(u32),
    /// All redirects from the server will be followed.
    FollowAll,
}

/// An HTTP request.
#[derive(Debug, uniffi::Record)]
pub struct HttpRequest {
    /// The HTTP method for the request.
    pub method: HttpMethod,
    /// The URL to which the request should be made.
    pub url: String,
    /// The headers for the request.
    pub headers: HashMap<String, String>,
    /// The request body.
    pub body: Option<Vec<u8>>,
    /// The policy to use for redirects.
    pub redirect_policy: RedirectPolicy,
}

/// An HTTP response.
#[derive(Debug, uniffi::Record)]
pub struct HttpResponse {
    /// The response headers.
    pub headers: HashMap<String, String>,
    /// The response body.
    pub body: Vec<u8>,
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum HttpResponseError {
    Inner(String),
}

impl Display for HttpResponseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            HttpResponseError::Inner(err) => {
                write!(f, "{}", err)
            }
        }
    }
}
