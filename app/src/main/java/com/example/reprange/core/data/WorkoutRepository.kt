package com.example.reprange.core.data

import com.example.reprange.core.model.AppStats
import com.example.reprange.core.model.ExportableSetRow
import com.example.reprange.core.model.WorkoutDay
import com.example.reprange.core.model.ExerciseHistory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface WorkoutRepository {
    fun observeDay(date: LocalDate): Flow<WorkoutDay?>

    fun observeWorkoutDates(): Flow<Set<LocalDate>>

    fun observeAppStats(): Flow<AppStats>

    fun observeAllExerciseNames(): Flow<List<String>>

    fun observeExerciseSuggestions(query: String): Flow<List<String>>

    fun observeExerciseHistory(exerciseName: String): Flow<ExerciseHistory>

    suspend fun getExportableSetRows(): List<ExportableSetRow>

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

    suspend fun renameExercise(exerciseEntryId: Long, newName: String)

    suspend fun deleteExercise(exerciseEntryId: Long)

    suspend fun deleteSession(sessionId: Long)

    suspend fun createSession(date: LocalDate): Long
}
