package com.klyx.core.process

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970
import platform.posix.pthread_getname_np
import platform.posix.pthread_self
import platform.posix.pthread_setname_np
import platform.posix.pthread_t

@OptIn(ExperimentalForeignApi::class)
actual class Thread private constructor(
    private val nsThread: NSThread?,
    private val pthread: pthread_t?
) {

    private var _name by atomic("")
    private var _isDaemon by atomic(false)
    private var _priority by atomic(5)
    private var _isAlive by atomic(false)
    private var _isInterrupted by atomic(false)
    private val _id = ThreadIdGenerator.nextId()

    actual var name: String
        get() = nsThread?.name ?: run {
            val p = pthread ?: return _name

            val buffer = ByteArray(65)
            val result = pthread_getname_np(p, buffer.refTo(0), buffer.size.convert())
            if (result != 0) return _name

            buffer
                .takeWhile { it != 0.toByte() }
                .toByteArray()
                .toKString()
        }
        set(value) {
            _name = value
            nsThread?.name = value
            pthread?.let {
                pthread_setname_np(value)
            }
        }

    actual val id: Long
        get() = nsThread?.hash?.toLong() ?: _id

    actual var isDaemon: Boolean
        get() = _isDaemon
        set(value) {
            _isDaemon = value
        }

    actual var priority: Int
        get() = nsThread?.threadPriority?.toInt() ?: _priority
        set(value) {
            _priority = value.coerceIn(1, 10)
            nsThread?.threadPriority = (value / 10.0)
        }

    actual val isAlive: Boolean
        get() = nsThread?.isExecuting() ?: _isAlive

    actual fun start() {
        nsThread?.start()
        _isAlive = true
    }

    actual fun join() {
        while (isAlive) sleep(10)
    }

    actual fun join(millis: Long) {
        val startTime = currentTimeMillis()
        while (isAlive && (currentTimeMillis() - startTime) < millis) sleep(10)
    }

    actual fun interrupt() {
        _isInterrupted = true
        nsThread?.cancel()
    }

    actual val isInterrupted: Boolean
        get() = _isInterrupted || nsThread?.isCancelled() == true

    private fun currentTimeMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000).toLong()
    }

    private fun sleep(millis: Long) {
        NSThread.sleepForTimeInterval(millis / 1000.0)
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Thread) return false
        return nsThread == other.nsThread && pthread == other.pthread
    }

    actual override fun hashCode() = nsThread?.hash?.toInt() ?: _id.toInt()

    actual override fun toString(): String {
        return "Thread(name=$name, id=$id, priority=$priority, daemon=$isDaemon, alive=$isAlive)"
    }

    actual companion object {
        actual fun create(name: String?, isDaemon: Boolean, block: () -> Unit): Thread {
            val thread = NSThread {
                try {
                    block()
                } catch (e: Throwable) {
                    NSLog("Thread $name threw exception: ${e.message}")
                }
            }
            if (name != null)
                thread.name = name
            return Thread(thread, null).apply { this.isDaemon = isDaemon }
        }

        actual fun currentThread(): Thread {
            val current = NSThread.currentThread
            return Thread(current, pthread_self())
        }

        actual fun sleep(millis: Long) = NSThread.sleepForTimeInterval(millis / 1000.0)

        actual fun yield() = NSThread.sleepForTimeInterval(0.0)

        actual fun availableProcessors() = NSProcessInfo.processInfo.activeProcessorCount.toInt()
    }
}
