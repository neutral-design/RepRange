package com.example.reprange.core.data

import com.example.reprange.core.data.local.ExerciseEntryEntity
import com.example.reprange.core.data.local.ExerciseHistoryRow
import com.example.reprange.core.data.local.SetEntryEntity
import com.example.reprange.core.data.local.WorkoutDao
import com.example.reprange.core.data.local.WorkoutDayEntity
import com.example.reprange.core.data.local.WorkoutDayWithDetails
import com.example.reprange.core.data.local.WorkoutSessionEntity
import com.example.reprange.core.model.ExerciseLog
import com.example.reprange.core.model.ExerciseHistory
import com.example.reprange.core.model.ExerciseHistoryEntry
import com.example.reprange.core.model.LoggedSet
import com.example.reprange.core.model.WorkoutDay
import com.example.reprange.core.model.WorkoutSession
import com.example.reprange.domain.StrengthEstimator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class OfflineWorkoutRepository(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override fun observeDay(date: LocalDate): Flow<WorkoutDay?> {
        return workoutDao.observeDay(date.toEpochDay()).map { entity -> entity?.toDomain() }
    }

    override fun observeWorkoutDates(): Flow<Set<LocalDate>> {
        return workoutDao.observeWorkoutDates().map { dates ->
            dates.mapTo(linkedSetOf()) { LocalDate.ofEpochDay(it) }
        }
    }

    override fun observeExerciseSuggestions(query: String): Flow<List<String>> {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            return workoutDao.observeExerciseSuggestions("%")
        }
        val escaped = normalized
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return workoutDao.observeExerciseSuggestions("$escaped%")
    }

    override fun observeExerciseHistory(exerciseName: String): Flow<ExerciseHistory> {
        return workoutDao.observeExerciseHistory(exerciseName).map { rows ->
            rows.toExerciseHistory(exerciseName)
        }
    }

    override suspend fun addExercise(
        date: LocalDate,
        sessionId: Long?,
        exerciseName: String,
        reps: Int,
        weightKg: Double
    ) {
        val dayId = ensureDay(date)
        val targetSessionId = sessionId ?: workoutDao.getLatestSessionId(dayId) ?: createSession(date)
        val exerciseId = workoutDao.insertExercise(
            ExerciseEntryEntity(
                sessionId = targetSessionId,
                exerciseName = exerciseName.trim(),
                sortOrder = workoutDao.getExerciseCount(targetSessionId)
            )
        )
        insertSet(exerciseId, reps, weightKg)
        workoutDao.updateDayTimestamp(dayId, System.currentTimeMillis())
    }

    override suspend fun addSet(
        exerciseEntryId: Long,
        reps: Int,
        weightKg: Double
    ) {
        insertSet(exerciseEntryId, reps, weightKg)
    }

    override suspend fun updateSet(
        setId: Long,
        reps: Int,
        weightKg: Double
    ) {
        val estimatedOneRm = if (reps > 0 && weightKg > 0) {
            StrengthEstimator.estimate1Rm(weightKg, reps).avg
        } else {
            null
        }
        workoutDao.updateSet(setId, reps, weightKg, estimatedOneRm)
        workoutDao.getDayIdBySet(setId)?.let { dayId ->
            workoutDao.updateDayTimestamp(dayId, System.currentTimeMillis())
        }
    }

    override suspend fun deleteSet(setId: Long) {
        val exerciseId = workoutDao.getExerciseIdBySet(setId) ?: return
        val sessionId = workoutDao.getSessionIdByExercise(exerciseId) ?: return
        val dayId = workoutDao.getDayIdBySession(sessionId) ?: return

        workoutDao.deleteSetById(setId)

        if (workoutDao.getSetCountForExercise(exerciseId) == 0) {
            workoutDao.deleteExerciseById(exerciseId)
        }
        if (workoutDao.getExerciseCountForSession(sessionId) == 0) {
            workoutDao.deleteSessionById(sessionId)
        }

        workoutDao.deleteDayIfEmpty(dayId)
        if (workoutDao.hasDay(dayId)) {
            workoutDao.updateDayTimestamp(dayId, System.currentTimeMillis())
        }
    }

    override suspend fun renameExercise(exerciseEntryId: Long, newName: String) {
        val sessionId = workoutDao.getSessionIdByExercise(exerciseEntryId) ?: return
        val dayId = workoutDao.getDayIdBySession(sessionId) ?: return
        workoutDao.updateExerciseName(exerciseEntryId, newName.trim())
        if (workoutDao.hasDay(dayId)) {
            workoutDao.updateDayTimestamp(dayId, System.currentTimeMillis())
        }
    }

    override suspend fun deleteExercise(exerciseEntryId: Long) {
        val sessionId = workoutDao.getSessionIdByExercise(exerciseEntryId) ?: return
        val dayId = workoutDao.getDayIdBySession(sessionId) ?: return
        workoutDao.deleteExerciseById(exerciseEntryId)
        if (workoutDao.getExerciseCountForSession(sessionId) == 0) {
            workoutDao.deleteSessionById(sessionId)
        }
        workoutDao.deleteDayIfEmpty(dayId)
        if (workoutDao.hasDay(dayId)) {
            workoutDao.updateDayTimestamp(dayId, System.currentTimeMillis())
        }
    }

    override suspend fun deleteSession(sessionId: Long) {
        val dayId = workoutDao.getDayIdBySession(sessionId) ?: return
        workoutDao.deleteSessionById(sessionId)
        workoutDao.deleteDayIfEmpty(dayId)
        if (workoutDao.hasDay(dayId)) {
            workoutDao.updateDayTimestamp(dayId, System.currentTimeMillis())
        }
    }

    override suspend fun createSession(date: LocalDate): Long {
        val dayId = ensureDay(date)
        val sessionId = workoutDao.insertSession(
            WorkoutSessionEntity(
                dayId = dayId,
                startedAtMillis = System.currentTimeMillis(),
                sortOrder = workoutDao.getSessionCount(dayId)
            )
        )
        workoutDao.updateDayTimestamp(dayId, System.currentTimeMillis())
        return sessionId
    }

    private suspend fun ensureDay(date: LocalDate): Long {
        val existing = workoutDao.getDayIdByDate(date.toEpochDay())
        if (existing != null) {
            return existing
        }
        val now = System.currentTimeMillis()
        return workoutDao.insertDay(
            WorkoutDayEntity(
                dateEpochDay = date.toEpochDay(),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    private suspend fun insertSet(exerciseEntryId: Long, reps: Int, weightKg: Double) {
        val estimatedOneRm = if (reps > 0 && weightKg > 0) {
            StrengthEstimator.estimate1Rm(weightKg, reps).avg
        } else {
            null
        }
        workoutDao.insertSet(
            SetEntryEntity(
                exerciseEntryId = exerciseEntryId,
                reps = reps,
                weightKg = weightKg,
                estimatedOneRmKg = estimatedOneRm,
                sortOrder = workoutDao.getSetCount(exerciseEntryId)
            )
        )
    }
}

private fun WorkoutDayWithDetails.toDomain(): WorkoutDay {
    return WorkoutDay(
        id = day.id,
        date = LocalDate.ofEpochDay(day.dateEpochDay),
        sessions = sessions.sortedBy { it.session.sortOrder }.map { session ->
            WorkoutSession(
                id = session.session.id,
                startedAtMillis = session.session.startedAtMillis,
                sortOrder = session.session.sortOrder,
                exercises = session.exercises.sortedBy { it.exercise.sortOrder }.map { exercise ->
                    ExerciseLog(
                        id = exercise.exercise.id,
                        name = exercise.exercise.exerciseName,
                        sortOrder = exercise.exercise.sortOrder,
                        sets = exercise.sets.sortedBy { it.sortOrder }.map { set ->
                            LoggedSet(
                                id = set.id,
                                reps = set.reps,
                                weightKg = set.weightKg,
                                estimatedOneRmKg = set.estimatedOneRmKg,
                                sortOrder = set.sortOrder
                            )
                        }
                    )
                }
            )
        }
    )
}

private fun List<ExerciseHistoryRow>.toExerciseHistory(exerciseName: String): ExerciseHistory {
    val entries = groupBy { row ->
        Triple(row.dateEpochDay, row.sessionSortOrder, row.exerciseEntryId)
    }.map { (key, rows) ->
        ExerciseHistoryEntry(
            exerciseEntryId = key.third,
            date = LocalDate.ofEpochDay(key.first),
            sessionStartedAtMillis = rows.first().sessionStartedAtMillis,
            sessionSortOrder = key.second,
            sets = rows.sortedBy { it.setSortOrder }.map { row ->
                LoggedSet(
                    id = row.setId,
                    reps = row.reps,
                    weightKg = row.weightKg,
                    estimatedOneRmKg = row.estimatedOneRmKg,
                    sortOrder = row.setSortOrder
                )
            }
        )
    }.sortedWith(
        compareByDescending<ExerciseHistoryEntry> { it.date }
            .thenByDescending { it.sessionSortOrder }
            .thenByDescending { it.exerciseEntryId }
    )

    return ExerciseHistory(
        exerciseName = exerciseName,
        entries = entries
    )
}
