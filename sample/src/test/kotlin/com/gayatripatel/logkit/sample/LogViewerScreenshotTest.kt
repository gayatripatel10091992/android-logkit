package com.gayatripatel.logkit.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayatripatel.logkit.LogEntry
import com.gayatripatel.logkit.LogLevel
import com.gayatripatel.logkit.PiiRedactor
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class LogViewerScreenshotTest {

    private val redactor = PiiRedactor()
    private val start = 1_795_415_412_000L

    private val entries = listOf(
        Triple(LogLevel.DEBUG, "SIP", "REGISTER sip:alice@voip.example.com -> 200 OK (expires 3600)"),
        Triple(LogLevel.INFO, "Call", "Dialing +1 (415) 555-0134 from contact sheet"),
        Triple(LogLevel.VERBOSE, "Audio", "Codec negotiated: OPUS 48 kHz, ptime 20 ms"),
        Triple(LogLevel.INFO, "Audio", "Audio route changed: EARPIECE -> SPEAKER"),
        Triple(LogLevel.INFO, "Call", "Call connected in 820 ms"),
        Triple(LogLevel.WARN, "Audio", "Jitter 48 ms above 30 ms threshold, enabling FEC"),
        Triple(LogLevel.ERROR, "Call", "Call dropped after 01:42, reconnecting"),
        Triple(LogLevel.INFO, "Call", "Reconnected, sending summary to support@example.com"),
    ).mapIndexed { index, (level, tag, message) ->
        LogEntry(
            timestampMillis = start + index * 137L,
            level = level,
            tag = tag,
            message = redactor.redact(message),
            throwable = if (level == LogLevel.ERROR) IOException("Socket closed by peer") else null,
            threadName = if (tag == "Audio") "audio-io" else "main",
        )
    }

    @Test
    fun logViewer_light() {
        captureRoboImage("build/outputs/roborazzi/01_log_viewer_light.png") {
            SampleTheme(darkTheme = false) { Screen(LogLevel.VERBOSE) }
        }
    }

    @Test
    fun logViewer_dark() {
        captureRoboImage("build/outputs/roborazzi/02_log_viewer_dark.png") {
            SampleTheme(darkTheme = true) { Screen(LogLevel.VERBOSE) }
        }
    }

    @Test
    fun logViewer_warningsAndErrorsOnly() {
        captureRoboImage("build/outputs/roborazzi/03_log_viewer_warn_filter.png") {
            SampleTheme(darkTheme = false) { Screen(LogLevel.WARN) }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Screen(minLevel: LogLevel) {
        LogViewerScreen(
            entries = entries,
            minLevel = minLevel,
            logFileSummary = "2 log file(s) · 14.6 KB on disk",
            onMinLevelChange = {},
            onSimulateCall = {},
            onClear = {},
            timeZone = TimeZone.getTimeZone("UTC"),
        )
    }
}
