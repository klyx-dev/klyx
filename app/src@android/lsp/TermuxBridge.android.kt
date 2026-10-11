package com.klyx.lsp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.klyx.core.App
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TERMUX_PACKAGE = "com.termux"
private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
private const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"

private const val EXTRA_RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
private const val EXTRA_RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
private const val EXTRA_RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
private const val EXTRA_RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
private const val EXTRA_RUN_COMMAND_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

@SuppressLint("SdCardPath")
private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"

@SuppressLint("SdCardPath")
private const val TERMUX_HOME = "/data/data/com.termux/files/home"

private fun getAndroidContext(): Context = App.global.get()

actual fun isTermuxInstalledPlatform(): Boolean {
    val context = getAndroidContext()
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        }
        true
    }.getOrDefault(false)
}

actual fun hasRunCommandPermissionPlatform(): Boolean {
    val context = getAndroidContext()
    return context.checkCallingOrSelfPermission(RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED
}

actual fun runCommandInTermuxPlatform(
    command: String,
    arguments: List<String>,
    workDir: String?,
    background: Boolean,
): Boolean {
    val context = getAndroidContext()
    return runCatching {
        val intent = Intent().apply {
            setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
            action = RUN_COMMAND_ACTION
            putExtra(EXTRA_RUN_COMMAND_PATH, command)
            if (arguments.isNotEmpty()) {
                putExtra(EXTRA_RUN_COMMAND_ARGUMENTS, arguments.toTypedArray())
            }
            if (workDir != null) {
                putExtra(EXTRA_RUN_COMMAND_WORKDIR, workDir)
            }
            putExtra(EXTRA_RUN_COMMAND_BACKGROUND, background)
            putExtra(EXTRA_RUN_COMMAND_SESSION_ACTION, "0")
        }

        try {
            context.startService(intent)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                throw e
            }
        }
        true
    }.getOrElse { error ->
        println("[LSP Termux ERROR] Failed to send RUN_COMMAND intent: ${error.message}")
        false
    }
}

actual fun launchLspBridgeInTermuxPlatform(
    command: String,
    args: List<String>,
    connectPort: Int,
    workDir: String?,
): Boolean {
    val fullCmd = if (args.isEmpty()) command else "$command ${args.joinToString(" ")}"

    val script = $$"""
        LOG="$HOME/.klyx-bridge.log"
        BBIN="$(dirname "$$TERMUX_BASH")"
        export PATH="$BBIN:$PATH"
        RUNNER="$HOME/.klyx-lsp-run.$$.sh"
        cat > "$RUNNER" <<RUNNEREOF
        #!$BBIN/bash
        export PATH="$BBIN:$PATH"
        TEXEC="$(dirname "$BBIN")/lib/libtermux-exec.so"
        [ -f "\$TEXEC" ] && export LD_PRELOAD="\$TEXEC"
        LOG="$HOME/.klyx-bridge.log"
        DLOG="/data/local/tmp/klyx-bridge.log"
        note() { echo "\$*" >> "\$LOG" 2>/dev/null; /system/bin/log -p i -t klyx-bridge "\$*" 2>/dev/null || true; }
        set -- $$fullCmd
        note "runner argc=\$# cmd1=\$1"
        t=""
        saveIFS="\$IFS"; IFS=:
        for d in \$PATH; do
          IFS="\$saveIFS"
          [ -n "\$d" ] || continue
          if [ -x "\$d/\$1" ] && [ ! -d "\$d/\$1" ]; then t="\$d/\$1"; break; fi
        done
        IFS="\$saveIFS"
        [ -n "\$t" ] || { note "not found: \$1"; exit 127; }
        note "resolved [\$1] -> [\$t]"
        shift
        "\$t" "\$@" 2>/dev/null || { rc=\$?; note "direct failed rc=\$rc, trying bash"; bash "\$t" "\$@" 2>/dev/null || { rc=\$?; note "bash failed rc=\$rc, trying node"; exec node "\$t" "\$@"; }; }
        RUNNEREOF
        chmod 700 "$RUNNER"
        [ -s "$RUNNER" ] || { echo "[$(date +%T)] runner script missing, aborting" >> "$LOG" 2>&1; exit 1; }
        find "$HOME" -maxdepth 1 -name '.klyx-lsp-run.*.sh' -mmin +1440 -delete 2>/dev/null
        {
        echo "[$(date +%T)] bridge port $$connectPort: $$fullCmd"
        echo "[$(date +%T)] env PATH=$PATH HOME=$HOME LD_PRELOAD=${LD_PRELOAD:-<unset>}"
        if command -v socat >/dev/null 2>&1; then
            echo "[$(date +%T)] using $(command -v socat)"
            exec socat TCP:127.0.0.1:$$connectPort EXEC:"$RUNNER"
        elif command -v ncat >/dev/null 2>&1; then
            echo "[$(date +%T)] using $(command -v ncat)"
            exec ncat 127.0.0.1 $$connectPort -c "$$fullCmd"
        elif command -v python3 >/dev/null 2>&1; then
            echo "[$(date +%T)] using $(command -v python3)"
            exec python3 -c "import shutil,socket,subprocess,threading; sh=shutil.which('bash') or 'bash'; s=socket.socket(); s.connect(('127.0.0.1',$$connectPort)); p=subprocess.Popen([sh,'-c','exec $$fullCmd'],stdin=subprocess.PIPE,stdout=subprocess.PIPE); threading.Thread(target=lambda: [p.stdin.write(b) or p.stdin.flush() for b in iter(lambda: s.recv(4096), b'')],daemon=True).start(); [s.sendall(b) for b in iter(lambda: p.stdout.read(4096), b'')]"
        else
            echo "[$(date +%T)] NO BRIDGE TOOL (need socat, ncat or python3)"
            echo "Termux LSP: No bridge (socat, ncat, python3) found. Run 'pkg install socat' in Termux." >&2
        fi
        } >> "$LOG" 2>&1
    """.trimIndent()

    return runCommandInTermuxPlatform(
        command = TERMUX_BASH,
        arguments = listOf("-c", script),
        workDir = workDir ?: TERMUX_HOME,
        background = true,
    )
}

actual fun openTermuxPlatform(): Boolean {
    val context = getAndroidContext()
    return runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

@SuppressLint("UseKtx")
actual fun openAppSettingsPlatform(): Boolean {
    val context = getAndroidContext()
    return runCatching {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

@SuppressLint("UseKtx")
actual fun openTermuxOverlaySettingsPlatform(): Boolean {
    val context = getAndroidContext()
    return runCatching {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$TERMUX_PACKAGE"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

private const val RUN_COMMAND_REQUEST_CODE = 1001

actual fun requestRunCommandPermissionPlatform(): Boolean {
    val activity: Activity = App.global.getOrNull() ?: return openAppSettingsPlatform()
    return runCatching {
        activity.requestPermissions(arrayOf(RUN_COMMAND_PERMISSION), RUN_COMMAND_REQUEST_CODE)
        true
    }.getOrDefault(false)
}

actual fun runShellInTermuxPlatform(shell: String, workDir: String?): Boolean =
    runCommandInTermuxPlatform(
        command = TERMUX_BASH,
        arguments = listOf("-c", shell),
        workDir = workDir ?: TERMUX_HOME,
        background = true,
    )

actual fun runVisibleShellInTermuxPlatform(shell: String, workDir: String?): Boolean =
    runCommandInTermuxPlatform(
        command = TERMUX_BASH,
        arguments = listOf("-c", shell),
        workDir = workDir ?: TERMUX_HOME,
        background = false,
    )

actual suspend fun probeTermuxBinariesPlatform(
    commands: List<String>,
    timeoutMs: Long,
): Map<String, Boolean> = withContext(Dispatchers.IO) {
    if (commands.isEmpty()) return@withContext emptyMap()
    if (!isTermuxInstalledPlatform() || !hasRunCommandPermissionPlatform()) return@withContext emptyMap()

    val server = runCatching {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
    }.getOrNull() ?: return@withContext emptyMap()
    try {
        server.soTimeout = timeoutMs.toInt().coerceIn(1000, 60_000)
        val port = server.localPort
        val names = commands.distinct()
        val checks = names.joinToString("; ") { name ->
            val safe = name.replace("'", "'\\''")
            "command -v '$safe' >/dev/null 2>&1 && echo '$safe=1' || echo '$safe=0'"
        }
        val dispatched = runCommandInTermuxPlatform(
            command = TERMUX_BASH,
            arguments = listOf("-c", "{ $checks; } > /dev/tcp/127.0.0.1/$port"),
            workDir = TERMUX_HOME,
            background = true,
        )
        if (!dispatched) return@withContext emptyMap()

        val result = mutableMapOf<String, Boolean>()
        try {
            val conn = server.accept()
            try {
                conn.soTimeout = 5_000
                conn.getInputStream().bufferedReader().forEachLine { line ->
                    val name = line.substringBefore("=").trim()
                    val value = line.substringAfter("=", "").trim()
                    if (name in names && (value == "0" || value == "1")) {
                        result[name] = value == "1"
                    }
                }
            } finally {
                val _ = runCatching { conn.close() }
            }
        } catch (_: SocketTimeoutException) {
            // Termux denied or never answered. keep whatever arrived (likely nothing).
        }
        result.toMap()
    } finally {
        val _ = runCatching { server.close() }
    }
}
