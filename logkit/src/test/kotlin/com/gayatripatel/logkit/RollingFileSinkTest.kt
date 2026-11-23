package com.gayatripatel.logkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.Executor

class RollingFileSinkTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val directExecutor = Executor { it.run() }
    private val plainFormatter = LogFormatter { it.message }

    private fun entry(message: String) = LogEntry(0L, LogLevel.INFO, "T", message, threadName = "t")

    @Test
    fun writesLinesToCurrentFile() {
        val sink = RollingFileSink(temp.root, maxFileBytes = 1_000, formatter = plainFormatter, executor = directExecutor)

        sink.write(entry("one"))
        sink.write(entry("two"))

        assertEquals("one\ntwo\n", sink.currentFile.readText())
    }

    @Test
    fun rotatesWhenFull_andKeepsAtMostMaxFiles() {
        val sink = RollingFileSink(
            directory = temp.root,
            maxFileBytes = 10, // each line is 6 bytes: one line per file
            maxFiles = 3,
            formatter = plainFormatter,
            executor = directExecutor,
        )

        listOf("line1", "line2", "line3", "line4").forEach { sink.write(entry(it)) }

        val files = sink.files()
        assertEquals(3, files.size)
        assertEquals("line4\n", files[0].readText())
        assertEquals("line3\n", files[1].readText())
        assertEquals("line2\n", files[2].readText())
        assertTrue(files.none { it.readText().contains("line1") })
    }
}
