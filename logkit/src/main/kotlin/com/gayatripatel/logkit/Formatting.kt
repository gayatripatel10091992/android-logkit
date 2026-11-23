package com.gayatripatel.logkit

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

public fun interface LogFormatter {
    public fun format(entry: LogEntry): String
}

/** `2026-11-23 09:30:12.345 I/Billing [main]: message` followed by the stack trace, if any. */
public class DefaultLogFormatter(
    private val timeZone: TimeZone = TimeZone.getDefault(),
) : LogFormatter {

    // SimpleDateFormat is not thread-safe; ThreadLocal keeps one per thread (works on minSdk 21).
    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply { timeZone = this@DefaultLogFormatter.timeZone }
    }

    override fun format(entry: LogEntry): String = buildString {
        append(dateFormat.get()!!.format(Date(entry.timestampMillis)))
        append(' ').append(entry.level.letter).append('/').append(entry.tag)
        append(" [").append(entry.threadName).append("]: ")
        append(entry.message)
        entry.throwable?.let { append('\n').append(it.stackTraceToString().trimEnd()) }
    }
}

/** Removes personal data before it reaches any sink. */
public fun interface Redactor {
    public fun redact(message: String): String

    public companion object {
        public val None: Redactor = Redactor { it }
    }
}

/**
 * Masks e-mail addresses, bearer tokens and phone numbers (10+ digits, so dates and durations survive).
 * Phone numbers keep their last two digits to stay debuggable: `+1 415 555 0134` -> `[phone:…34]`.
 */
public class PiiRedactor : Redactor {
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val bearer = Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*")
    private val phone = Regex("(?<![\\w])\\+?\\d[\\d\\s().-]{7,}\\d(?![\\w])")

    override fun redact(message: String): String = message
        .replace(email, "[email]")
        .replace(bearer, "\$1[token]")
        .replace(phone) { match ->
            val digits = match.value.filter(Char::isDigit)
            if (digits.length >= 10) "[phone:…${digits.takeLast(2)}]" else match.value
        }
}
