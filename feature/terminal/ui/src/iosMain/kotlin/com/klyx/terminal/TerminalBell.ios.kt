package com.klyx.terminal

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate

actual class TerminalBell {

    actual fun ring() {
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
    }

    actual fun release() {}
}
