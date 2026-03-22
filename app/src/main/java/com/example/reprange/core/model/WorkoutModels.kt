package com.example.reprange.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WorkoutDay(
    val id: Long,
    val date: LocalDate,
    val sessions: List<WorkoutSession>
) {
    val totalVolume: Double
        get() = sessions.sumOf { it.totalVolume }
}

data class WorkoutSession(
    val id: Long,
    val startedAtMillis: Long,
    val sortOrder: Int,
    val exercises: List<ExerciseLog>
) {
    val totalVolume: Double
        get() = exercises.sumOf { it.totalVolume }

    fun label(zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (sortOrder == 0) {
            return "Session"
        }
        val localTime = Instant.ofEpochMilli(startedAtMillis).atZone(zoneId).toLocalTime()
        return "Session ${sortOrder + 1} - ${localTime.toString().take(5)}"
    }
}

data class ExerciseLog(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val sets: List<LoggedSet>
) {
    val totalVolume: Double
        get() = sets.sumOf { it.volume }
}

data class LoggedSet(
    val id: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRmKg: Double?,
    val sortOrder: Int
) {
    val volume: Double
        get() = reps * weightKg
}

data class ExerciseHistory(
    val exerciseName: String,
    val entries: List<ExerciseHistoryEntry>
) {
    val highestWeightKg: Double?
        get() = entries.flatMap { it.sets }.maxOfOrNull { it.weightKg }

    val highestEstimatedOneRmKg: Double?
        get() = entries.flatMap { it.sets }.maxOfOrNull { it.estimatedOneRmKg ?: 0.0 }?.takeIf { it > 0.0 }

    val totalVolume: Double
        get() = entries.sumOf { entry -> entry.sets.sumOf { it.volume } }

    val oneRmPoints: List<ExerciseProgressPoint>
        get() = entries.mapNotNull { entry ->
            val bestOneRm = entry.sets.maxOfOrNull { it.estimatedOneRmKg ?: 0.0 }?.takeIf { it > 0.0 }
            bestOneRm?.let {
                ExerciseProgressPoint(
                    date = entry.date,
                    value = it,
                    label = "${entry.date} - 1RM ${"%.1f".format(it)} kg"
                )
            }
        }.sortedBy { it.date }

    val volumePoints: List<ExerciseProgressPoint>
        get() = entries.map { entry ->
            ExerciseProgressPoint(
                date = entry.date,
                value = entry.totalVolume,
                label = "${entry.date} - Volume ${"%.0f".format(entry.totalVolume)} kg"
            )
        }.sortedBy { it.date }
}

data class ExerciseHistoryEntry(
    val exerciseEntryId: Long,
    val date: LocalDate,
    val sessionStartedAtMillis: Long,
    val sessionSortOrder: Int,
    val sets: List<LoggedSet>
) {
    val totalVolume: Double
        get() = sets.sumOf { it.volume }
}

data class ExerciseProgressPoint(
    val date: LocalDate,
    val value: Double,
    val label: String
)

data class AppStats(
    val workoutDays: Int = 0,
    val sessions: Int = 0,
    val exercises: Int = 0,
    val sets: Int = 0,
    val totalVolumeKg: Double = 0.0
)

data class ExportableSetRow(
    val date: LocalDate,
    val sessionIndex: Int,
    val sessionStartedAtMillis: Long,
    val exerciseName: String,
    val exerciseIndex: Int,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRmKg: Double?
) {
    val setVolumeKg: Double
        get() = reps * weightKg
}
