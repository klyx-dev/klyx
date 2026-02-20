package com.klyx.test

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.klyx.core.KlyxBuildConfig
import com.klyx.core.allowDiskReads
import com.klyx.core.io.Paths
import com.klyx.core.ui.component.FpsText
import com.klyx.core.util.join
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.ui.Terminal
import com.klyx.terminal.ui.rememberTerminalState
import com.klyx.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.io.files.SystemFileSystem

private val tmpDir by lazy { allowDiskReads { Paths.tempDir } }
private val filesDir by lazy { allowDiskReads { Paths.dataDir.join("files") } }

private val env by lazy {
    allowDiskReads {
        mapOf(
            "PROOT_TMP_DIR" to "${
                tmpDir.join("terminal/test-session").also {
                    SystemFileSystem.createDirectories(it)
                }
            }",
            "COLORTERM" to "truecolor",
            "TERM" to "xterm-256color",
            "USER" to "vv",
            "TZ" to "UTC",
            "WKDIR" to "/",
            "LANG" to "C.UTF-8",
            "DEBUG" to "${KlyxBuildConfig.IS_DEBUG}",
            "LOCAL" to "${filesDir.join("usr")}",
            "SANDBOX_DIR" to "${Paths.dataDir.join("sandbox")}",
            "LD_LIBRARY_PATH" to "${filesDir.join("usr/lib")}",
            "PROMPT_DIRTRIM" to "2",
            "LINKER" to "/system/bin/linker64",
            "TZ" to "UTC",
            "TMP_DIR" to "$tmpDir",
            "TMPDIR" to "$tmpDir",
            "DATADIR" to "${Paths.dataDir}",
            "DOTNET_GCHeapHardLimit" to "1C0000000",
            "PENDING_CMD" to "false",
            "DISPLAY" to ":0"
        )
    }
}

@Composable
fun TestScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { scaffoldPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .consumeWindowInsets(scaffoldPadding)
                .systemBarsPadding()
        ) {
            var cursorStyle by remember { mutableStateOf(CursorStyle.Block) }
            val terminalState = rememberTerminalState(
                shell = "/system/bin/sh",
                args = listOf("-c", "${filesDir.join("usr/bin/sandbox")}"),
                env = env,
                cwd = "$filesDir",
                cursorStyle = cursorStyle
            )

            LaunchedEffect(terminalState) {
                delay(5000)
                //cursorStyle = CursorStyle.Underline
                //terminalState.session.write("echo hello\n")
            }

            Terminal(
                state = terminalState,
                modifier = Modifier
                    .fillMaxSize()
                    .border(Dp.Hairline, Color.Green),
                fontSize = 18.sp,
                fontFamily = JetBrainsMonoFontFamily
            )

            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd,
            ) {
                FpsText()
            }
        }
    }
}
