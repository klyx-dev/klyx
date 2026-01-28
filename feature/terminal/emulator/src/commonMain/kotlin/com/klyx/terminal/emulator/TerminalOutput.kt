package com.klyx.terminal.emulator

/**
 * A client which receives callbacks from events triggered by feeding input to a [TerminalEmulator].
 */
interface TerminalOutput {

    /**
     * Write a string using the UTF-8 encoding to the terminal client.
     */
    suspend fun write(data: String) {
        val bytes = data.encodeToByteArray()
        write(bytes, 0, bytes.size)
    }

    /**
     * Write bytes to the terminal client.
     */
    suspend fun write(data: ByteArray, offset: Int = 0, count: Int = data.size - offset)

    /**
     * Notify the terminal client that the terminal title has changed.
     */
    fun titleChanged(oldTitle: String?, newTitle: String?)

    /**
     * Notify the terminal client that text should be copied to clipboard.
     */
    suspend fun onCopyTextToClipboard(text: String)

    /**
     * Notify the terminal client that text should be pasted from clipboard.
     */
    suspend fun onPasteTextFromClipboard()

    /**
     * Notify the terminal client that a bell character (ASCII 7, bell, BEL, \a, ^G)) has been received.
     */
    fun onBell()

    fun onColorsChanged()
}
