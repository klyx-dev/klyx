package com.klyx.ui.component.terminal

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.klyx.activities.TerminalActivity
import com.klyx.core.terminal.ExtraKeys
import com.klyx.core.terminal.extrakey.ExtraKeysConstants
import com.klyx.core.terminal.extrakey.ExtraKeysInfo
import com.klyx.core.terminal.extrakey.ExtraKeysView
import com.klyx.core.terminal.extrakey.ExtraKeysViewClient
import com.klyx.core.toJson
import com.klyx.terminal.TerminalClient
import com.klyx.terminal.service.SessionService
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

var extraKeysView = WeakReference<ExtraKeysView?>(null)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerminalScreen(
    user: String,
    activity: TerminalActivity,
    modifier: Modifier = Modifier,
    onSessionFinish: (TerminalSession) -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        context.startService(Intent(context, SessionService::class.java))
    }

    if (activity.sessionBinder != null) {
        Column(
            modifier = modifier
                .systemBarsPadding()
                .imePadding()
        ) {
            TerminalScreenInternal(
                user = user,
                activity = activity,
                onSessionFinish = onSessionFinish
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ContainedLoadingIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.TerminalScreenInternal(
    user: String,
    activity: TerminalActivity,
    onSessionFinish: (TerminalSession) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val terminal by produceState<TerminalView?>(null) {
        value = with(context) { TerminalView(user, activity) }
    }

    when (val terminal = terminal) {
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator()
            }
        }

        else -> {
            val extraKeysView = remember {
                ExtraKeysView(context, terminal, with(density) { 75.dp.toPx() })
            }

            AndroidView(
                factory = {
                    terminal.apply {
                        post {
                            keepScreenOn = true
                            requestFocus()
                            isFocusableInTouchMode = true
                        }
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
}

context(context: Context)
private suspend fun TerminalView(
    user: String,
    activity: TerminalActivity,
) = withContext(Dispatchers.Main) {
    TerminalView(context, null).apply {
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

        val client = TerminalClient(this, activity)

//        val session = with(activity.sessionBinder!!) {
//            withContext(Dispatchers.Default) {
//                getSession(service.currentSession)
//                    ?: createSession(
//                        id = service.currentSession,
//                        userName = user,
//                        client = client,
//                        activity = activity
//                    )
//            }
//        }
//        session.updateTerminalSessionClient(client)
//        attachSession(session)
        setTerminalViewClient(client)
    }
}

private fun ExtraKeysView(
    context: Context,
    terminalView: TerminalView,
    heightPx: Float
) = ExtraKeysView(context, null).apply {
    extraKeysView = WeakReference(this)
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
