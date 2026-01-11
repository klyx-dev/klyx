package com.klyx.extension

import kotlinx.serialization.Serializable

@Serializable
data class AgentServerManifestEntry(
    /**
     * Display name for the agent (shown in menus).
     */
    val name: String,

    /**
     * Per-target configuration for archive-based installation.
     * The key format is "{os}-{arch}" where:
     * - os: "darwin" (macOS), "linux", "windows", "android"
     * - arch: "aarch64" (arm64), "x86_64"
     *
     * Example:
     * ```toml
     * [agent_servers.myagent.targets.darwin-aarch64]
     * archive = "https://example.com/myagent-darwin-arm64.zip"
     * cmd = "./myagent"
     * args = ["--serve"]
     * sha256 = "abc123..."  # optional
     * ```
     *
     * For Node.js-based agents, you can use "node" as the cmd to automatically
     * use Klyx's managed Node.js runtime instead of relying on the user's PATH:
     * ```toml
     * [agent_servers.nodeagent.targets.darwin-aarch64]
     * archive = "https://example.com/nodeagent.zip"
     * cmd = "node"
     * args = ["index.js", "--port", "3000"]
     * ```
     *
     * Note: All commands are executed with the archive extraction directory as the
     * working directory, so relative paths in args (like "index.js") will resolve
     * relative to the extracted archive contents.
     */
    val targets: Map<String, TargetConfig>,

    /**
     * Environment variables to set when launching the agent server.
     */
    val env: Map<String, String> = emptyMap(),

    /**
     * Optional icon path (relative to extension root, e.g., "ai.svg").
     * Should be a small SVG icon for display in menus.
     */
    val icon: String? = null,
)

/**
 * @property archive URL to download the archive from (e.g., "https://github.com/owner/repo/releases/download/v1.0.0/myagent-darwin-arm64.zip")
 * @property cmd Command to run (e.g., "./myagent" or "./myagent.exe")
 * @property args Command-line arguments to pass to the agent server.
 * @property sha256 Optional SHA-256 hash of the archive for verification.
 *                  If not provided and the URL is a GitHub release, we'll attempt to fetch it from GitHub.
 * @property env Environment variables to set when launching the agent server.
 *               These target-specific env vars will override any env vars set at the agent level.
 */
@Serializable
data class TargetConfig(
    val archive: String,
    val cmd: String,
    val args: List<String> = emptyList(),
    val sha256: String? = null,
    val env: Map<String, String> = emptyMap()
)
