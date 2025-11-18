package com.klyx.runner

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import com.klyx.activities.TerminalActivity
import com.klyx.core.Notifier
import com.klyx.core.file.KxFile
import com.klyx.core.file.uri
import com.klyx.core.runner.CodeRunner
import com.klyx.terminal.TerminalCommand
import com.klyx.core.terminal.klyxBinDir
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val CANNOT_RUN_ERROR =
    "Running code is not supported in this location. Consider moving your file to the terminal's home directory to run it"
private const val SDCARD_RUNNER_WARNING =
    "Running code is not recommended in this location. Consider moving your file to the terminal's home directory."

class UniversalRunner : CodeRunner, KoinComponent {
    private val notifier: Notifier by inject()
    private val context: Context by inject()

    @JvmName("canRun1")
    private fun KxFile.canRun() = this.uri.scheme == ContentResolver.SCHEME_FILE

    @SuppressLint("SdCardPath")
    private fun KxFile.isFromExternalStorage() = absolutePath.startsWith("/sdcard") or
            absolutePath.startsWith("/storage") or
            absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)

    private val runnableExtensions = setOf(
        "py", "js", "ts", "java", "kt", "rs", "rb", "php",
        "c", "cpp", "cxx", "cc", "cs",
        "sh", "bash", "zsh", "fish",
        "pl", "lua", "r",
        "f90", "f95", "f03", "f08",
        "pas", "tcm", "elm",
        "fsx", "fs"
    )

    override fun canRun(file: KxFile): Boolean {
        return file.extension.lowercase() in runnableExtensions && file.canRun()
    }

    override suspend fun run(file: KxFile) {
        if (!file.canRun()) {
            notifier.error(CANNOT_RUN_ERROR)
            return
        }

        if (file.isFromExternalStorage()) {
            //notifier.toast(SDCARD_RUNNER_WARNING)
            Toast.makeText(context, SDCARD_RUNNER_WARNING, Toast.LENGTH_SHORT).show()
        }

        with(context) {
            startActivity(Intent(this, TerminalActivity::class.java).apply {
                putExtra(
                    "command", TerminalCommand(
                        cmd = "/bin/bash",
                        args = arrayOf(klyxBinDir.resolve("universal_runner").absolutePath, file.absolutePath),
                        id = "universal_runner",
                        terminatePreviousSession = true,
                        cwd = file.parentFile?.absolutePath ?: "/"
                    )
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
