package com.klyx.terminal.ui

import android.os.Handler
import android.os.HandlerThread
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import kotlinx.coroutines.runBlocking

internal actual suspend fun PlatformTextInputSessionScope.createInputRequest(state: TerminalState): PlatformTextInputMethodRequest {
    val client = state.client
    return PlatformTextInputMethodRequest { outAttrs ->
        // Ensure that inputType is only set if Terminal is selected view with the keyboard and
        // an alternate view is not selected, like an EditText. This is necessary if an activity is
        // initially started with the alternate view or if activity is returned to from another app
        // and the alternate view was the one selected the last time.
        if (client.isTerminalSelected) {
            if (client.shouldEnforceCharBasedInput) {
                // Some keyboards seems do not reset the internal state on TYPE_NULL.
                // Affects mostly Samsung stock keyboards.
                // https://github.com/termux/termux-app/issues/686
                // However, this is not a valid value as per AOSP since `InputType.TYPE_CLASS_*` is
                // not set and it logs a warning:
                // W/InputAttributes: Unexpected input class: inputType=0x00080090 imeOptions=0x02000000
                // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:packages/inputmethods/LatinIME/java/src/com/android/inputmethod/latin/InputAttributes.java;l=79
                outAttrs.inputType =
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            } else {
                // Using InputType.NULL is the most correct input type and avoids issues with other hacks.
                //
                // Previous keyboard issues:
                // https://github.com/termux/termux-packages/issues/25
                // https://github.com/termux/termux-app/issues/87.
                // https://github.com/termux/termux-app/issues/126.
                // https://github.com/termux/termux-app/issues/137 (japanese chars and TYPE_NULL).
                outAttrs.inputType = InputType.TYPE_NULL
            }
        } else {
            // Corresponds to android:inputType="text"
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        }

        // Note that IME_ACTION_NONE cannot be used as that makes it impossible to input newlines using the on-screen
        // keyboard on Android TV (see https://github.com/termux/termux-app/issues/221).
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN

        object : BaseInputConnection(view, true) {
            override fun finishComposingText(): Boolean {
                if (state.enableKeyLogging) client.logInfo(TerminalState.LOG_TAG, "IME: finishComposingText()")
                super.finishComposingText()

                runBlocking { sendTextToTerminal(editable!!) }
                editable!!.clear()
                return true
            }

            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                if (state.enableKeyLogging) client.logInfo(
                    TerminalState.LOG_TAG,
                    "IME: commitText($text, $newCursorPosition)"
                )
                super.commitText(text, newCursorPosition)
                if (state.emulator == null) return true

                runBlocking { sendTextToTerminal(editable!!) };
                editable!!.clear()
                return true
            }

            override fun getHandler(): Handler {
                val thread = HandlerThread("Terminal [IME]").also { it.start() }
                return Handler(thread.looper)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (state.enableKeyLogging) client.logInfo(
                    TerminalState.LOG_TAG,
                    "IME: deleteSurroundingText($beforeLength, $afterLength)"
                )
                // The stock Samsung keyboard with 'Auto check spelling' enabled sends leftLength > 1.
                val deleteKey = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                for (_ in 0 until beforeLength) sendKeyEvent(deleteKey)
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            suspend fun sendTextToTerminal(text: CharSequence) {
                //stopTextSelectionMode()
                var index = 0
                val length = text.length

                while (index < length) {
                    var codePoint = Character.codePointAt(text, index)
                    index += Character.charCount(codePoint)

                    if (client.readShiftKey()) {
                        codePoint = Character.toUpperCase(codePoint)
                    }

                    var ctrlHeld = false
                    if (codePoint <= 31 && codePoint != 27) {
                        if (codePoint == '\n'.code) {
                            // The AOSP keyboard and descendants seems to send \n as text when the enter key is pressed,
                            // instead of a key event like most other keyboard apps. A terminal expects \r for the enter
                            // key (although when icrnl is enabled this doesn't make a difference - run 'stty -icrnl' to
                            // check the behaviour).
                            codePoint = '\r'.code
                        }

                        // E.g. penti keyboard for ctrl input.
                        ctrlHeld = true
                        codePoint = when (codePoint) {
                            31 -> '_'.code
                            30 -> '^'.code
                            29 -> ']'.code
                            28 -> '\\'.code
                            else -> codePoint + 96
                        }
                    }

                    state.inputCodePoint(KeyEventSource.SoftKeyboard, codePoint, ctrlHeld, false)
                }
            }
        }
    }
}
