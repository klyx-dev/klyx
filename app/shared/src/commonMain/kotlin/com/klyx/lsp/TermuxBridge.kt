package com.klyx.lsp

expect fun isTermuxInstalledPlatform(): Boolean

expect fun hasRunCommandPermissionPlatform(): Boolean

expect fun runCommandInTermuxPlatform(
    command: String,
    arguments: List<String>,
    workDir: String?,
    background: Boolean,
): Boolean

expect fun launchLspBridgeInTermuxPlatform(
    command: String,
    args: List<String>,
    connectPort: Int,
    workDir: String?,
): Boolean

expect fun openTermuxPlatform(): Boolean

expect fun openAppSettingsPlatform(): Boolean

expect fun requestRunCommandPermissionPlatform(): Boolean

expect fun openTermuxOverlaySettingsPlatform(): Boolean

expect fun runShellInTermuxPlatform(shell: String, workDir: String?): Boolean

expect fun runVisibleShellInTermuxPlatform(shell: String, workDir: String?): Boolean

expect suspend fun probeTermuxBinariesPlatform(commands: List<String>, timeoutMs: Long): Map<String, Boolean>

object TermuxBridge {

    fun isTermuxInstalled(): Boolean = isTermuxInstalledPlatform()


    fun hasRunCommandPermission(): Boolean = hasRunCommandPermissionPlatform()


    fun runCommand(
        command: String,
        arguments: List<String> = emptyList(),
        workDir: String? = null,
        background: Boolean = true,
    ): Boolean = runCommandInTermuxPlatform(command, arguments, workDir, background)


    fun launchLspBridge(
        command: String,
        args: List<String> = emptyList(),
        connectPort: Int,
        workDir: String? = null,
    ): Boolean = launchLspBridgeInTermuxPlatform(command, args, connectPort, workDir)


    fun generateManualCommand(
        command: String,
        args: List<String> = emptyList(),
        port: Int = 9257,
    ): String {
        val fullCmd = if (args.isEmpty()) command else "$command ${args.joinToString(" ")}"
        return "socat TCP-LISTEN:$port,reuseaddr,fork EXEC:\"$fullCmd\""
    }

    fun openTermux(): Boolean = openTermuxPlatform()

    fun openAppSettings(): Boolean = openAppSettingsPlatform()

    fun requestRunCommandPermission(): Boolean = requestRunCommandPermissionPlatform()

    fun openTermuxOverlaySettings(): Boolean = openTermuxOverlaySettingsPlatform()

    fun installCommandFor(command: String): String? {
        val serverInstall = when (command) {
            "rust-analyzer" -> "pkg install -y rust-analyzer"
            "clangd" -> "pkg install -y clangd"
            "gopls" -> $$"pkg install -y golang && export PATH=$PATH:$HOME/go/bin && go install golang.org/x/tools/gopls@latest"
            "pyright-langserver" -> "pkg install -y nodejs && npm i -g pyright"
            "vtsls" -> "pkg install -y nodejs && npm i -g @vtsls/language-server"
            "vscode-json-language-server" -> "pkg install -y nodejs && npm i -g vscode-langservers-extracted"
            "lua-language-server" -> "pkg install -y lua-language-server"
            "bash-language-server" -> "pkg install -y nodejs && npm i -g bash-language-server"
            "yaml-language-server" -> "pkg install -y nodejs && npm i -g yaml-language-server"
            else -> return null
        }
        val presentCheck = when (command) {
            "gopls" -> $$"[ -x $HOME/go/bin/gopls ] || command -v gopls"
            else -> "command -v $command"
        }
        return "$presentCheck >/dev/null 2>&1 || { $serverInstall; }; command -v socat >/dev/null 2>&1 || pkg install -y socat"
    }


    fun allowExternalAppsCommand(): String =
        "mkdir -p ~/.termux && grep -q '^allow-external-apps' ~/.termux/termux.properties 2>/dev/null || echo 'allow-external-apps=true' >> ~/.termux/termux.properties"

    fun runShellInTermux(shell: String, workDir: String? = null): Boolean =
        runShellInTermuxPlatform(shell, workDir)

    fun runVisibleInTermux(shell: String, workDir: String? = null): Boolean =
        runVisibleShellInTermuxPlatform(shell, workDir)

    suspend fun probeTermuxBinaries(
        commands: List<String>,
        timeoutMs: Long = 10_000L,
    ): Map<String, Boolean> =
        if (commands.isEmpty()) emptyMap()
        else probeTermuxBinariesPlatform(commands.distinct(), timeoutMs)
}
