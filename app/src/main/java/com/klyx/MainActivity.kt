package com.klyx

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.EntryDsl
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.klyx.presentation.components.dialogs.AllFilesAccessDialog
import com.klyx.presentation.components.dialogs.LegacyStorageAccessDialog
import com.klyx.presentation.navigation.Screen
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.presentation.navigation.rememberNavigator
import com.klyx.presentation.navigation.toEntries
import com.klyx.presentation.screen.HomeScreen
import com.klyx.presentation.screen.SettingScreens
import com.klyx.presentation.screen.SettingsScreen
import com.klyx.presentation.screen.settings.About
import com.klyx.presentation.screen.settings.Appearance
import com.klyx.presentation.screen.settings.DeveloperOptions
import com.klyx.presentation.screen.settings.Editor
import com.klyx.presentation.screen.settings.SystemDiagnostics
import com.klyx.ui.ComposeActivity
import com.klyx.ui.animation.LocalReduceMotion
import com.klyx.ui.animation.orSnap
import com.klyx.ui.theme.ProvideGoogleSansTypography

class MainActivity : ComposeActivity() {
    @Composable
    override fun BoxScope.Content() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val reduceMotion = LocalReduceMotion.current

        fun hasLegacyStoragePermissions(): Boolean {
            val read = ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            return read && write
        }

        var hasStoragePermission by remember {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    hasLegacyStoragePermissions()
                }
            )
        }

        val legacyPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
            hasStoragePermission = readGranted && writeGranted
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Environment.isExternalStorageManager()
                    } else {
                        hasLegacyStoragePermissions()
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val navigator = rememberNavigator()
        val entryProvider = remember { appScreenEntryProvider() }

        NavDisplay(
            entries = with(navigator) { entryProvider.toEntries() },
            onBack = navigator::navigateBack,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { transitionSpec(reduceMotion) },
            popTransitionSpec = { popTransitionSpec(reduceMotion) },
            predictivePopTransitionSpec = { popTransitionSpec(reduceMotion) }
        )

        if (!hasStoragePermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AllFilesAccessDialog(
                    onDismiss = { finish() },
                    onConfirm = {
                        val intent =
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = "package:${packageName}".toUri()
                            }
                        startActivity(intent)
                    }
                )
            } else {
                // Android 10 and below
                LegacyStorageAccessDialog(
                    onDismiss = { finish() },
                    onConfirm = {
                        legacyPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                )
            }
        }
    }

    private fun appScreenEntryProvider(): (Screen) -> NavEntry<Screen> = entryProvider(
        fallback = { key ->
            NavEntry(key = key) { screen ->
                UnknownScreen(screen)
            }
        }
    ) {
        entry<Screen.Home> { HomeScreen() }
        entry<Screen.Settings> {
            ProvideGoogleSansTypography { SettingsScreen() }
        }

        settingsEntry<SettingsScreen.Editor> {
            ProvideGoogleSansTypography { SettingScreens.Editor() }
        }
        settingsEntry<SettingsScreen.Appearance> {
            ProvideGoogleSansTypography { SettingScreens.Appearance() }
        }
        settingsEntry<SettingsScreen.DeveloperOptions> {
            ProvideGoogleSansTypography { SettingScreens.DeveloperOptions() }
        }
        settingsEntry<SettingsScreen.SystemDiagnostics> {
            ProvideGoogleSansTypography { SettingScreens.SystemDiagnostics() }
        }
        settingsEntry<SettingsScreen.About> {
            ProvideGoogleSansTypography { SettingScreens.About() }
        }
    }

    @Composable
    private fun UnknownScreen(screen: Screen) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Oops! Something went wrong.\nThis screen isn't available right now.\n\n(${screen::class.qualifiedName})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    @EntryDsl
    private inline fun <reified K : SettingsScreen> EntryProviderScope<Screen>.settingsEntry(
        metadata: Map<String, Any> = emptyMap(),
        noinline content: @Composable (K) -> Unit,
    ) {
        entry(
            //clazzContentKey = ::identity,
            metadata = metadata,
            content = content
        )
    }
}

private fun intOffsetSpec(reduceMotion: Boolean) =
    tween<IntOffset>(350, easing = FastOutSlowInEasing).orSnap(reduceMotion)

private fun floatSpec(reduceMotion: Boolean) =
    tween<Float>(350, easing = FastOutSlowInEasing).orSnap(reduceMotion)

private fun enterTransition(reduceMotion: Boolean) =
    slideInHorizontally(initialOffsetX = { it }, animationSpec = intOffsetSpec(reduceMotion))

private fun exitTransition(reduceMotion: Boolean) = slideOutHorizontally(
    targetOffsetX = { -it / 3 },
    animationSpec = intOffsetSpec(reduceMotion)
) + fadeOut(animationSpec = floatSpec(reduceMotion))

private fun popEnterTransition(reduceMotion: Boolean) = slideInHorizontally(
    initialOffsetX = { -it / 3 },
    animationSpec = intOffsetSpec(reduceMotion)
) + scaleIn(animationSpec = floatSpec(reduceMotion), initialScale = 0.9f)

private fun popExitTransition(reduceMotion: Boolean) =
    slideOutHorizontally(
        animationSpec = intOffsetSpec(reduceMotion),
        targetOffsetX = { it }
    ) + scaleOut(
        animationSpec = floatSpec(reduceMotion),
        targetScale = 0.75f,
        transformOrigin = TransformOrigin(0.5f, 0.5f)
    )

private fun transitionSpec(reduceMotion: Boolean) =
    enterTransition(reduceMotion) togetherWith exitTransition(reduceMotion)

private fun popTransitionSpec(reduceMotion: Boolean) =
    popEnterTransition(reduceMotion) togetherWith popExitTransition(reduceMotion)
