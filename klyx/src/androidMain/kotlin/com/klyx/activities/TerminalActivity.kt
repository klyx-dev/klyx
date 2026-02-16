package com.klyx.activities

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.terminal.service.SessionService
import com.klyx.terminal.service.SessionServiceConnection
import com.klyx.ui.component.terminal.Terminal

class TerminalActivity : KlyxActivity() {
    val serviceConnection = SessionServiceConnection()

    inline val sessionBinder get() = serviceConnection.sessionBinder

    @Composable
    override fun Content() {
        Terminal(
            activity = this,
            modifier = Modifier.fillMaxSize(),
            onSessionFinish = { finish() }
        )
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SessionService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
//        if (serviceConnection.isBound) {
//            unbindService(serviceConnection)
//            serviceConnection.isBound = false
//        }
    }
}
