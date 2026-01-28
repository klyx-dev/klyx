package com.klyx.terminal.emulator

import androidx.compose.runtime.Stable
import com.klyx.terminal.Logger
import com.klyx.terminal.ScreenEvent
import com.klyx.terminal.native.closeFd
import com.klyx.terminal.native.createSubprocess
import com.klyx.terminal.native.killProcess
import com.klyx.terminal.native.readFromFd
import com.klyx.terminal.native.readSymlink
import com.klyx.terminal.native.setPtyWindowSize
import com.klyx.terminal.native.waitFor
import com.klyx.terminal.native.writeToFd
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

/**
 * A terminal session, consisting of a process coupled to a terminal interface.
 *
 * The subprocess will be executed by the constructor, and when the size is made known by a call to
 * [updateSize] terminal emulation will begin and threads will be spawned to handle the subprocess I/O.
 * All terminal emulation and callback methods will be performed on the main thread.
 *
 * The child process may be exited forcefully by using the [finishIfRunning] method.
 *
 * NOTE: The terminal session may outlive the EmulatorView, so be careful with callbacks!
 *
 * *References: https://github.com/termux/termux-app/blob/master/terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java*
 */
@Stable
open class TerminalSession(
    private val shellPath: String,
    private val cwd: String,
    private val args: List<String>,
    private val env: List<String>,
    private val transcriptRows: Int,
    client: TerminalSessionClient
) : TerminalOutput {

    var client: TerminalSessionClient = client
        private set
    val handle = Uuid.random().toHexDashString()

    var emulator: TerminalEmulator? = null
        private set

    /** Queue for process output to terminal */
    private val processToTerminalIOQueue = ByteQueue(4096)

    /** Queue for terminal input to process */
    private val terminalToProcessIOQueue = ByteQueue(4096)

    /** Buffer to translate code points into UTF-8 */
    private val utf8InputBuffer = ByteArray(5)

    /** The pid of the shell process. 0 if not started and -1 if finished running. */
    @Volatile
    private var shellPid: Int = 0

    /** The exit status of the shell process. Only valid if shellPid is -1. */
    @Volatile
    private var shellExitStatus: Int = 0

    /** The file descriptor referencing the master half of a pseudo-terminal pair */
    private var terminalFileDescriptor: Int = 0

    /** Set by the application for user identification of session */
    var sessionName: String? = null

    /**
     * The terminal title as set through escape sequences or null if none set.
     */
    val title: String? get() = emulator?.title

    /**
     * Only valid if not running.
     */
    @get:Synchronized
    val exitStatus: Int
        get() = shellExitStatus

    val pid get() = shellPid

    /**
     * Returns the shell's working directory or null if it was unavailable.
     */
    val cwdOrNull: String?
        get() {
            if (shellPid < 1) return null

            return try {
                readSymlink("/proc/$shellPid/cwd")
            } catch (e: Exception) {
                Logger.logError(client, LOG_TAG, "Error getting current directory: ${e.message}")
                null
            }
        }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val isRunning: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val screenEvents: SharedFlow<ScreenEvent>
        field = MutableSharedFlow(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    companion object {
        private const val LOG_TAG = "TerminalSession"
    }

    /**
     * Update the terminal session client.
     */
    fun updateTerminalSessionClient(client: TerminalSessionClient) {
        this.client = client
        emulator?.updateTerminalSessionClient(client)
    }

    /**
     * Inform the attached pty of the new size and reflow or initialize the emulator.
     */
    fun updateSize(columns: Int, rows: Int, cellWidthPixels: Int, cellHeightPixels: Int) {
        val currentEmulator = emulator
        if (currentEmulator == null) {
            initializeEmulator(columns, rows, cellWidthPixels, cellHeightPixels)
        } else {
            setPtyWindowSize(
                fd = terminalFileDescriptor,
                rows = rows.toUInt(),
                cols = columns.toUInt(),
                cellWidth = cellWidthPixels.toUInt(),
                cellHeight = cellHeightPixels.toUInt()
            )
            currentEmulator.resize(columns, rows, cellWidthPixels, cellHeightPixels)
        }
    }

    /**
     * Set the terminal emulator's window size and start terminal emulation.
     */
    fun initializeEmulator(columns: Int, rows: Int, cellWidthPixels: Int, cellHeightPixels: Int) {
        emulator = TerminalEmulator(
            session = this,
            columns = columns,
            rows = rows,
            cellWidthPixels = cellWidthPixels,
            cellHeightPixels = cellHeightPixels,
            transcriptRows = transcriptRows,
            client = client
        )

        val result = createSubprocess(
            cmd = shellPath,
            cwd = cwd,
            args = args,
            envVars = env,
            rows = rows,
            columns = columns,
            cellWidth = cellWidthPixels,
            cellHeight = cellHeightPixels
        )

        terminalFileDescriptor = result.ptmFd
        shellPid = result.pid
        isRunning.update { true }

        client.setTerminalShellPid(this, shellPid)

        startInputReader()
        startOutputWriter()
        startProcessWaiter()
    }

    /**
     * Coroutine to read from process and write to terminal
     */
    private fun startInputReader() {
        scope.launch(Dispatchers.IO) {
            try {
                while (isActive && shellPid > 0) {
                    val data = readFromFd(
                        fd = terminalFileDescriptor,
                        maxLen = 4096u
                    )
                    if (data.isEmpty()) break

                    if (!processToTerminalIOQueue.write(data, 0, data.size)) break

                    // Notify on main thread
                    withContext(Dispatchers.Main) {
                        handleNewInput()
                    }
                }
            } catch (e: Exception) {
                Logger.logError(client, LOG_TAG, "Input reader error: ${e.message}")
            }
        }
    }

    /**
     * Coroutine to read from terminal and write to process
     */
    private fun startOutputWriter() {
        scope.launch(Dispatchers.IO) {
            try {
                val buffer = ByteArray(4096)
                while (isActive && shellPid > 0) {
                    val bytesToWrite = terminalToProcessIOQueue.read(buffer, blocking = true)
                    if (bytesToWrite <= 0) break

                    val data = buffer.copyOf(bytesToWrite)
                    writeToFd(terminalFileDescriptor, data)
                }
            } catch (e: Exception) {
                Logger.logError(client, LOG_TAG, "Output writer error: ${e.message}")
            }
        }
    }

    /**
     * Coroutine to wait for process exit
     */
    private fun startProcessWaiter() {
        scope.launch(Dispatchers.IO) {
            try {
                val exitCode = waitFor(shellPid)

                withContext(Dispatchers.Main) {
                    handleProcessExited(exitCode)
                }
            } catch (e: Exception) {
                Logger.logError(client, LOG_TAG, "Process waiter error: ${e.message}")
            }
        }
    }

    private suspend fun handleProcessExited(exitCode: Int) {
        cleanupResources(exitCode)

        val exitDescription = buildString {
            append("\r\n[Process completed")
            when {
                exitCode > 0 -> append(" (code $exitCode)")
                exitCode < 0 -> append(" (signal ${-exitCode})")
            }
            append(" - press Enter]")
        }

        val bytesToWrite = exitDescription.encodeToByteArray()
        emulator?.append(bytesToWrite, bytesToWrite.size)
        notifyScreenUpdate()

        client.onSessionFinished(this)
    }

    /**
     * Handle new input from process
     */
    private suspend fun handleNewInput() {
        val buffer = ByteArray(4 * 1024)
        val bytesRead = processToTerminalIOQueue.read(buffer, blocking = false)

        if (bytesRead > 0) {
            emulator?.append(buffer, bytesRead)
            notifyScreenUpdate()
        }
    }

    /**
     * Write the Unicode code point to the terminal encoded in UTF-8.
     */
    suspend fun writeCodePoint(prependEscape: Boolean, codePoint: Int) {
        require(codePoint <= 0x10FFFF && (codePoint !in 0xD800..0xDFFF)) {
            // 1114111 (= 2**16 + 1024**2 - 1) is the highest code point, [0xD800,0xDFFF] is the surrogate range.
            "Invalid code point: $codePoint"
        }

        var bufferPosition = 0
        if (prependEscape) utf8InputBuffer[bufferPosition++] = 27

        when {
            codePoint <= 0x7F -> {
                // 7 bits - single byte
                utf8InputBuffer[bufferPosition++] = codePoint.toByte()
            }

            codePoint <= 0x7FF -> {
                // 11 bits - two bytes
                utf8InputBuffer[bufferPosition++] = (0xC0 or (codePoint shr 6)).toByte()
                utf8InputBuffer[bufferPosition++] = (0x80 or (codePoint and 0x3F)).toByte()
            }

            codePoint <= 0xFFFF -> {
                // 16 bits - three bytes
                utf8InputBuffer[bufferPosition++] = (0xE0 or (codePoint shr 12)).toByte()
                utf8InputBuffer[bufferPosition++] = (0x80 or ((codePoint shr 6) and 0x3F)).toByte()
                utf8InputBuffer[bufferPosition++] = (0x80 or (codePoint and 0x3F)).toByte()
            }

            else -> {
                // 21 bits - four bytes
                utf8InputBuffer[bufferPosition++] = (0xF0 or (codePoint shr 18)).toByte()
                utf8InputBuffer[bufferPosition++] = (0x80 or ((codePoint shr 12) and 0x3F)).toByte()
                utf8InputBuffer[bufferPosition++] = (0x80 or ((codePoint shr 6) and 0x3F)).toByte()
                utf8InputBuffer[bufferPosition++] = (0x80 or (codePoint and 0x3F)).toByte()
            }
        }

        write(utf8InputBuffer, 0, bufferPosition)
    }

    /**
     * Notify the client that the screen has changed.
     */
    protected fun notifyScreenUpdate() {
        screenEvents.tryEmit(ScreenEvent.ContentChanged(skipScrolling = false))
        client.onTextChanged(this)
    }

    /**
     * Reset state for terminal emulator state.
     */
    fun reset() {
        emulator?.reset()
        notifyScreenUpdate()
    }

    /**
     * Finish this terminal session by sending SIGKILL to the shell.
     */
    fun finishIfRunning() {
        if (shellPid > 0) {
            killProcess(shellPid, 9) // SIGKILL
        }
    }

    /**
     * Cleanup resources when the process exits.
     */
    internal suspend fun cleanupResources(exitStatus: Int) {
        synchronized(this) {
            shellPid = -1
            shellExitStatus = exitStatus
            isRunning.update { false }
        }

        // Stop the coroutines and close the I/O streams
        terminalToProcessIOQueue.close()
        processToTerminalIOQueue.close()
        Logger.logDebug(client, "TerminalSession", "Kotlin closing fd $terminalFileDescriptor")
        closeFd(terminalFileDescriptor)

        scope.cancel()
    }

    override suspend fun write(data: ByteArray, offset: Int, count: Int) {
        if (shellPid > 0) {
            terminalToProcessIOQueue.write(data, offset, count)
        }
    }

    override fun titleChanged(oldTitle: String?, newTitle: String?) {
        client.onTitleChanged(this)
    }

    override suspend fun onCopyTextToClipboard(text: String) {
        client.onCopyTextToClipboard(this, text)
    }

    override suspend fun onPasteTextFromClipboard() {
        client.onPasteTextFromClipboard(this)
    }

    override fun onBell() {
        client.onBell(this)
    }

    override fun onColorsChanged() {
        client.onColorsChanged(this)
    }
}
