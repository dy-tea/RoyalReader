package app.royalreader.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Daily totals for the last month as a bar chart.
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewmodel: StatsViewModel = hiltViewModel(),
) {
    val state by viewmodel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reading time") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(text = "Last ${StatsUiState.DAYS_SHOWN} days", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Total: ${formatDuration(state.totalMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DailyBars(daily = state.daily)
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            val longest = state.daily.maxByOrNull { it.totalMs }
            Text(
                text = longest?.let {
                    val date = LocalDate.ofEpochDay(it.day)
                    "Busiest day: ${date.format(dayFormat)} (${formatDuration(it.totalMs)})"
                } ?: "Nothing tracked yet - open a book to log time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One bar per day, aligned on the bottom edge of a fixed-height track.
 **/
@Composable
private fun DailyBars(daily: List<app.royalreader.domain.model.DailyReadingTime>) {
    if (daily.isEmpty()) {
        Text(
            text = "No reading time this month yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val max = daily.maxOf { it.totalMs }.coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
    ) {
        daily.forEach { entry ->
            val fraction = entry.totalMs.toFloat() / max.toFloat()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((fraction * 120f).coerceAtLeast(4f).dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    // First and last day labels as month ends.
    Row(modifier = Modifier.fillMaxWidth()) {
        val first = LocalDate.ofEpochDay(daily.first().day)
        val last = LocalDate.ofEpochDay(daily.last().day)
        Text(first.format(dayFormat), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(0.dp))
        Box(modifier = Modifier.weight(1f))
        Text(last.format(dayFormat), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/**
 * Format a duration as "12h 34m" or "45m".
 **/
fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    val hours = minutes / 60
    return if (hours > 0) "$hours h ${minutes % 60} m" else "$minutes m"
}
