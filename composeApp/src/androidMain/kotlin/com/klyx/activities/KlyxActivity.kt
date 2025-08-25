package com.klyx.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.klyx.core.ContextHolder
import com.klyx.core.WindowManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

open class KlyxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        ContextHolder.setCurrentActivity(this)

        WindowManager.currentTaskId = taskId
        WindowManager.addWindow(taskId)
    }

    override fun onResume() {
        super.onResume()
        ContextHolder.setCurrentActivity(this)
        WindowManager.currentTaskId = taskId
    }

    override fun onDestroy() {
        super.onDestroy()
        ContextHolder.setCurrentActivity(null)
    }
}
