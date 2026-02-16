package com.klyx.terminal

import com.klyx.activities.TerminalActivity
import com.klyx.core.terminal.extrakey.ExtraKeysViewClient
import com.klyx.ui.component.terminal.extraKeysView
import com.termux.view.TerminalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun changeSession(
    activity: TerminalActivity,
    terminalView: TerminalView,
    sessionId: TerminalSessionId
) {
    withContext(Dispatchers.Main) {
        terminalView.apply {
            val client = TerminalClient(this, activity)

//            val session = withContext(Dispatchers.Default) {
//                activity.sessionBinder!!.getSession(sessionId)
//                    ?: activity.sessionBinder!!.createSession(sessionId, client, activity = activity)
//            }
//
//            session.updateTerminalSessionClient(client)
//            attachSession(session)
            setTerminalViewClient(client)

            post {
                keepScreenOn = true
                requestFocus()
                setFocusableInTouchMode(true)
            }

            extraKeysView.get()?.let {
                it.extraKeysViewClient = terminalView.mTermSession?.let { ExtraKeysViewClient(it) }
            }
        }
    }
    //activity.sessionBinder!!.service.currentSession = sessionId
}
