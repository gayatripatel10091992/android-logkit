package com.gayatripatel.logkit

/**
 * Entry point.
 *
 * ```
 * LogKit.install {
 *     minLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.INFO
 *     sink(LogcatSink())
 *     sink(RollingFileSink(File(filesDir, "logs")))
 * }
 * private val log = LogKit.logger("Billing")
 * log.i { "Purchase acknowledged for ${sku}" }   // lambda is skipped entirely below minLevel
 * ```
 */
public object LogKit {

    @Volatile
    private var config: LogKitConfig = LogKitConfig.DEFAULT

    public fun install(block: LogKitConfig.Builder.() -> Unit) {
        config = LogKitConfig.Builder().apply(block).build()
    }

    public val minLevel: LogLevel get() = config.minLevel

    public fun isLoggable(level: LogLevel): Boolean = level >= config.minLevel

    public fun logger(tag: String): Logger = Logger(tag)

    public fun log(level: LogLevel, tag: String, throwable: Throwable? = null, message: () -> String) {
        val current = config
        if (level < current.minLevel || current.sinks.isEmpty()) return
        val entry = LogEntry(
            timestampMillis = current.clock(),
            level = level,
            tag = tag,
            message = current.redactor.redact(message()),
            throwable = throwable,
        )
        // A failing sink must never crash the app or block the other sinks.
        current.sinks.forEach { sink -> runCatching { sink.write(entry) } }
    }

    public fun flush() {
        config.sinks.forEach { sink -> runCatching { sink.flush() } }
    }
}

public class LogKitConfig internal constructor(
    public val minLevel: LogLevel,
    public val sinks: List<LogSink>,
    public val redactor: Redactor,
    internal val clock: () -> Long,
) {
    public class Builder {
        public var minLevel: LogLevel = LogLevel.DEBUG
        public var redactor: Redactor = PiiRedactor()
        internal var clock: () -> Long = { System.currentTimeMillis() }
        private val sinks = mutableListOf<LogSink>()

        public fun sink(sink: LogSink) {
            sinks += sink
        }

        internal fun build(): LogKitConfig = LogKitConfig(minLevel, sinks.toList(), redactor, clock)
    }

    internal companion object {
        val DEFAULT = LogKitConfig(LogLevel.DEBUG, emptyList(), Redactor.None) { System.currentTimeMillis() }
    }
}

/** Tagged logger. Messages are lambdas so string building costs nothing when the level is disabled. */
public class Logger internal constructor(public val tag: String) {
    public fun v(message: () -> String): Unit = LogKit.log(LogLevel.VERBOSE, tag, null, message)
    public fun d(message: () -> String): Unit = LogKit.log(LogLevel.DEBUG, tag, null, message)
    public fun i(message: () -> String): Unit = LogKit.log(LogLevel.INFO, tag, null, message)
    public fun w(throwable: Throwable? = null, message: () -> String): Unit = LogKit.log(LogLevel.WARN, tag, throwable, message)
    public fun e(throwable: Throwable? = null, message: () -> String): Unit = LogKit.log(LogLevel.ERROR, tag, throwable, message)
}
