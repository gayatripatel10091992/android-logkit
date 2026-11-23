package com.gayatripatel.logkit

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogKitTest {

    private val buffer = RingBufferSink(capacity = 10)

    @After
    fun tearDown() {
        LogKit.install { }
    }

    @Test
    fun entriesBelowMinLevel_areDropped_andTheirMessageIsNeverBuilt() {
        LogKit.install {
            minLevel = LogLevel.INFO
            sink(buffer)
        }
        var built = false
        LogKit.logger("Test").d { built = true; "debug" }
        LogKit.logger("Test").i { "info" }

        assertFalse("debug message lambda should not run", built)
        assertEquals(listOf("info"), buffer.entries.value.map { it.message })
    }

    @Test
    fun redactorRuns_beforeAnySinkSeesTheMessage() {
        LogKit.install {
            sink(buffer)
            redactor = PiiRedactor()
            clock = { 42L }
        }
        LogKit.logger("Call").i { "Dialing +1 (415) 555-0134 for jane@example.com" }

        val entry = buffer.entries.value.single()
        assertEquals("Dialing [phone:…34] for [email]", entry.message)
        assertEquals(42L, entry.timestampMillis)
        assertEquals("Call", entry.tag)
    }

    @Test
    fun failingSink_doesNotCrash_orBlockOtherSinks() {
        LogKit.install {
            sink { error("disk full") }
            sink(buffer)
        }
        LogKit.logger("Test").e(IllegalStateException("boom")) { "still logged" }

        val entry = buffer.entries.value.single()
        assertEquals(LogLevel.ERROR, entry.level)
        assertTrue(entry.throwable is IllegalStateException)
    }

    @Test
    fun ringBuffer_keepsOnlyNewestEntries() {
        LogKit.install {
            minLevel = LogLevel.VERBOSE
            sink(buffer)
        }
        repeat(15) { index -> LogKit.logger("Loop").v { "#$index" } }

        val messages = buffer.entries.value.map { it.message }
        assertEquals(10, messages.size)
        assertEquals("#5", messages.first())
        assertEquals("#14", messages.last())
    }
}
