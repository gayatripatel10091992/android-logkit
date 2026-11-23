package com.gayatripatel.logkit

public enum class LogLevel(public val priority: Int, public val letter: Char) {
    VERBOSE(2, 'V'),
    DEBUG(3, 'D'),
    INFO(4, 'I'),
    WARN(5, 'W'),
    ERROR(6, 'E'),
}

public data class LogEntry(
    public val timestampMillis: Long,
    public val level: LogLevel,
    public val tag: String,
    public val message: String,
    public val throwable: Throwable? = null,
    public val threadName: String = Thread.currentThread().name,
)

/** Destination for log entries. Implementations must be thread-safe. */
public fun interface LogSink {
    public fun write(entry: LogEntry)

    /** Blocks until buffered entries are persisted. No-op by default. */
    public fun flush() {}
}
