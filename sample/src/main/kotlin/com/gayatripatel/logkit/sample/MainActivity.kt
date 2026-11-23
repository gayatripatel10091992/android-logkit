package com.gayatripatel.logkit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayatripatel.logkit.LogKit
import com.gayatripatel.logkit.LogLevel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val entries by SampleApplication.inAppBuffer.entries.collectAsStateWithLifecycle()
            var minLevel by rememberSaveable { mutableStateOf(LogLevel.VERBOSE) }

            SampleTheme(darkTheme = isSystemInDarkTheme()) {
                LogViewerScreen(
                    entries = entries,
                    minLevel = minLevel,
                    logFileSummary = fileSummary(),
                    onMinLevelChange = { minLevel = it },
                    onSimulateCall = {
                        CallSimulator.simulateCall()
                        LogKit.flush()
                    },
                    onClear = { SampleApplication.inAppBuffer.clear() },
                )
            }
        }
    }

    private fun fileSummary(): String {
        val files = SampleApplication.fileSink.files()
        val kb = files.sumOf { it.length() } / 1024.0
        return "%d log file(s) · %.1f KB on disk".format(files.size, kb)
    }
}
