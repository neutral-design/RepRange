package com.example.reprange.features.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.reprange.core.model.ExerciseHistory
import com.example.reprange.core.model.ExerciseHistoryEntry
import com.example.reprange.core.model.ExerciseProgressPoint
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ExerciseHistoryOverview(
    history: ExerciseHistory,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("History overview", style = MaterialTheme.typography.titleMedium)
                Text("Logged ${history.entries.size} time(s)")
                Text(
                    history.highestWeightKg?.let { "Highest weight ${formatDouble(it, 1)} kg" }
                        ?: "Highest weight unavailable"
                )
                Text(
                    history.highestEstimatedOneRmKg?.let { "Best estimated 1RM ${formatDouble(it, 1)} kg" }
                        ?: "Best estimated 1RM unavailable"
                )
                Text("Total volume ${formatDouble(history.totalVolume, 0)} kg")
            }
        }

        if (history.oneRmPoints.isNotEmpty()) {
            ProgressChartCard(
                title = "Estimated 1RM over time",
                points = history.oneRmPoints,
                valueFormatter = { "${formatDouble(it, 1)} kg" }
            )
        }

        if (history.volumePoints.isNotEmpty()) {
            ProgressChartCard(
                title = "Volume over time",
                points = history.volumePoints,
                valueFormatter = { "${formatDouble(it, 0)} kg" }
            )
        }

        if (history.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history yet for this exercise.")
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                history.entries.forEach { entry ->
                    ExerciseHistoryEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
fun ProgressChartCard(
    title: String,
    points: List<ExerciseProgressPoint>,
    valueFormatter: (Double) -> String
) {
    val latest = points.lastOrNull()
    val minValue = points.minOfOrNull { it.value }
    val maxValue = points.maxOfOrNull { it.value }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            latest?.let {
                Text(
                    text = "Latest ${valueFormatter(it.value)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (minValue != null && maxValue != null) {
                Text(
                    text = "Range ${valueFormatter(minValue)} - ${valueFormatter(maxValue)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            MiniLineChart(
                points = points,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = points.firstOrNull()?.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)).orEmpty(),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = points.lastOrNull()?.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)).orEmpty(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ExerciseHistoryEntryCard(
    entry: ExerciseHistoryEntry
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = entry.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.titleSmall
            )
            if (entry.sessionSortOrder > 0) {
                Text(
                    text = "Session ${entry.sessionSortOrder + 1}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            entry.sets.forEachIndexed { index, set ->
                Text(
                    "Set ${index + 1}: ${formatSetSummary(set.weightKg, set.reps)}  |  1RM ${set.estimatedOneRmKg?.let { formatDouble(it, 1) } ?: "-"}"
                )
            }
            Text(
                text = "Volume ${formatDouble(entry.totalVolume, 0)} kg",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MiniLineChart(
    points: List<ExerciseProgressPoint>,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val horizontalPadding = 18.dp.toPx()
        val verticalPadding = 20.dp.toPx()
        val chartWidth = size.width - (horizontalPadding * 2)
        val chartHeight = size.height - (verticalPadding * 2)
        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        val minValue = points.minOf { it.value }
        val maxValue = points.maxOf { it.value }
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

        val offsets = points.mapIndexed { index, point ->
            val fractionX = if (points.size == 1) 0.5f else index.toFloat() / points.lastIndex.toFloat()
            val normalizedY = ((point.value - minValue) / range).toFloat()
            Offset(
                x = horizontalPadding + chartWidth * fractionX,
                y = verticalPadding + chartHeight * (1f - normalizedY)
            )
        }

        drawRect(
            color = surfaceVariant.copy(alpha = 0.5f),
            topLeft = Offset(horizontalPadding, verticalPadding),
            size = Size(chartWidth, chartHeight)
        )

        val linePath = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path = linePath, color = color, style = Stroke(width = 3.dp.toPx()))

        offsets.forEach { offset ->
            drawCircle(color = color, radius = 5.dp.toPx(), center = offset)
        }
    }
}

fun formatDouble(value: Double, digits: Int): String = "%.${digits}f".format(value)

fun formatSetSummary(weightKg: Double, reps: Int): String {
    return if (weightKg <= 0.0) "$reps reps" else "${formatDouble(weightKg, 1)} kg x $reps"
}
