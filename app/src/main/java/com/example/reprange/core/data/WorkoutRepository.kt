package com.example.reprange.core.data

import com.example.reprange.core.model.WorkoutDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface WorkoutRepository {
    fun observeDay(date: LocalDate): Flow<WorkoutDay?>

    fun observeExerciseSuggestions(query: String): Flow<List<String>>

    suspend fun addExercise(
        date: LocalDate,
        sessionId: Long?,
        exerciseName: String,
        reps: Int,
        weightKg: Double
    )

    suspend fun addSet(
        exerciseEntryId: Long,
        reps: Int,
        weightKg: Double
    )

    suspend fun updateSet(
        setId: Long,
        reps: Int,
        weightKg: Double
    )

    suspend fun deleteSet(setId: Long)

    suspend fun deleteSession(sessionId: Long)

    suspend fun createSession(date: LocalDate): Long
}
