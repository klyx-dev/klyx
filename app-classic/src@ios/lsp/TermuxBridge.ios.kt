package com.klyx.lsp

actual fun isTermuxInstalledPlatform(): Boolean = false

actual fun hasRunCommandPermissionPlatform(): Boolean = false

actual fun runCommandInTermuxPlatform(
    command: String,
    arguments: List<String>,
    workDir: String?,
    background: Boolean,
): Boolean = false

actual fun launchLspBridgeInTermuxPlatform(
    command: String,
    args: List<String>,
    connectPort: Int,
    workDir: String?,
): Boolean = false

actual fun openTermuxPlatform(): Boolean = false

actual fun openAppSettingsPlatform(): Boolean = false

actual fun openTermuxOverlaySettingsPlatform(): Boolean = false

actual fun requestRunCommandPermissionPlatform(): Boolean = false

actual fun runShellInTermuxPlatform(shell: String, workDir: String?): Boolean = false

actual fun runVisibleShellInTermuxPlatform(shell: String, workDir: String?): Boolean = false

actual suspend fun probeTermuxBinariesPlatform(
    commands: List<String>,
    timeoutMs: Long,
): Map<String, Boolean> = emptyMap()
