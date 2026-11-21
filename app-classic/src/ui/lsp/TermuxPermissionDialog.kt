package com.klyx.ui.lsp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.klyx.editor.EditorStore
import com.klyx.lsp.EditorLsp
import com.klyx.lsp.TermuxBridge
import com.klyx.lsp.TermuxPermissionConsent
import com.klyx.lsp.ensureSession
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.components.KxSwitch
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.textPrimary
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun TermuxPermissionDialog() {
    val consent: TermuxPermissionConsent = koinInject()
    val lsp: EditorLsp = koinInject()
    val workspace: Workspace = koinInject()
    val editors: EditorStore = koinInject()
    val visible by consent.showPrompt.collectAsState()
    var dontAskAgain by remember(visible) { mutableStateOf(false) }
    var awaitingPermission by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, awaitingPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingPermission && TermuxBridge.hasRunCommandPermission()) {
                awaitingPermission = false
                scope.launch {
                    workspace.documents.value.values.toList().forEach { document ->
                        if (editors.controllersFor(document.id).isNotEmpty()) lsp.ensureSession(document)
                    }
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    fun dismiss() {
        scope.launch { consent.dismiss(dontAskAgain) }
    }

    KxDialog(
        visible = visible,
        onDismissRequest = ::dismiss,
        title = "allow language servers?",
        subtitle = "Klyx needs permission to run commands in Termux for completion, diagnostics, and other language features. You can keep editing without it.",
        actions = {
            KxButton(onClick = ::dismiss, variant = KxButtonVariant.Ghost) { Text("not now") }
            Spacer(Modifier.width(8.dp))
            KxButton(onClick = {
                scope.launch {
                    consent.dismiss(dontAskAgain)
                    awaitingPermission = true
                    if (!TermuxBridge.requestRunCommandPermission()) {
                        val _ = TermuxBridge.openAppSettings()
                    }
                }
            }) { Text("continue") }
        },
    ) {
        Text(
            "Continue to the Android permission prompt or app settings. You can also enable this later in Settings > Termux setup.",
            color = Theme[colors][textPrimary],
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            KxSwitch(
                checked = dontAskAgain,
                onCheckedChange = { dontAskAgain = it },
                accessibilityLabel = "Don't ask again",
            )
            Text("don't ask again", color = Theme[colors][textPrimary])
        }
    }
}
