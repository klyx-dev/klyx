package com.klyx.core.process

import kotlinx.atomicfu.atomic

/**
 * Represents a thread of execution in a program. Threads allow concurrent execution
 * of code segments within a single application.
 *
 * This class provides methods to manage the lifecycle and properties of threads,
 * including starting, stopping, and querying thread state information.
 */
expect class Thread {
    /**
     * The name of this thread.
     */
    var name: String

    /**
     * The unique identifier for this thread.
     */
    val id: Long

    /**
     * Whether this thread is a daemon thread.
     * Daemon threads do not prevent the process from exiting.
     */
    var isDaemon: Boolean

    /**
     * The priority of this thread (1-10, where 5 is normal).
     */
    var priority: Int

    /**
     * Whether this thread is currently alive (started and not yet terminated).
     */
    val isAlive: Boolean

    /**
     * Starts the execution of this thread.
     */
    fun start()

    /**
     * Waits for this thread to terminate.
     */
    fun join()

    /**
     * Waits for this thread to terminate with a timeout.
     * @param millis The maximum time to wait in milliseconds.
     */
    fun join(millis: Long)

    /**
     * Interrupts this thread.
     */
    fun interrupt()

    /**
     * Whether this thread has been interrupted.
     */
    val isInterrupted: Boolean

    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int

    companion object {
        /**
         * Creates a new thread with the given name and block to execute.
         */
        fun create(name: String? = null, isDaemon: Boolean = false, block: () -> Unit): Thread

        /**
         * Returns the currently executing thread.
         */
        fun currentThread(): Thread

        /**
         * Causes the currently executing thread to sleep for the specified milliseconds.
         */
        fun sleep(millis: Long)

        /**
         * Yields the current thread to allow other threads to execute.
         */
        fun yield()

        /**
         * Returns the number of available processors.
         */
        fun availableProcessors(): Int
    }
}

fun thread(
    start: Boolean = true,
    name: String? = null,
    isDaemon: Boolean = false,
    priority: Int = 5,
    block: () -> Unit
): Thread {
    val thread = Thread.create(name ?: "Thread-${ThreadIdGenerator.nextId()}", isDaemon, block)
    thread.priority = priority
    if (start) thread.start()
    return thread
}

@PublishedApi
internal object ThreadIdGenerator {
    private val counter = atomic(0L)

    fun nextId(): Long = counter.incrementAndGet()
}
