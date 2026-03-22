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
