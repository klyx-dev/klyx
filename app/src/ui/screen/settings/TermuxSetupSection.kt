package com.klyx.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.core.utils.clipEntryOfText
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
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.components.KxTextField
import com.klyx.ui.theme.KxColors
import com.klyx.ui.theme.accent
import com.klyx.ui.theme.bodySmall
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.error
import com.klyx.ui.theme.label
import com.klyx.ui.theme.labelSmall
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

val isAndroid = currentOperatingSystem == OperatingSystem.Android

@Composable
fun TermuxSetupSection(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val colors = KxColors.current

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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!installed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.warning.copy(alpha = 0.12f))
                    .border(1.dp, colors.warning)
                    .clickable { uriHandler.openUri("https://f-droid.org/en/packages/com.termux") },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "termux not installed",
                        color = colors.warning.copy(alpha = 0.85f),
                    )
                    KxButton(
                        onClick = { check++ },
                        variant = KxButtonVariant.Ghost,
                        contentPadding = PaddingValues(),
                    ) {
                        Text("re-check", style = Theme[typography][labelSmall])
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "install termux (f-droid or github), then re-check. language servers run inside termux.",
                    style = Theme[typography][bodySmall],
                    color = colors.textMuted.compositeOver(colors.warning),
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                )
            }
        }

        SetupCard(
            title = "1. allow Klyx to run commands",
            subtitle = "This permission lets Klyx start language servers inside Termux.",
        ) {
            StatusLine(
                title = "run-command permission",
                ok = granted,
                onRecheck = { check++ },
            )

            if (!granted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton("grant permission") {
                        if (!TermuxBridge.requestRunCommandPermission()) {
                            val _ = TermuxBridge.openAppSettings()
                            feedback = "system settings opened. allow the Termux permission, then re-check."
                        } else {
                            feedback = "permission request sent. approve it in Termux, then re-check."
                        }
                    }
                    SmallButton("app settings", ghost = true) {
                        val _ = TermuxBridge.openAppSettings()
                    }
                }
                HintText("If no permission prompt appears, open Android settings and check Klyx > Permissions.")
            } else {
                HintText("Permission is ready. Klyx can send commands to Termux.")
            }
        }

        SetupCard(
            title = "2. enable external app access",
            subtitle = "Termux blocks commands from other apps until this setting is enabled.",
        ) {
            HintText("Add this line to ~/.termux/termux.properties:")
            CommandPreview("allow-external-apps=true")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (granted) {
                    SmallButton("enable automatically") {
                        feedback = if (TermuxBridge.runVisibleInTermux(TermuxBridge.allowExternalAppsCommand())) {
                            "command opened in Termux. apply it, then restart Termux."
                        } else {
                            "could not send the command to Termux. add the line manually instead."
                        }
                    }
                }
                SmallButton("open termux", ghost = true) {
                    if (!TermuxBridge.openTermux()) feedback = "Termux could not be opened."
                }
            }

            HintText("After editing the file manually, apply the change with:")
            CommandPreview("termux-reload-settings")
            HintText("Restarting Termux also applies the change.")
        }

        SetupCard(
            title = "3. let Termux open when needed",
            subtitle = "Some Android versions block Termux from opening itself during an install or command launch.",
        ) {
            HintText("On Android 10+, allow Display over other apps for Termux, or tap the Termux notification when it appears.")
            SmallButton("display-over-apps for termux", ghost = true) {
                if (!TermuxBridge.openTermuxOverlaySettings()) {
                    feedback = "could not open the system settings page. open Termux's app settings manually."
                } else {
                    feedback = "opened Termux's display-over-apps settings. enable the permission, then return here."
                }
            }
            HintText("If Android shows a list of apps instead of Termux's settings, select Termux from that list.")
        }

        if (feedback != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .border(1.dp, colors.accent.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "setup update",
                    style = Theme[typography][labelSmall],
                    color = colors.accent,
                )
                Text(
                    text = feedback!!,
                    style = Theme[typography][bodySmall],
                    color = colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SetupCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val muted = Theme[colors][textMuted]
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(muted.copy(alpha = 0.035f), RoundedCornerShape(12.dp))
            .border(1.dp, muted.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = Theme[typography][label],
                color = Theme[colors][textPrimary],
            )
            Text(
                text = subtitle,
                style = Theme[typography][bodySmall],
                color = Theme[colors][textMuted],
            )
        }
        content()
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
                    text = if (ok) "granted" else "needed",
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
    val muted = Theme[colors][textMuted]
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(muted.copy(alpha = 0.055f), RoundedCornerShape(8.dp))
            .border(1.dp, muted.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = command,
            style = Theme[typography][bodySmall],
            color = Theme[colors][textPrimary],
        )

        KxButton(
            onClick = { scope.launch { clipboard.setClipEntry(clipEntryOfText(command)) } },
            variant = KxButtonVariant.Ghost,
            playClickSound = true,
            contentPadding = PaddingValues(),
        ) {
            Text("copy", style = Theme[typography][labelSmall])
        }
    }
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

        SetupCard(
            title = "manage servers",
            subtitle = if (isAndroid) {
                "Add a custom server or refresh installation status."
            } else {
                "Add a custom language-server configuration."
            },
        ) {
            if (shownCommand != null) {
                CommandPreview(shownCommand!!)
                if (isAndroid) {
                    SmallButton("open termux", ghost = true) { val _ = TermuxBridge.openTermux() }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallButton(
                    text = "add custom server",
                    modifier = Modifier.weight(1f),
                ) {
                    editing = null
                    dialogOpen = true
                }
                if (isAndroid) {
                    SmallButton(
                        text = "re-check installed",
                        ghost = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        lsp.refreshServerAvailability()
                        check++
                    }
                }
            }
            HintText(
                if (isAndroid) "installed state is probed in Termux at startup (needs the run-command permission). Install skips what is already there."
                else "custom servers with auto mode launch via Termux on Android"
            )
        }
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
    val palette = KxColors.current
    val shape = RoundedCornerShape(12.dp)
    val statusColor = when (statusOk) {
        true -> palette.success
        false -> palette.error
        null -> palette.textMuted
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.textMuted.copy(alpha = 0.035f), shape)
            .border(1.dp, palette.textMuted.copy(alpha = 0.20f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = Theme[typography][label],
                color = Theme[colors][textPrimary],
                modifier = Modifier.weight(1f),
            )
            if (status != null) {
                Column(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.28f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = status,
                        style = Theme[typography][labelSmall],
                        color = statusColor,
                    )
                }
            }
        }

        Text(
            text = subtitle,
            style = Theme[typography][bodySmall],
            color = Theme[colors][textMuted],
        )

        if (showInstall || showEdit || showDelete) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showInstall) SmallButton("install", onClick = onInstall)
                if (showEdit) SmallButton("edit", ghost = true, onClick = onEdit)
                if (showDelete) SmallButton("delete", ghost = true, onClick = onDelete)
            }
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
                borderColor = if (manualSocket) KxColors.current.accent else KxColors.current.border,
                backgroundColor = if (manualSocket) {
                    KxColors.current.accent.copy(alpha = 0.10f)
                } else {
                    KxColors.current.surface.copy(alpha = 0.45f)
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "manual socket: run socat in Termux",
                        style = Theme[typography][labelSmall],
                        color = if (manualSocket) KxColors.current.accent else KxColors.current.textPrimary,
                    )
                    Text(
                        text = if (manualSocket) "✓" else "○",
                        style = Theme[typography][label],
                        color = if (manualSocket) KxColors.current.accent else KxColors.current.textMuted,
                    )
                }
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

