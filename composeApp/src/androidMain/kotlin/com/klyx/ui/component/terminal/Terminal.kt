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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.klyx.activities.TerminalActivity
import com.klyx.core.file.DownloadableFile
import com.klyx.core.file.downloadAll
import com.klyx.core.file.humanBytes
import com.klyx.core.net.isConnected
import com.klyx.core.net.isNotConnected
import com.klyx.core.net.rememberNetworkState
import com.klyx.terminal.SetupNextStage
import com.klyx.terminal.getNextStage
import com.klyx.core.terminal.currentUser
import com.klyx.terminal.internal.extractTarGz
import com.klyx.terminal.internal.packageUrl
import com.klyx.terminal.internal.ubuntuRootFsUrl
import com.klyx.core.terminal.isTerminalInstalled
import com.klyx.core.terminal.klyxBinDir
import com.klyx.core.terminal.klyxFilesDir
import com.klyx.core.terminal.klyxLibDir
import com.klyx.core.terminal.localDir
import com.klyx.core.terminal.sandboxDir
import com.klyx.terminal.terminalSetupNextStage
import com.klyx.ui.theme.rememberFontFamily
import com.termux.terminal.TerminalSession

internal val LocalTerminalUsername = compositionLocalOf<String> {
    error("Terminal username not provided")
}

@Composable
fun Terminal(
    activity: TerminalActivity,
    modifier: Modifier = Modifier,
    onSessionFinish: (TerminalSession) -> Unit = { activity.finish() }
) {
    val context = LocalContext.current

    Surface(modifier = modifier) {
        with(context) {
            TerminalScreen1(activity = activity, onSessionFinish = onSessionFinish)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
context(context: Context)
private fun TerminalScreen1(activity: TerminalActivity, onSessionFinish: (TerminalSession) -> Unit) {
    val networkState by rememberNetworkState()
    val fontFamily = rememberFontFamily("JetBrains Mono")
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        fontFamily = fontFamily
    )

    var error: Throwable? by remember { mutableStateOf(null) }

    var downloaded by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var needsDownload by remember { mutableStateOf(true) }
    var currentFileName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        error = null

        val isProotExists = klyxBinDir.resolve("proot").exists()
        val libtallocExists = klyxLibDir.resolve("libtalloc.so").exists()
        needsDownload = !isProotExists || !libtallocExists || !isTerminalInstalled()

        val filesToDownload = buildList {
            if (!isProotExists) add(packageDownloadableFile("proot"))
            if (!libtallocExists) add(packageDownloadableFile("libtalloc"))

            if (!isTerminalInstalled()) {
                add(
                    DownloadableFile(
                        url = ubuntuRootFsUrl,
                        outputPath = "${context.cacheDir.absolutePath}/sandbox.tar.gz"
                    )
                )
            }
        }

        filesToDownload.downloadAll(
            onComplete = { file ->
                if (file.name.startsWith("proot") || file.name.startsWith("libtalloc")) {
                    if (!extractTarGz(file.absolutePath, klyxFilesDir.absolutePath)) {
                        error("Failed to extract ${file.name}")
                    }
                }
            },
            onFileProgress = { file, sent, totalBytes ->
                downloaded = sent
                currentFileName = file.name
                total = totalBytes ?: 0L
            },
            onAllComplete = {
                terminalSetupNextStage = getNextStage()
                needsDownload = false
            },
            onError = { file, exception ->
                error = exception

                runCatching {
                    if (file.absolutePath.contains(localDir.absolutePath)) {
                        localDir.deleteRecursively()
                    }

                    if (file.name == "sandbox.tar.gz") {
                        sandboxDir.deleteRecursively()

                        with(context.cacheDir.resolve("sandbox.tar.gz")) {
                            if (exists()) delete()
                        }
                    }
                }.onFailure { it.printStackTrace() }
            }
        )
    }

    val progress by remember {
        derivedStateOf {
            if (total == 0L) 0f else downloaded.toFloat() / total
        }
    }

    val (message, progressValue) = when {
        networkState.isNotConnected -> "No internet connection" to null
        progress > 0 && progress < 1f -> {
            "$currentFileName ${(progress * 100).toInt()}% (${downloaded.humanBytes()} / ${total.humanBytes()})" to progress
        }

        progress >= 1f -> "Extracting files..." to null
        else -> "Installing terminal..." to null
    }

    if (needsDownload && terminalSetupNextStage == SetupNextStage.None) {
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
                            LinearWavyProgressIndicator(progress = { progressValue })
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
    } else {
        var user by remember { with(context) { mutableStateOf(currentUser) } }

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

                    val createSession = {
                        with(context) {
                            currentUser = userName
                            user = userName
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
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { createSession() }),
                        isError = !isValid,
                        shape = RoundedCornerShape(12.dp),
                    )

                    //Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = createSession,
                        shape = RoundedCornerShape(12.dp),
                        enabled = isValid
                    ) {
                        Text("Create Session", fontFamily = fontFamily)
                    }
                }
            } else {
                CompositionLocalProvider(LocalTerminalUsername provides user!!) {
                    TerminalScreen(
                        modifier = Modifier.fillMaxSize(),
                        activity = activity,
                        user = user!!,
                        onSessionFinish = onSessionFinish
                    )
                }
            }
        }
    }
}

context(context: Context)
private fun packageOutPath(name: String): String {
    return context.cacheDir.resolve("$name.tar.gz").absolutePath
}

context(context: Context)
private fun packageDownloadableFile(name: String): DownloadableFile {
    return DownloadableFile(
        url = packageUrl(name),
        outputPath = packageOutPath(name)
    )
}
