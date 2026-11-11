package com.klyx

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.klyx.core.App
import com.klyx.core.Log
import com.klyx.core.action.action
import com.klyx.runtime.action.OpenDocument
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.settings.ThemeMode
import com.klyx.runtime.workspace.DocumentLocation
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.name
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private var storagePermissionGranted by mutableStateOf(false)

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true

        if (readGranted && writeGranted) {
            storagePermissionGranted = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        App.global.declare(this)
        FileKit.init(this)
        Log.i("App", "MainActivity.onCreate")
        if (!hasStorageAccess()) {
            requestStoragePermission()
        }
        setContent {
            val settingsStore: SettingsStore = koinInject()
            val settings by settingsStore.settings.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val darkMode by remember {
                derivedStateOf {
                    when (settings.themeMode) {
                        ThemeMode.Dark -> true
                        ThemeMode.Light -> false
                        ThemeMode.System -> isSystemDark
                    }
                }
            }

            LaunchedEffect(settings.themeMode) {
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

            val pendingCrash = remember {
                mutableStateOf(CrashReporter.loadPending(this))
            }

            Screen()

            pendingCrash.value?.let { report ->
                CrashReportDialog(
                    report = report,
                    onDismiss = {
                        CrashReporter.clearPending(this)
                        pendingCrash.value = null
                    },
                    onCopy = { copyCrashReport(report) },
                    onShare = {
                        shareCrashReport(report)
                        CrashReporter.clearPending(this)
                        pendingCrash.value = null
                    },
                    onOpenIssue = {
                        openCrashIssue(report)
                        CrashReporter.clearPending(this)
                        pendingCrash.value = null
                    },
                )
            }
        }
    }

    private fun copyCrashReport(report: CrashReport) {
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Klyx crash report", report.toShareText()))
    }

    private fun shareCrashReport(report: CrashReport) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Klyx crash report")
            putExtra(Intent.EXTRA_TEXT, report.toShareText())
        }
        startActivity(Intent.createChooser(intent, "Share crash report"))
    }

    private fun openCrashIssue(report: CrashReport) {
        val title = "Crash: ${report.exceptionClass}"
        val body = report.toShareText().take(6000)
        val url = "https://github.com/klyx-dev/klyx/issues/new" +
                "?title=${Uri.encode(title)}" +
                "&body=${Uri.encode(body)}" +
                "&labels=crash,alpha"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onResume() {
        super.onResume()
        App.global.declare(this)
        storagePermissionGranted = hasStorageAccess()
        handleIntent(intent)
    }

    @SuppressLint("UseKtx")
    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun hasLegacyStoragePermissions(): Boolean {
        val read = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    private fun hasStorageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasLegacyStoragePermissions()
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data

        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT) {
            if (uri != null) {
                val file = PlatformFile(uri)
                action(
                    OpenDocument(
                        location = DocumentLocation.Platform(file = file),
                        name = file.name,
                        preview = false
                    )
                ).invoke()
            }
            setIntent(Intent())
        }
    }
}
