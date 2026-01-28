package com.klyx.terminal.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.klyx.terminal.emulator.KeyHandler
import com.klyx.terminal.emulator.KeyMod
import com.klyx.terminal.emulator.TermKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline

internal fun TerminalState.handleKeyEvent(event: KeyEvent, coroutineScope: CoroutineScope): Boolean {
    val emulator = emulator

    when (event.type) {
        KeyEventType.KeyDown -> {
            if (enableKeyLogging) {
                client.logInfo(
                    TerminalState.LOG_TAG,
                    "[Terminal] onKeyDown(isSystem=${event.isSystem()}, event=$event)"
                )
            }

            if (emulator == null) return true

            if (isSelectingText.value) {
                stopTextSelection()
            }

            if (client.onKeyDown(event.key, event, session)) {
                invalidate()
                return true
            } else if (event.isSystem() && (!client.shouldBackButtonBeMappedToEscape || event.key != Key.Back)) {
                return false
            }

            val ctrl = event.isCtrlPressed || client.readControlKey()
            val alt = event.isAltPressed || client.readAltKey()
            val shift = event.isShiftPressed || client.readShiftKey()

            var keyMod = 0
            if (ctrl) keyMod = keyMod or KeyMod.CTRL
            if (alt) keyMod = keyMod or KeyMod.ALT
            if (shift) keyMod = keyMod or KeyMod.SHIFT
            if (event.isNumLockOn) keyMod = keyMod or KeyMod.NUM_LOCK

            // https://github.com/termux/termux-app/issues/731
            if (!event.isFnPressed && handleKey(event.key, keyMod, coroutineScope)) {
                if (enableKeyLogging) {
                    client.logInfo(TerminalState.LOG_TAG, "[Terminal] handleKey() took key event")
                }
                return true
            }

            val resolvedKey = event.resolveUnicodeKey(client)
            if (enableKeyLogging) {
                client.logInfo(TerminalState.LOG_TAG, "[Terminal KeyEvent] resolvedKey=$resolvedKey")
            }
            if (resolvedKey == null) return false

            // TODO: Make async - Currently, inputCodePoint() is called within coroutineScope.launch{} which already runs asynchronously,
            // but the function returns `true` immediately before the async operation completes. This may cause issues if the caller
            // expects the key input to be processed synchronously.
            coroutineScope.launch {
                inputCodePoint(
                    eventSource = KeyEventSource(event.deviceId()),
                    codePoint = resolvedKey.codePoint,
                    ctrlDownFromEvent = ctrl,
                    leftAltDownFromEvent = alt
                )
            }
            return true
        }

        KeyEventType.KeyUp -> {
            if (enableKeyLogging) {
                client.logInfo(TerminalState.LOG_TAG, "[Terminal] onKeyUp(event=$event)")
            }

            // Do not return for Key.Back and send it to the client since user may be trying
            // to exit the activity.
            if (emulator == null && event.key != Key.Back) return true

            if (client.onKeyUp(event.key, event)) {
                invalidate()
                return true
            } else if (event.isSystem()) {
                // Let system key events through.
                return false
            }

            return true
        }
    }
    return false
}

internal fun TerminalState.handleKey(key: Key, keyMod: Int, coroutineScope: CoroutineScope): Boolean {
    // Ensure cursor is shown when a key is pressed down like long hold on (arrow) keys
    if (emulator != null) {
        emulator!!.cursorBlinkState = true
    }

    if (handleKeyAction(key, keyMod)) return true

    val emulator = checkNotNull(session.emulator) { "TerminalEmulator should not be null at this point." }
    val code = KeyHandler.getCode(
        termKey = TermKey(key = key, mods = keyMod),
        cursorApp = emulator.isCursorKeysApplicationMode,
        keypadApp = emulator.isKeypadApplicationMode
    ) ?: return false
    coroutineScope.launch { session.write(code) }
    return true
}

private fun handleKeyAction(key: Key, keyMod: Int): Boolean {
    val shift = (keyMod and KeyMod.SHIFT) != 0

    when (key) {
        Key.PageUp, Key.PageDown -> {
            // shift+page_up and shift+page_down should scroll scrollback history instead of
            // scrolling command history or changing pages
            if (shift) {
                // TODO: scroll
                //doScroll()
            }
            return true
        }
    }
    return false
}

@JvmInline
internal value class ResolvedKey(val codePoint: Int)

internal expect fun KeyEvent.resolveUnicodeKey(client: TerminalClient): ResolvedKey?
