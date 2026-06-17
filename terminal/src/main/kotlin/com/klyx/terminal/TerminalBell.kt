package com.klyx.terminal

import android.media.AudioManager
import android.media.ToneGenerator

class TerminalBell(
    volume: Float = 1.0f,
    soundType: BellSoundType = BellSoundType.Gentle
) {
    private var currentVolume = volume
    private var currentSoundType = soundType
    private var toneGenerator: ToneGenerator? = createToneGenerator()

    private fun createToneGenerator(): ToneGenerator? {
        return if (currentSoundType == BellSoundType.VisualOnly) null
        else ToneGenerator(AudioManager.STREAM_NOTIFICATION, (currentVolume * 100).toInt().coerceIn(0, 100))
    }

    fun ring() {
        val gen = toneGenerator ?: return
        val tone = when (currentSoundType) {
            BellSoundType.System -> ToneGenerator.TONE_CDMA_ABBR_ALERT
            BellSoundType.Gentle -> ToneGenerator.TONE_PROP_ACK
            BellSoundType.VisualOnly -> return
        }
        gen.startTone(tone, 150)
    }

    fun updateVolume(newVolume: Float) {
        currentVolume = newVolume
        toneGenerator?.release()
        toneGenerator = createToneGenerator()
    }

    fun updateSoundType(newType: BellSoundType) {
        currentSoundType = newType
        toneGenerator?.release()
        toneGenerator = createToneGenerator()
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
