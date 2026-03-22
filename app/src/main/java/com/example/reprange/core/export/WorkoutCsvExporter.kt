package com.example.reprange.core.export

import android.content.Context
import com.example.reprange.core.data.WorkoutRepository
import java.io.File
import java.time.format.DateTimeFormatter

class WorkoutCsvExporter(
    private val repository: WorkoutRepository
) {
    suspend fun export(context: Context): File {
        val rows = repository.getExportableSetRows()
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(java.time.LocalDateTime.now())
        val file = File(exportDir, "reprange-workouts-$timestamp.csv")

        val content = buildString {
            appendLine("date,session_index,exercise_name,exercise_index,set_index,reps,weight_kg,estimated_1rm_kg,set_volume_kg")
            rows.forEach { row ->
                appendCsvRow(
                    row.date.toString(),
                    row.sessionIndex.toString(),
                    row.exerciseName,
                    row.exerciseIndex.toString(),
                    row.setIndex.toString(),
                    row.reps.toString(),
                    row.weightKg.toString(),
                    row.estimatedOneRmKg?.toString().orEmpty(),
                    row.setVolumeKg.toString()
                )
            }
        }

        file.writeText(content, Charsets.UTF_8)
        return file
    }
}

private fun StringBuilder.appendCsvRow(vararg values: String) {
    append(values.joinToString(",") { value ->
        val escaped = value.replace("\"", "\"\"")
        "\"$escaped\""
    })
    appendLine()
}
