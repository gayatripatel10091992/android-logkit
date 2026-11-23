package com.gayatripatel.logkit

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Writes to Logcat, splitting messages longer than Logcat's ~4 KB line limit. */
public class LogcatSink : LogSink {
    override fun write(entry: LogEntry) {
        val text = buildString {
            append(entry.message)
            entry.throwable?.let { append('\n').append(Log.getStackTraceString(it)) }
        }
        text.chunked(MAX_CHUNK).forEach { Log.println(entry.level.priority, entry.tag, it) }
    }

    private companion object {
        const val MAX_CHUNK = 4000
    }
}

/** Keeps the last [capacity] entries in memory and exposes them as a [StateFlow] for an in-app log viewer. */
public class RingBufferSink(private val capacity: Int = 500) : LogSink {
    init {
        require(capacity > 0) { "capacity must be > 0" }
    }

    private val lock = Any()
    private val buffer = ArrayDeque<LogEntry>(capacity)
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())

    public val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    override fun write(entry: LogEntry) {
        synchronized(lock) {
            if (buffer.size == capacity) buffer.removeFirst()
            buffer.addLast(entry)
            _entries.value = buffer.toList()
        }
    }

    public fun clear() {
        synchronized(lock) {
            buffer.clear()
            _entries.value = emptyList()
        }
    }
}

/**
 * Appends formatted lines to `app.log` off the calling thread and rotates to `app.log.1`, `app.log.2`, …
 * when the file exceeds [maxFileBytes]. Keeps at most [maxFiles] files.
 */
public class RollingFileSink(
    private val directory: File,
    private val fileName: String = "app.log",
    private val maxFileBytes: Long = 512L * 1024,
    private val maxFiles: Int = 3,
    private val formatter: LogFormatter = DefaultLogFormatter(),
    private val executor: Executor = defaultExecutor(),
) : LogSink {

    init {
        require(maxFiles >= 1) { "maxFiles must be >= 1" }
        require(maxFileBytes > 0) { "maxFileBytes must be > 0" }
    }

    public val currentFile: File get() = File(directory, fileName)

    /** Existing log files, newest first. Handy for "share logs" in a support screen. */
    public fun files(): List<File> = (0 until maxFiles).map(::fileFor).filter(File::exists)

    override fun write(entry: LogEntry) {
        val line = formatter.format(entry) + "\n"
        executor.execute { append(line) }
    }

    override fun flush() {
        val latch = CountDownLatch(1)
        executor.execute { latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)
    }

    private fun fileFor(index: Int): File = if (index == 0) currentFile else File(directory, "$fileName.$index")

    private fun append(line: String) {
        runCatching {
            directory.mkdirs()
            val bytes = line.toByteArray(Charsets.UTF_8)
            if (currentFile.exists() && currentFile.length() + bytes.size > maxFileBytes) rotate()
            FileOutputStream(currentFile, true).use { it.write(bytes) }
        }
    }

    private fun rotate() {
        fileFor(maxFiles - 1).delete()
        for (index in maxFiles - 2 downTo 0) {
            val source = fileFor(index)
            if (source.exists()) source.renameTo(fileFor(index + 1))
        }
    }

    private companion object {
        fun defaultExecutor(): ExecutorService = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "logkit-file").apply { isDaemon = true }
        }
    }
}
