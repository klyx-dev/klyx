package com.klyx.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.lsp.EditorLsp
import com.klyx.lsp.LspServerRegistry
import com.klyx.lsp.LspSessionState
import com.klyx.lsp.TermuxBridge
import com.klyx.lsp.defaultPortForLanguage
import com.klyx.lsp.resolveCommandPath
import com.klyx.runtime.OperatingSystem
import com.klyx.runtime.currentOperatingSystem
import com.klyx.runtime.settings.CustomLspServer
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.accent
import com.klyx.ui.bodySmall
import com.klyx.ui.colors
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.components.KxTextField
import com.klyx.ui.error
import com.klyx.ui.label
import com.klyx.ui.labelSmall
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private val isAndroid = currentOperatingSystem == OperatingSystem.Android

@Composable
fun TermuxSetupSection(modifier: Modifier = Modifier) {
    if (!isAndroid) return
    var check by remember { mutableStateOf(0) }
    val installed = remember(check) { TermuxBridge.isTermuxInstalled() }
    val granted = remember(check) { installed && TermuxBridge.hasRunCommandPermission() }
    var feedback by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) check++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusLine(
            title = "termux installed",
            ok = installed,
            onRecheck = { check++ },
        )
        if (!installed) {
            HintText("install Termux (F-Droid or GitHub), then re-check. Language servers run inside Termux.")
            return@Column
        }

        StatusLine(
            title = "run-command permission",
            subtitle = "lets Klyx start servers in Termux",
            ok = granted,
            onRecheck = { check++ },
        )
        if (!granted) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("grant") {
                    if (!TermuxBridge.requestRunCommandPermission()) {
                        val _ = TermuxBridge.openAppSettings()
                        feedback = "opened system settings. allow the Termux permission, then re-check"
                    }
                }
                SmallButton("app settings", ghost = true) {
                    val _ = TermuxBridge.openAppSettings()
                }
            }
            HintText("if the popup never appears, grant it in system settings: Klyx > Permissions.")
        }

        HintText("Termux also blocks outside apps until ~/.termux/termux.properties contains:")
        CommandPreview("allow-external-apps=true")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (granted) {
                SmallButton("enable in termux") {
                    feedback = if (TermuxBridge.runVisibleInTermux(TermuxBridge.allowExternalAppsCommand())) {
                        "opened in Termux. then restart Termux"
                    } else {
                        "could not reach Termux. add the line by hand"
                    }
                }
            }
            SmallButton("open termux", ghost = true) {
                if (!TermuxBridge.openTermux()) feedback = "Termux could not be opened"
            }
        }
        HintText("after editing termux.properties by hand, run: termux-reload-settings (or restart Termux).")
        HintText("on Android 10+ the system can block Termux from opening itself for installs. allow 'Display over other apps' for Termux, or tap its notification when it appears.")
        SmallButton("display-over-apps for termux", ghost = true) {
            if (!TermuxBridge.openTermuxOverlaySettings()) {
                feedback = "could not open system settings"
            }
        }
        HintText("if your device shows the full app list instead of Termux, pick Termux in that list.")

        if (feedback != null) {
            Text(text = feedback!!, style = Theme[typography][bodySmall], color = Theme[colors][accent])
        }
    }
}

@Composable
private fun StatusLine(
    title: String,
    ok: Boolean,
    subtitle: String? = null,
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = Theme[typography][label], color = Theme[colors][textPrimary])
                Text(
                    text = if (ok) "yes" else "no",
                    style = Theme[typography][labelSmall],
                    color = if (ok) Theme[colors][accent] else Theme[colors][error],
                )
            }
            if (subtitle != null) {
                Text(text = subtitle, style = Theme[typography][bodySmall], color = Theme[colors][textMuted])
            }
        }
        SmallButton("re-check", ghost = true, onClick = onRecheck)
    }
}

@Composable
private fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = Theme[typography][bodySmall],
        color = Theme[colors][textMuted],
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CommandPreview(command: String, modifier: Modifier = Modifier) {
    Text(
        text = command,
        style = Theme[typography][bodySmall],
        color = Theme[colors][textPrimary],
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SmallButton(
    text: String,
    ghost: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    KxButton(
        onClick = onClick,
        variant = if (ghost) KxButtonVariant.Ghost else KxButtonVariant.Outlined,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = modifier,
    ) {
        Text(text = text, style = Theme[typography][labelSmall])
    }
}

@Composable
fun LspServersSection(modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()
    val lsp: EditorLsp = koinInject()
    val scope = rememberCoroutineScope()
    val customs by workspace.customLspServers.collectAsState()
    val sessions by lsp.activeSessions.collectAsState()
    val availability by lsp.termuxAvailability.collectAsState()
    var check by remember { mutableStateOf(0) }
    val termuxGranted = remember(check) { !isAndroid || TermuxBridge.hasRunCommandPermission() }
    var shownCommand by remember { mutableStateOf<String?>(null) }
    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomLspServer?>(null) }
    var installSent by remember { mutableStateOf(setOf<String>()) }
    var pathHits by remember { mutableStateOf(mapOf<String, Boolean>()) }

    val listLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(listLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lsp.refreshServerAvailability()
            }
        }
        listLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { listLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun restart(languageId: String) {
        scope.launch {
            val _ = runCatching { lsp.restartSession(languageId) }
            check++
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val servers = remember(customs, check) { LspServerRegistry.all() }
        LaunchedEffect(servers) {
            if (!isAndroid) {
                pathHits = withContext(Dispatchers.IO) {
                    servers.associate { (languageId, command) -> languageId to (resolveCommandPath(command) != command) }
                }
            }
        }
        servers.forEach { server ->
            val custom = customs.firstOrNull { it.languageId == server.languageId }
            val sessionState = sessions[server.languageId]
            val installedKnown = sessionState == LspSessionState.Running ||
                    (!isAndroid && pathHits[server.languageId] == true) ||
                    availability[server.command] == true
            val statusPair: Pair<String?, Boolean?> = when {
                sessionState == LspSessionState.Running -> "running" to true
                sessionState == LspSessionState.Starting -> "starting" to true
                sessionState == LspSessionState.Error -> "failed to start" to false
                !isAndroid && pathHits[server.languageId] == true -> "installed" to true
                !isAndroid && pathHits[server.languageId] == false -> "not found" to false
                availability[server.command] == true -> "installed" to true
                availability[server.command] == false -> "not installed" to false
                installSent.contains(server.command) -> "install sent" to true
                isAndroid -> "unknown" to null
                else -> null to null
            }
            ServerRow(
                title = server.languageId + if (custom != null) " (custom)" else "",
                subtitle = server.command +
                        (if (server.args.isNotEmpty()) " ${server.args.joinToString(" ")}" else "") +
                        (if (server.socketPort > 0) "  :${server.socketPort}" else ""),
                status = statusPair.first,
                statusOk = statusPair.second,
                showInstall = isAndroid && !installedKnown &&
                        TermuxBridge.installCommandFor(server.command) != null,
                showEdit = custom != null,
                showDelete = custom != null,
                onInstall = {
                    val cmd = TermuxBridge.installCommandFor(server.command) ?: return@ServerRow
                    shownCommand = cmd
                    if (termuxGranted && TermuxBridge.runVisibleInTermux(cmd)) {
                        shownCommand = "$cmd\n\n(opened in Termux. if it doesn't open, tap its notification)"
                        installSent = installSent + server.command
                    }
                },
                onEdit = { editing = custom; dialogOpen = true },
                onDelete = {
                    workspace.removeCustomLspServer(server.languageId)
                    restart(server.languageId)
                },
            )
        }

        if (shownCommand != null) {
            CommandPreview(shownCommand!!)
            if (isAndroid) {
                SmallButton("open termux", ghost = true) { val _ = TermuxBridge.openTermux() }
            }
        }

        SmallButton("add custom server") {
            editing = null
            dialogOpen = true
        }
        if (isAndroid) {
            SmallButton("re-check installed", ghost = true) {
                lsp.refreshServerAvailability()
                check++
            }
        }
        HintText(
            if (isAndroid) "installed state is probed in Termux at startup (needs the run-command permission). Install skips what is already there."
            else "custom servers with auto mode launch via Termux on Android"
        )
    }

    if (dialogOpen) {
        CustomServerDialog(
            initial = editing,
            onDismiss = { dialogOpen = false },
            onSave = { server ->
                workspace.addCustomLspServer(server)
                dialogOpen = false
                restart(server.languageId)
            },
        )
    }
}

@Composable
private fun ServerRow(
    title: String,
    subtitle: String,
    status: String?,
    statusOk: Boolean?,
    showInstall: Boolean,
    showEdit: Boolean,
    showDelete: Boolean,
    onInstall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = Theme[typography][label], color = Theme[colors][textPrimary])
                if (status != null) {
                    Text(
                        text = status,
                        style = Theme[typography][labelSmall],
                        color = when (statusOk) {
                            true -> Theme[colors][accent]
                            false -> Theme[colors][error]
                            null -> Theme[colors][textMuted]
                        },
                    )
                }
            }
            Text(text = subtitle, style = Theme[typography][bodySmall], color = Theme[colors][textMuted])
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showInstall) SmallButton("install", onClick = onInstall)
            if (showEdit) SmallButton("edit", ghost = true, onClick = onEdit)
            if (showDelete) SmallButton("delete", ghost = true, onClick = onDelete)
        }
    }
}

@Composable
private fun CustomServerDialog(
    initial: CustomLspServer?,
    onDismiss: () -> Unit,
    onSave: (CustomLspServer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val langId = remember(initial) { TextFieldState(initial?.languageId ?: "") }
    val extensions = remember(initial) { TextFieldState(initial?.extensions?.joinToString(", ") ?: "") }
    val command = remember(initial) { TextFieldState(initial?.command ?: "") }
    val args = remember(initial) { TextFieldState(initial?.args?.joinToString(" ") ?: "") }
    val port = remember(initial) {
        TextFieldState(if (initial != null && initial.port > 0) initial.port.toString() else "")
    }
    var manualSocket by remember(initial) { mutableStateOf(initial?.manualSocket ?: false) }
    var formError by remember(initial) { mutableStateOf<String?>(null) }

    KxDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = if (initial == null) "Add server" else "Edit server",
        subtitle = "auto launches the server; manual connects to a socat listener you run in Termux",
        modifier = modifier,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxButton(onClick = onDismiss, variant = KxButtonVariant.Ghost) {
                    Text(text = "cancel", style = Theme[typography][label])
                }
                KxButton(
                    onClick = {
                        val id = langId.text.toString().trim().lowercase()
                        val cmd = command.text.toString().trim()
                        val portValue = port.text.toString().trim().let {
                            if (it.isEmpty()) 0 else it.toIntOrNull()
                        }
                        formError = when {
                            id.isEmpty() -> "language id is empty"
                            cmd.isEmpty() -> "command is empty"
                            portValue == null || portValue < 0 || portValue > 65535 ->
                                "port must be empty or 1-65535"

                            else -> null
                        }
                        if (formError != null) return@KxButton
                        onSave(
                            CustomLspServer(
                                languageId = id,
                                extensions = extensions.text.toString()
                                    .split(',', ' ', '\n').map { it.trim().lowercase().removePrefix(".") }
                                    .filter { it.isNotEmpty() }.distinct(),
                                command = cmd,
                                args = args.text.toString()
                                    .split(' ', '\n', '\t').map { it.trim() }
                                    .filter { it.isNotEmpty() },
                                port = portValue ?: 0,
                                manualSocket = manualSocket,
                            )
                        )
                    },
                    variant = KxButtonVariant.Filled,
                ) {
                    Text(text = "save", style = Theme[typography][label])
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KxTextField(state = langId, label = "language id", placeholder = "dart")
            KxTextField(state = extensions, label = "file extensions", placeholder = "dart")
            KxTextField(state = command, label = "command", placeholder = "dart language-server")
            KxTextField(state = args, label = "args (space separated)", placeholder = "--stdio")
            KxTextField(state = port, label = "port (empty = auto)", placeholder = "9257")
            KxButton(
                onClick = { manualSocket = !manualSocket },
                variant = KxButtonVariant.Outlined,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = (if (manualSocket) "[x] " else "[ ] ") + "manual socket: I run socat in Termux",
                    style = Theme[typography][labelSmall],
                )
            }
            if (manualSocket) {
                val previewPort = port.text.toString().trim().toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: defaultPortForLanguage(langId.text.toString().ifBlank { "custom" })
                CommandPreview(
                    TermuxBridge.generateManualCommand(
                        command.text.toString().trim().ifBlank { "<command>" },
                        args.text.toString().split(' ', '\n', '\t').map { it.trim() }
                            .filter { it.isNotEmpty() },
                        previewPort,
                    )
                )
                HintText("run that in Termux first, Klyx then connects to 127.0.0.1:$previewPort")
            }
            if (formError != null) {
                Text(text = formError!!, style = Theme[typography][bodySmall], color = Theme[colors][error])
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
