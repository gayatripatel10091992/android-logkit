package com.gayatripatel.logkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class FormattingTest {

    private val redactor = PiiRedactor()

    @Test
    fun defaultFormatter_producesLogcatStyleLine() {
        val formatter = DefaultLogFormatter(TimeZone.getTimeZone("UTC"))
        val entry = LogEntry(
            timestampMillis = 1_795_415_412_345L, // 2026-11-23 06:30:12.345 UTC
            level = LogLevel.INFO,
            tag = "Billing",
            message = "Purchase acknowledged",
            threadName = "main",
        )
        assertEquals("2026-11-23 06:30:12.345 I/Billing [main]: Purchase acknowledged", formatter.format(entry))
    }

    @Test
    fun defaultFormatter_appendsStackTrace() {
        val formatter = DefaultLogFormatter(TimeZone.getTimeZone("UTC"))
        val line = formatter.format(
            LogEntry(0L, LogLevel.ERROR, "Net", "failed", IllegalStateException("timeout"), "io"),
        )
        assertTrue(line.contains("E/Net [io]: failed\njava.lang.IllegalStateException: timeout"))
    }

    @Test
    fun redactor_masksEmailTokenAndPhone() {
        val input = "User jane.doe@example.com called +44 20 7946 0958 with Bearer abc.DEF-123"
        assertEquals("User [email] called [phone:…58] with Bearer [token]", redactor.redact(input))
    }

    @Test
    fun redactor_keepsDatesDurationsAndShortNumbers() {
        val input = "Sync on 2026-11-23 took 820 ms, retry 3 of 5, build 1234567"
        assertEquals(input, redactor.redact(input))
    }
}
