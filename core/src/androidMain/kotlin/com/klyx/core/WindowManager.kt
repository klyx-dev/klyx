package com.klyx.core

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess

object WindowManager : KoinComponent {
    private val context by inject<Context>()

    var currentTaskId by mutableIntStateOf(-1)
    val openedWindows = mutableStateListOf<Int>()

    fun addWindow(taskId: Int) {
        openedWindows += taskId
    }

    fun closeWindow(taskId: Int) {
        val am = context.getSystemService(ActivityManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                am.appTasks.single().finishAndRemoveTask()
                exitProcess(0)
            } catch (_: Exception) {
                for (appTask in am.appTasks) {
                    if (appTask.taskInfo.taskId == taskId) {
                        appTask.finishAndRemoveTask()
                        break
                    }
                }
            }
        } else {
            ContextHolder.currentActivityOrNull()?.finishAndRemoveTask()
        }
        openedWindows -= taskId
        currentTaskId = openedWindows.lastOrNull() ?: -1
    }

    fun closeCurrentWindow() {
        closeWindow(currentTaskId)
    }
}
