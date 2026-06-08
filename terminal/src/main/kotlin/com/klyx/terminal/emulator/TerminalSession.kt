package com.klyx.terminal.emulator

import android.annotation.SuppressLint
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.compose.runtime.Stable
import com.klyx.terminal.Logger
import com.klyx.terminal.ScreenEvent
import com.klyx.terminal.native.Native
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
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.Volatile
import kotlin.system.exitProcess
import kotlin.uuid.ExperimentalUuidApi
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
    client: TerminalSessionClient,
    private val transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS
) : TerminalOutput {

    var client: TerminalSessionClient = client
        private set

    @OptIn(ExperimentalUuidApi::class)
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
                Os.readlink("/proc/$shellPid/cwd")
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

    @SuppressLint("DiscouragedPrivateApi")
    private fun wrapFileDescriptor(fileDescriptor: Int): FileDescriptor {
        val result = FileDescriptor()
        try {
            val descriptorField = try {
                FileDescriptor::class.java.getDeclaredField("descriptor")
            } catch (_: NoSuchFieldException) {
                FileDescriptor::class.java.getDeclaredField("fd")
            }
            descriptorField.isAccessible = true
            descriptorField.set(result, fileDescriptor)
        } catch (e: Exception) {
            Logger.logStackTraceWithMessage(
                client,
                LOG_TAG,
                "Error accessing FileDescriptor.descriptor private field",
                e
            )
            exitProcess(1)
        }
        return result
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
            Native.setPtyWindowSize(
                fd = terminalFileDescriptor,
                rows = rows,
                cols = columns,
                cellWidth = cellWidthPixels,
                cellHeight = cellHeightPixels
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

        val processIdArray = IntArray(1)

        terminalFileDescriptor = Native.createSubprocess(
            cmd = shellPath,
            cwd = cwd,
            args = args.toTypedArray(),
            envVars = env.toTypedArray(),
            processIdArray = processIdArray,
            rows = rows,
            columns = columns,
            cellWidth = cellWidthPixels,
            cellHeight = cellHeightPixels
        )

        shellPid = processIdArray[0]
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
                FileInputStream(wrapFileDescriptor(terminalFileDescriptor)).use { input ->
                    val buffer = ByteArray(4096)

                    while (isActive && shellPid > 0) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead <= 0) break
                        if (!processToTerminalIOQueue.write(buffer, 0, bytesRead)) break
                        withContext(Dispatchers.Main) { handleNewInput() }
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
                FileOutputStream(wrapFileDescriptor(terminalFileDescriptor)).use { output ->
                    val buffer = ByteArray(4096)
                    while (isActive && shellPid > 0) {
                        val bytesToWrite = terminalToProcessIOQueue.read(buffer, blocking = true)
                        if (bytesToWrite <= 0) break
                        output.write(buffer, 0, bytesToWrite)
                    }
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
                val exitCode = Native.waitFor(shellPid)
                withContext(Dispatchers.Main) { handleProcessExited(exitCode) }
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
            codePoint <= /* 7 bits */0b1111111 -> {
                utf8InputBuffer[bufferPosition++] = codePoint.toByte()
            }

            codePoint <= /* 11 bits */0b11111111111 -> {
                /* 110xxxxx leading byte with leading 5 bits */
                utf8InputBuffer[bufferPosition++] = (0b11000000 or (codePoint shr 6)).toByte()
                /* 10xxxxxx continuation byte with following 6 bits */
                utf8InputBuffer[bufferPosition++] =
                    (0b10000000 or (codePoint and 0b111111)).toByte()
            }

            codePoint <= /* 16 bits */0b1111111111111111 -> {
                /* 1110xxxx leading byte with leading 4 bits */
                utf8InputBuffer[bufferPosition++] = (0b11100000 or (codePoint shr 12)).toByte()
                /* 10xxxxxx continuation byte with following 6 bits */
                @Suppress("KotlinConstantConditions")
                utf8InputBuffer[bufferPosition++] =
                    (0b10000000 or ((codePoint shl 6) and 0b111111)).toByte()
                /* 10xxxxxx continuation byte with following 6 bits */
                utf8InputBuffer[bufferPosition++] =
                    (0b10000000 or (codePoint and 0b111111)).toByte()
            }

            else -> { /* We have checked codePoint <= 1114111 above, so we have max 21 bits = 0b111111111111111111111 */
                /* 11110xxx leading byte with leading 3 bits */
                utf8InputBuffer[bufferPosition++] = (0b11110000 or (codePoint shr 18)).toByte()
                /* 10xxxxxx continuation byte with following 6 bits */
                utf8InputBuffer[bufferPosition++] =
                    (0b10000000 or ((codePoint shr 12) and 0b111111)).toByte()
                /* 10xxxxxx continuation byte with following 6 bits */
                utf8InputBuffer[bufferPosition++] =
                    (0b10000000 or ((codePoint shr 6) and 0b111111)).toByte()
                /* 10xxxxxx continuation byte with following 6 bits */
                utf8InputBuffer[bufferPosition++] =
                    (0b10000000 or (codePoint and 0b111111)).toByte()
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
            try {
                Os.kill(shellPid, OsConstants.SIGKILL)
            } catch (e: ErrnoException) {
                Logger.logWarn(client, LOG_TAG, "Failed sending SIGKILL: " + e.message);
            }
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

        terminalToProcessIOQueue.close()
        processToTerminalIOQueue.close()
        Native.close(terminalFileDescriptor)

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
