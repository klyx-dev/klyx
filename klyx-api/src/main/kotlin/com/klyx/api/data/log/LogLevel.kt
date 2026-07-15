package com.klyx.api.data.log

/**
 * Defines the severity levels for log messages in the Klyx ecosystem.
 */
enum class LogLevel {
    /**
     * Extremely fine-grained diagnostic information, typically only useful for deep debugging
     * of library or framework internals.
     */
    TRACE,

    /**
     * Information useful during development and troubleshooting.
     * Often includes internal state transitions or detailed operation results.
     */
    DEBUG,

    /**
     * High-level information highlighting the progress of the application.
     * Should be concise and meaningful to users or administrators.
     */
    INFO,

    /**
     * Potentially harmful situations or non-critical failures that don't stop the application
     * but might require attention.
     */
    WARN,

    /**
     * Error events that might still allow the application to continue running,
     * but indicate a significant failure or unexpected condition.
     */
    ERROR
}
