package com.klyx.core.process

fun java.lang.Thread.wrap() = Thread(javaThread = this)

actual class Thread(private val javaThread: java.lang.Thread) {
    actual var name: String
        get() = javaThread.name
        set(value) {
            javaThread.name = value
        }

    actual val id: Long
        get() = javaThread.threadId()

    actual var isDaemon: Boolean
        get() = javaThread.isDaemon
        set(value) {
            javaThread.isDaemon = value
        }

    actual var priority: Int
        get() = javaThread.priority
        set(value) {
            javaThread.priority = value.coerceIn(java.lang.Thread.MIN_PRIORITY, java.lang.Thread.MAX_PRIORITY)
        }

    actual val isAlive: Boolean
        get() = javaThread.isAlive

    actual fun start() {
        javaThread.start()
    }

    actual fun join() {
        javaThread.join()
    }

    actual fun join(millis: Long) {
        javaThread.join(millis)
    }

    actual fun interrupt() {
        javaThread.interrupt()
    }

    actual val isInterrupted: Boolean
        get() = javaThread.isInterrupted

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Thread) return false
        return javaThread == other.javaThread
    }

    actual override fun hashCode() = javaThread.hashCode()

    actual override fun toString(): String {
        return "Thread(name=$name, id=$id, priority=$priority, daemon=$isDaemon, alive=$isAlive)"
    }

    actual companion object {
        actual fun create(name: String?, isDaemon: Boolean, block: () -> Unit): Thread {
            val thread = object : java.lang.Thread() {
                override fun run() {
                    block()
                }
            }
            if (name != null)
                thread.name = name
            if (isDaemon)
                thread.isDaemon = true
            return Thread(javaThread = thread)
        }

        actual fun currentThread() = Thread(javaThread = java.lang.Thread.currentThread())

        actual fun sleep(millis: Long) = java.lang.Thread.sleep(millis)

        actual fun yield() = java.lang.Thread.yield()

        actual fun availableProcessors() = Runtime.getRuntime().availableProcessors()
    }
}
