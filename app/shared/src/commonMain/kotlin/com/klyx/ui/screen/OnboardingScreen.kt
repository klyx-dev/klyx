package com.klyx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.runtime.settings.SettingsStore
import com.klyx.ui.components.KxButton
import com.klyx.ui.theme.background
import com.klyx.ui.theme.bodySmall
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.error
import com.klyx.ui.theme.headline
import com.klyx.ui.theme.label
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.typography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun OnboardingScreen(
    storageAccessGranted: Boolean,
    onRequestStorageAccess: () -> Unit,
) {
    val settings: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    fun finish() {
        if (!storageAccessGranted || busy) return
        busy = true
        failure = null
        scope.launch {
            try {
                settings.update { it.copy(onboardingCompleted = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failure = "Could not finish setup: ${error.message ?: "Please try again."}"
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme[colors][background])
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Welcome to Klyx", style = Theme[typography][headline], color = Theme[colors][textPrimary])
            Text(
                "Open a folder, explore your files, and start editing code.",
                style = Theme[typography][bodySmall], color = Theme[colors][textMuted],
            )
            Text("File access", style = Theme[typography][label], color = Theme[colors][textPrimary])
            Text(
                if (storageAccessGranted) "File access is enabled. Open a folder from the home screen to start coding."
                else "Klyx needs all-files access to browse and edit projects using filesystem paths. " +
                        "Language servers also need this access to work with your project files. " +
                        "Enable file access in the Android permission screen, then return to Klyx.",
                style = Theme[typography][bodySmall], color = Theme[colors][textMuted],
            )
            Text(
                "For language servers, you can connect Termux later in Settings.",
                style = Theme[typography][bodySmall], color = Theme[colors][textMuted],
            )
            failure?.let {
                Text(it, style = Theme[typography][bodySmall], color = Theme[colors][error])
            }
            if (!storageAccessGranted) {
                KxButton(
                    onClick = {
                        failure = null
                        try {
                            onRequestStorageAccess()
                        } catch (error: Exception) {
                            failure = "Could not open permission settings: ${error.message}"
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant access")
                }
            } else {
                KxButton(
                    onClick = ::finish,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (busy) "Setting up…" else "Get started")
                }
            }
        }
    }
}
