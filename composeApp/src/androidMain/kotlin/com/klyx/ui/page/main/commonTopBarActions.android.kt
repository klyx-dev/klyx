package com.klyx.ui.page.main

import android.content.Intent
import com.klyx.activities.TerminalActivity
import com.klyx.core.PlatformContext

actual fun PlatformContext.openTerminal() {
    startActivity(Intent(this, TerminalActivity::class.java))
}
