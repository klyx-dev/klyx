package com.klyx.ui.component.terminal

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.klyx.core.terminal.ExtraKeys
import com.klyx.core.toJson
import com.klyx.terminal.TerminalSessionClient
import com.klyx.terminal.TerminalViewClient
import com.klyx.terminal.extrakeys.ExtraKeysConstants
import com.klyx.terminal.extrakeys.ExtraKeysInfo
import com.klyx.terminal.extrakeys.ExtraKeysView
import com.klyx.terminal.extrakeys.ExtraKeysViewClient
import com.klyx.terminal.internal.createSession
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import kotlinx.coroutines.launch

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun TerminalScreen(
    user: String,
    modifier: Modifier = Modifier,
    onSessionFinish: (TerminalSession) -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val terminal = remember { with(context) { TerminalView(user) } }
    val extraKeysView = remember {
        with(context) { ExtraKeysView(terminal, with(density) { 75.dp.toPx() }) }
    }

    Column(
        modifier = modifier
            .systemBarsPadding()
            .imePadding()
    ) {
        AndroidView(
            factory = {
                terminal.apply {
                    setTerminalViewClient(TerminalViewClient(this, extraKeysView, context as? Activity).also {
                        it.onSessionFinish = onSessionFinish
                    })
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        val pagerState = rememberPagerState { 2 }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
        ) { page ->
            if (page == 0) {
                AndroidView(
                    factory = { extraKeysView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp)
                )
            } else if (page == 1) {
                val scope = rememberCoroutineScope()
                var text by rememberSaveable { mutableStateOf("") }

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it

                        if (pagerState.currentPage != 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    },
                    placeholder = { Text("Type something...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (text.isEmpty()) {
                                terminal.dispatchKeyEvent(
                                    KeyEvent(
                                        KeyEvent.ACTION_DOWN,
                                        KeyEvent.KEYCODE_ENTER
                                    )
                                )
                                terminal.dispatchKeyEvent(
                                    KeyEvent(
                                        KeyEvent.ACTION_UP,
                                        KeyEvent.KEYCODE_ENTER
                                    )
                                )
                            } else {
                                terminal.mTermSession.write(text)
                                text = ""
                            }
                        }
                    )
                )
            }
        }
    }
}

@Suppress("MagicNumber")
context(context: Context)
private fun TerminalView(user: String) = TerminalView(context, null).apply {
    defaultFocusHighlightEnabled = true
    isFocusableInTouchMode = true
    isVerticalScrollBarEnabled = true
    setTextSize(24)
    setTypeface(
        Typeface.createFromAsset(
            context.assets,
            "fonts/JetBrainsMono-Regular.ttf"
        )
    )

    val sessionClient = TerminalSessionClient(this, context as? Activity)

    val session = createSession(user, sessionClient)
    session.updateTerminalSessionClient(sessionClient)
    attachSession(session)
}

context(context: Context)
private fun ExtraKeysView(
    terminalView: TerminalView,
    heightPx: Float
) = ExtraKeysView(context, null).apply {
    extraKeysViewClient = ExtraKeysViewClient(terminalView.mTermSession)

    reload(
        ExtraKeysInfo(
            ExtraKeys.toJson(),
            "",
            ExtraKeysConstants.CONTROL_CHARS_ALIASES
        ),
        heightPx
    )
}
