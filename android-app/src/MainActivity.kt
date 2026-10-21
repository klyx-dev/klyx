package com.klyx

import android.Manifest
import android.annotation.SuppressLint
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
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.settings.ThemeMode
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

            Screen()
        }
    }

    override fun onResume() {
        super.onResume()
        App.global.declare(this)
        storagePermissionGranted = hasStorageAccess()
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
}
