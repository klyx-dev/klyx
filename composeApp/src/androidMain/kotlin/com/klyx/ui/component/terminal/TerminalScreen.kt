package com.klyx.ui.component.terminal

import android.content.Context
import android.graphics.Typeface
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.klyx.activities.TerminalActivity
import com.klyx.terminal.TerminalSessionClient
import com.klyx.terminal.TerminalViewClient
import com.klyx.terminal.internal.createSession
import com.termux.view.TerminalView

@Composable
context(context: Context)
fun TerminalScreen(
    user: String,
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current as? TerminalActivity

    AndroidView(
        factory = {
            TerminalView(it, null).apply {
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

                setTerminalViewClient(TerminalViewClient(this, activity))
                val sessionClient = TerminalSessionClient(this, activity)

                val session = createSession(user, sessionClient)
                session.updateTerminalSessionClient(sessionClient)
                attachSession(session)
            }
        },
        modifier = modifier
            .systemBarsPadding()
            .imePadding(),
    )
}
