package com.klyx.ui.component.terminal

import android.content.Context
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
import androidx.compose.material3.ContainedLoadingIndicator
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.klyx.core.file.humanBytes
import com.klyx.core.logging.logger
import com.klyx.core.net.isConnected
import com.klyx.core.net.isNotConnected
import com.klyx.core.net.rememberNetworkState
import com.klyx.terminal.internal.currentUser
import com.klyx.terminal.internal.downloadRootFs
import com.klyx.terminal.internal.isTerminalSetupDone
import com.klyx.terminal.internal.setupRootFs
import com.klyx.ui.theme.rememberFontFamily
import com.termux.terminal.TerminalSession
import io.ktor.client.content.ProgressListener

private val logger = logger("Terminal")

@Composable
fun Terminal(
    modifier: Modifier = Modifier,
    onSessionFinish: (TerminalSession) -> Unit = {}
) {
    val context = LocalContext.current

    Surface(modifier = modifier) {
        var isCompleted by remember { with(context) { mutableStateOf(isTerminalSetupDone) } }
        var user by remember { with(context) { mutableStateOf(currentUser) } }

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
                                with(context) {
                                    currentUser = userName
                                    user = userName
                                }
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
                        user = user!!,
                        onSessionFinish = onSessionFinish
                    )
                }
            }
        } else {
            with(context) {
                SetupScreen(
                    onComplete = {
                        isCompleted = true
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
context(context: Context)
private fun SetupScreen(
    onComplete: () -> Unit
) {
    val networkState by rememberNetworkState()
    val fontFamily = rememberFontFamily("JetBrains Mono")
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        fontFamily = fontFamily
    )

    var error: Throwable? by remember { mutableStateOf(null) }

    var downloaded by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }

    LaunchedEffect(networkState) {
        error = null

        if (!isTerminalSetupDone && networkState.isConnected) {
            setupTerminal(
                onDownload = { bytesDownloaded, contentLength ->
                    downloaded = bytesDownloaded
                    total = contentLength ?: 0L
                },
                onComplete = onComplete,
                onError = { error = it }
            )
        }
    }

    val progress by remember {
        derivedStateOf {
            if (total == 0L) 0f else downloaded.toFloat() / total
        }
    }

    val (message, progressValue) = when {
        networkState.isNotConnected -> "No internet connection" to null
        progress > 0 && progress < 1f -> {
            "Almost there... ${(progress * 100).toInt()}% (${downloaded.humanBytes()} / ${total.humanBytes()}) done!" to progress
        }

        progress >= 1f -> "Extracting files, please wait a moment..." to null
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
                        ContainedLoadingIndicator()
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

context(context: Context)
private suspend fun setupTerminal(
    onDownload: ProgressListener? = null,
    onComplete: () -> Unit = {},
    onError: (error: Throwable) -> Unit = {}
) {
    if (!isTerminalSetupDone) {
        downloadRootFs(
            onDownload = onDownload,
            onComplete = { path ->
                isTerminalSetupDone = try {
                    setupRootFs(path)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.error("Failed to setup terminal", e)
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
