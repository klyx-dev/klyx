package com.klyx.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klyx.core.SharedLocalProvider
import com.klyx.core.file.DownloadProgress
import com.klyx.core.net.isConnected
import com.klyx.core.net.isNotConnected
import com.klyx.core.net.rememberNetworkState
import com.klyx.terminal.internal.currentUser
import com.klyx.terminal.internal.downloadRootFs
import com.klyx.terminal.internal.isTerminalSetupDone
import com.klyx.terminal.internal.setupRootFs
import com.klyx.terminal.klyxBinDir
import com.klyx.ui.component.terminal.TerminalScreen
import com.klyx.ui.theme.KlyxTheme
import com.klyx.ui.theme.rememberFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.io.File

class TerminalActivity : ComponentActivity(), CoroutineScope by MainScope() {
    companion object {
        private const val TAG = "TerminalActivity"
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        runCatching {
            File(klyxBinDir, "init").writeBytes(
                assets.open("terminal/init.sh").use { it.readBytes() }
            )
        }

        setContent {
            SharedLocalProvider {
                KlyxTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val networkState by rememberNetworkState()
                        var progress by remember { mutableFloatStateOf(0f) }
                        var downloadError: Throwable? by remember { mutableStateOf(null) }
                        var isCompleted by remember { mutableStateOf(isTerminalSetupDone) }
                        var user by remember { mutableStateOf(currentUser) }

                        LaunchedEffect(networkState) {
                            downloadError = null

                            if (!isTerminalSetupDone && networkState.isConnected) {
                                setupTerminal(
                                    onProgress = { progress = it.percentage },
                                    onComplete = { isCompleted = true },
                                    onError = { downloadError = it }
                                )
                            }
                        }

                        if (isCompleted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .imePadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (user == null) {
                                    val fontFamily = rememberFontFamily("JetBrains Mono")

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        var userName by remember { mutableStateOf("") }
                                        val isValid by remember {
                                            derivedStateOf {
                                                userName.matches("^[a-z][-a-z0-9_]*$".toRegex())
                                            }
                                        }

                                        val errorMessage by remember {
                                            derivedStateOf {
                                                if (!isValid) "Invalid username" else null
                                            }
                                        }

                                        OutlinedTextField(
                                            value = userName,
                                            onValueChange = {
                                                userName = it.lowercase().trim()
                                            },
                                            placeholder = {
                                                Text("Enter your username")
                                            },
                                            supportingText = {
                                                Text(
                                                    text = errorMessage
                                                        ?: ("This will create a new user" +
                                                                " in the terminal and" +
                                                                " will be saved for future sessions."),
                                                    fontFamily = fontFamily
                                                )
                                            },
                                            isError = !isValid,
                                            shape = RoundedCornerShape(12.dp),
                                        )

                                        //Spacer(modifier = Modifier.height(4.dp))

                                        Button(
                                            onClick = {
                                                currentUser = userName
                                                user = userName
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = isValid
                                        ) {
                                            Text("Create Session", fontFamily = fontFamily)
                                        }
                                    }
                                } else {
                                    TerminalScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        user = user!!
                                    )
                                }
                            }
                        } else {
                            SetupScreen(progress, downloadError)
                        }
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun SetupScreen(
        progress: Float,
        error: Throwable? = null
    ) {
        val networkState by rememberNetworkState()
        val fontFamily = rememberFontFamily("JetBrains Mono")
        val textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = fontFamily
        )

        val (message, progressValue) = when {
            networkState.isNotConnected -> "No internet connection" to null
            progress > 0 && progress < 100f -> "Almost there... ${progress.toInt()}% done!" to (progress / 100f)
            progress >= 100f -> "Extracting files, please wait a moment..." to null
            else -> "Downloading Ubuntu Root FileSystem, please wait a moment..." to null
        }

        ProvideTextStyle(textStyle) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (error == null && networkState.isConnected) {
                        if (progressValue != null) {
                            CircularWavyProgressIndicator(
                                progress = { progressValue },
                            )
                        } else {
                            CircularWavyProgressIndicator()
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }

                    Text(
                        error?.message ?: message,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }
    }

    private suspend fun setupTerminal(
        onProgress: suspend (DownloadProgress) -> Unit = {},
        onComplete: suspend () -> Unit = {},
        onError: suspend (error: Throwable) -> Unit = {}
    ) {
        if (!isTerminalSetupDone) {
            downloadRootFs(
                onProgress = onProgress,
                onComplete = { path ->
                    isTerminalSetupDone = try {
                        setupRootFs(path)
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG, "Failed to setup terminal", e)
                        throw e
                    }
                    onComplete()
                },
                onError = onError
            )
        } else {
            onComplete()
        }
    }
}
