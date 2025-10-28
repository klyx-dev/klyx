package com.klyx.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import arrow.core.some
import com.klyx.core.ContextHolder
import com.klyx.core.SharedLocalProvider
import com.klyx.core.WindowManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.manualFileKitCoreInitialization
import androidx.activity.compose.setContent as setContentInternal

open class KlyxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FileKit.manualFileKitCoreInitialization(this)
        ContextHolder.setCurrentActivity(some())

        WindowManager.currentTaskId = taskId
        WindowManager.addWindow(taskId)
    }

    fun setContent(provideLocals: Boolean = true, content: @Composable (() -> Unit)) {
        setContentInternal {
            if (provideLocals) {
                SharedLocalProvider(content)
            } else {
                content()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ContextHolder.setCurrentActivity(some())
        WindowManager.currentTaskId = taskId
    }

    override fun onStart() {
        super.onStart()
        ContextHolder.setCurrentActivity(some())
    }

    override fun onRestart() {
        super.onRestart()
        ContextHolder.setCurrentActivity(some())
    }
}
