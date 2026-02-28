package com.klyx.terminal

import android.media.AudioManager
import android.media.ToneGenerator

actual class TerminalBell {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    actual fun ring() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 150)
    }

    actual fun release() {
        toneGenerator.release()
    }
}
