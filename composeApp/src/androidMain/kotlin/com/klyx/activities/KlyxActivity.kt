package com.klyx.activities

import android.content.res.Configuration
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import arrow.core.some
import com.klyx.KlyxApp
import com.klyx.core.ContextHolder
import com.klyx.core.WindowManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.manualFileKitCoreInitialization

abstract class KlyxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FileKit.manualFileKitCoreInitialization(this)
        ContextHolder.setCurrentActivity(some())

        WindowManager.currentTaskId = taskId
        WindowManager.addWindow(taskId)

        hideSystemBarsOnLandscape()

        setContent {
            KlyxApp { Content() }
        }
    }

    @Composable
    abstract fun Content()

    private fun hideSystemBarsOnLandscape() {
        when (display.rotation) {
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    show(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
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
        ContextHolder.setCurrentActivity(some())
        WindowManager.currentTaskId = taskId
        hideSystemBarsOnLandscape()
    }

    override fun onStart() {
        super.onStart()
        ContextHolder.setCurrentActivity(some())
        hideSystemBarsOnLandscape()
    }

    override fun onRestart() {
        super.onRestart()
        ContextHolder.setCurrentActivity(some())
        hideSystemBarsOnLandscape()
    }
}
