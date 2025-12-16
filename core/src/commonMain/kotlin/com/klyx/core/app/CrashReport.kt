package com.klyx.core.app

import com.klyx.core.platform.Platform
import com.klyx.core.process.Thread
import com.klyx.platform.PlatformInfo
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class CrashReport(
    val timestamp: LocalDateTime,
    val threadName: String,
    val threadId: Long,
    val message: String?,
    val exception: String,
    val stacktrace: String,
    val breadcrumbs: List<String>,
    val buildInfo: BuildInfo,
    val platform: Platform
) {
    override fun toString(): String = buildString {
        appendLine("=== Crash Report ===")
        appendLine("Time: $timestamp")
        appendLine("Platform: $platform")
        appendLine("OS Version: ${PlatformInfo.version}")
        appendLine()
        appendLine("App Version: ${buildInfo.versionName} (${buildInfo.versionCode})")
        appendLine("Build Type: ${buildInfo.buildType}")
        appendLine()
        appendLine("Thread: $threadName ($threadId)")
        appendLine("Exception: $exception")
        appendLine("Message: ${message ?: "No message"}")

        if (breadcrumbs.isNotEmpty()) {
            appendLine()
            appendLine("=== Breadcrumbs ===")
            breadcrumbs.forEach { appendLine(it) }
        }

        appendLine()
        appendLine("=== Build Info ===")
        appendLine("Commit: ${buildInfo.gitCommit ?: "unknown"}")
        appendLine("Branch: ${buildInfo.gitBranch ?: "unknown"}")
        appendLine("Build Time: ${buildInfo.buildTimestamp}")
        appendLine("Kotlin: ${buildInfo.kotlinVersion}")
        appendLine()
        appendLine("Stack Trace:")
        appendLine(stacktrace)
    }

    companion object {
        operator fun invoke(thread: Thread, throwable: Throwable): CrashReport {
            return createCrashReport(thread, throwable)
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun createCrashReport(thread: Thread, throwable: Throwable): CrashReport {
    val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    return CrashReport(
        timestamp = timestamp,
        threadName = thread.name,
        threadId = thread.id,
        message = throwable.message,
        exception = throwable::class.simpleName ?: "Unknown",
        stacktrace = throwable.stackTraceToString(),
        breadcrumbs = Breadcrumbs.snapshot(),
        buildInfo = BuildInfo.current(),
        platform = Platform.Current
    )
}
