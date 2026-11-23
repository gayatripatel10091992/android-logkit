package com.gayatripatel.logkit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayatripatel.logkit.LogEntry
import com.gayatripatel.logkit.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun SampleTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(primary = Color(0xFF7DD3FC), background = Color(0xFF0B1220), surface = Color(0xFF0B1220))
    } else {
        lightColorScheme(primary = Color(0xFF0369A1), background = Color(0xFFF7FAFC), surface = Color(0xFFF7FAFC))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

fun LogLevel.color(): Color = when (this) {
    LogLevel.VERBOSE -> Color(0xFF94A3B8)
    LogLevel.DEBUG -> Color(0xFF0EA5E9)
    LogLevel.INFO -> Color(0xFF22C55E)
    LogLevel.WARN -> Color(0xFFF59E0B)
    LogLevel.ERROR -> Color(0xFFEF4444)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    entries: List<LogEntry>,
    minLevel: LogLevel,
    logFileSummary: String,
    onMinLevelChange: (LogLevel) -> Unit,
    onSimulateCall: () -> Unit,
    onClear: () -> Unit,
    timeZone: TimeZone = TimeZone.getDefault(),
    modifier: Modifier = Modifier,
) {
    val visible = entries.filter { it.level >= minLevel }.asReversed()
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).apply { this.timeZone = timeZone }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LogKit", fontWeight = FontWeight.Bold)
                        Text(
                            logFileSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onSimulateCall, modifier = Modifier.weight(1f)) { Text("Simulate call") }
                OutlinedButton(onClick = onClear) { Text("Clear") }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(LogLevel.entries) { level ->
                    FilterChip(
                        selected = level == minLevel,
                        onClick = { onMinLevelChange(level) },
                        label = { Text("${level.letter}+") },
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible) { entry -> LogRow(entry, timeFormat.format(Date(entry.timestampMillis))) }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = entry.level.color().copy(alpha = 0.18f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        entry.level.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = entry.level.color(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(entry.tag, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Text(
                    "$time · ${entry.threadName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(entry.message, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            entry.throwable?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${it::class.java.simpleName}: ${it.message}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = LogLevel.ERROR.color(),
                )
            }
        }
    }
}
