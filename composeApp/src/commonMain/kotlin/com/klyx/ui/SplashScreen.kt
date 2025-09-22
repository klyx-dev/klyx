package com.klyx.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.klyx.core.LocalAppSettings
import com.klyx.core.icon.Klyx
import com.klyx.core.icon.KlyxIcons
import com.klyx.extension.ExtensionManager

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSetupFailure: (error: String) -> Unit = {},
    onSetupSuccess: () -> Unit
) {
    val appSettings = LocalAppSettings.current
    var isExtensionsLoaded by rememberSaveable { mutableStateOf(false) }
    val loadingState by ExtensionManager.loadingState.collectAsState()
    val (loaded, total) = loadingState

    LaunchedEffect(isExtensionsLoaded) {
        if (isExtensionsLoaded) {
            onSetupSuccess()
        }
    }

    LaunchedEffect(Unit) {
        if (!isExtensionsLoaded) {
            if (!appSettings.loadExtensionsOnStartup) onSetupSuccess()

            ExtensionManager
                .loadExtensions()
                .onFailure(onSetupFailure)
                .onSuccess { isExtensionsLoaded = true }
        } else {
            onSetupSuccess()
        }
    }

    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                KlyxIcons.Klyx,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (total > 0) "Loading extensions" else "Preparing environment...",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(2.dp))

            if (total > 0) {
                Text(
                    text = "Loaded $loaded of $total",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))

                val progress = if (total > 0) loaded / total.toFloat() else 0f

                if (progress == 0f) {
                    LinearProgressIndicator()
                } else {
                    LinearProgressIndicator(progress = { progress })
                }
            } else {
                LinearProgressIndicator()
            }
        }
    }
}
