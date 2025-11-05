package com.klyx.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import arrow.core.some
import com.klyx.core.ContextHolder
import com.klyx.core.WindowManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.manualFileKitCoreInitialization

open class KlyxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FileKit.manualFileKitCoreInitialization(this)
        ContextHolder.setCurrentActivity(some())

        WindowManager.currentTaskId = taskId
        WindowManager.addWindow(taskId)
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
