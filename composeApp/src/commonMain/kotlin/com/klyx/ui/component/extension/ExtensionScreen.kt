package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.klyx.appPrefs
import com.klyx.core.extension.ExtensionInfo
import com.klyx.core.ui.animatedComposable
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton
import com.klyx.navTypeMap
import com.klyx.res.Res.string
import com.klyx.res.extension_disclaimer
import com.klyx.ui.component.extension.ExtensionRoutes.ExtensionDetail
import com.klyx.ui.component.extension.ExtensionRoutes.ExtensionList
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.typeOf

@Composable
fun ExtensionScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    val onNavigateBack: () -> Unit = {
        with(navController) {
            if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                popBackStack()
            }
        }
    }

    NavHost(
        navController = navController,
        modifier = modifier.padding(horizontal = 8.dp),
        startDestination = ExtensionList
    ) {
        animatedComposable<ExtensionList> {
            ExtensionListScreen(
                modifier = Modifier.fillMaxSize(),
                onExtensionItemClick = { extension ->
                    navController.navigate(route = ExtensionDetail(extension)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        animatedComposable<ExtensionDetail>(typeMap = navTypeMap(typeOf<ExtensionInfo>())) { backStackEntry ->
            val detail: ExtensionDetail = backStackEntry.toRoute()

            ExtensionDetailScreen(
                modifier = Modifier.fillMaxSize(),
                extensionInfo = detail.extensionInfo,
                onNavigateBack = onNavigateBack
            )
        }
    }

    ExtensionDisclaimer()
}

@Composable
private fun ExtensionDisclaimer() {
    var showExtensionDisclaimer by remember {
        mutableStateOf(appPrefs.getBoolean("show_extension_disclaimer", true))
    }

    if (showExtensionDisclaimer) {
        AlertDialog(
            onDismissRequest = { showExtensionDisclaimer = false },
            confirmButton = { ConfirmButton("OK") { showExtensionDisclaimer = false } },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("Info", textAlign = TextAlign.Center) },
            text = { Text(stringResource(string.extension_disclaimer)) },
            dismissButton = {
                DismissButton("Don't show again") {
                    showExtensionDisclaimer = false
                    appPrefs.putBoolean("show_extension_disclaimer", false)
                }
            }
        )
    }
}

