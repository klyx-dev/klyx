package com.klyx.ui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import com.klyx.ui.provider.KlyxCompositionLocals
import com.klyx.api.ui.theme.LocalIsDarkMode

abstract class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideSystemBarsOnLandscape()
        setContent {
            KlyxCompositionLocals {
                val darkMode = LocalIsDarkMode.current

                LaunchedEffect(darkMode) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            darkScrim = 0,
                            lightScrim = 0,
                            detectDarkMode = { darkMode }
                        ),
                        navigationBarStyle = SystemBarStyle.auto(
                            darkScrim = 0,
                            lightScrim = 0,
                            detectDarkMode = { darkMode }
                        )
                    )
                }

                Content()
            }
        }
    }

    @Composable
    abstract fun BoxScope.Content()

    private fun hideSystemBarsOnLandscape() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.display ?: return
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay ?: return
        }

        when (display.rotation) {
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    show(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = BEHAVIOR_DEFAULT
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemBarsOnLandscape()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBarsOnLandscape()
    }
}
