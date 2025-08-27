package com.klyx.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import com.klyx.core.ContextHolder
import com.klyx.core.SharedLocalProvider
import com.klyx.core.WindowManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import androidx.activity.compose.setContent as setContentInternal

open class KlyxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        ContextHolder.setCurrentActivity(this)

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
        ContextHolder.setCurrentActivity(this)
        WindowManager.currentTaskId = taskId
    }

    override fun onStart() {
        super.onStart()
        ContextHolder.setCurrentActivity(this)
    }

    override fun onRestart() {
        super.onRestart()
        ContextHolder.setCurrentActivity(this)
    }
}
