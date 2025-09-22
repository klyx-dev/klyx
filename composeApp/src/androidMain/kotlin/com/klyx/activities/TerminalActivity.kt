package com.klyx.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.klyx.core.LocalAppSettings
import com.klyx.ui.component.terminal.Terminal
import com.klyx.ui.theme.KlyxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class TerminalActivity : KlyxActivity(), CoroutineScope by MainScope() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KlyxTheme(LocalAppSettings.current.theme) {
                Terminal(
                    modifier = Modifier.fillMaxSize(),
                    onSessionFinish = {
                        finishAfterTransition()
                    }
                )
            }
        }
    }
}
