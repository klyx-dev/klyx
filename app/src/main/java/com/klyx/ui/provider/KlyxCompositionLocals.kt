package com.klyx.ui.provider

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.core.App
import com.klyx.core.LocalApp
import com.klyx.data.editor.LocalKlyxEditorColorScheme
import com.klyx.data.editor.rememberEditorColorScheme
import com.klyx.api.data.preferences.AppSettings
import com.klyx.api.data.preferences.AppTheme
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.language.LanguageRegistry
import com.klyx.data.preferences.SettingsRepository
import com.klyx.event.eventBus
import com.klyx.language.LanguageRegistryImpl
import com.klyx.ui.ImmersiveModeHandler
import com.klyx.ui.animation.LocalReduceMotion
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.ui.theme.KlyxThemeSurface
import com.klyx.ui.widgets.ToastHost
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject

@Composable
fun KlyxCompositionLocals(content: @Composable BoxScope.() -> Unit) {
    val screenSize = rememberScreenSize()
    val treeSitter = rememberTreeSitter()

    val app: App = currentKoinScope().get()

    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val appTheme by settingsRepository.appTheme.collectAsStateWithLifecycle(initialValue = AppTheme.System)

    val darkMode by remember {
        derivedStateOf {
            when (appTheme) {
                AppTheme.Light -> false
                AppTheme.Dark -> true
                AppTheme.System -> isSystemInDarkTheme
            }
        }
    }

    val editorColorScheme = rememberEditorColorScheme()

    val values by remember {
        derivedStateOf {
            arrayOf(
                LocalScreenSize provides screenSize,
                LocalTreeSitter provides treeSitter,
                LocalIsDarkMode provides darkMode,
                LocalAppSettings provides settings,
                LocalReduceMotion provides settings.appearance.reduceMotion,
                LocalKlyxEditorColorScheme provides editorColorScheme,
                LocalApp provides app,
                LocalEventBus provides app.eventBus()
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val languageRegistry = app.globalOrNull<LanguageRegistry>() as? LanguageRegistryImpl

    DisposableEffect(lifecycleOwner, treeSitter) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                treeSitter.close()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        languageRegistry?.bind(treeSitter)
        onDispose {
            languageRegistry?.unbind()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(values = values) {
        ImmersiveModeHandler(
            isImmersiveModeEnabled = settings.appearance.immersiveMode
        ) {
            KlyxThemeSurface {
                content()
                ToastHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
