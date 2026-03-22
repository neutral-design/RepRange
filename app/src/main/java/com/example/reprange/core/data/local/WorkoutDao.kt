package com.example.reprange.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workout_day WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeDay(dateEpochDay: Long): Flow<WorkoutDayWithDetails?>

    @Query("SELECT id FROM workout_day WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getDayIdByDate(dateEpochDay: Long): Long?

    @Insert
    suspend fun insertDay(day: WorkoutDayEntity): Long

    @Query("UPDATE workout_day SET updatedAtMillis = :updatedAtMillis WHERE id = :dayId")
    suspend fun updateDayTimestamp(dayId: Long, updatedAtMillis: Long)

    @Query("SELECT COUNT(*) FROM workout_session WHERE dayId = :dayId")
    suspend fun getSessionCount(dayId: Long): Int

    @Query("SELECT id FROM workout_session WHERE dayId = :dayId ORDER BY sortOrder DESC, id DESC LIMIT 1")
    suspend fun getLatestSessionId(dayId: Long): Long?

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("SELECT COUNT(*) FROM exercise_entry WHERE sessionId = :sessionId")
    suspend fun getExerciseCount(sessionId: Long): Int

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntryEntity): Long

    @Query("SELECT COUNT(*) FROM set_entry WHERE exerciseEntryId = :exerciseEntryId")
    suspend fun getSetCount(exerciseEntryId: Long): Int

    @Insert
    suspend fun insertSet(set: SetEntryEntity): Long

    @Query(
        """
        UPDATE set_entry
        SET reps = :reps,
            weightKg = :weightKg,
            estimatedOneRmKg = :estimatedOneRmKg
        WHERE id = :setId
        """
    )
    suspend fun updateSet(
        setId: Long,
        reps: Int,
        weightKg: Double,
        estimatedOneRmKg: Double?
    )

    @Query("DELETE FROM set_entry WHERE id = :setId")
    suspend fun deleteSetById(setId: Long)

    @Query("DELETE FROM exercise_entry WHERE id = :exerciseEntryId")
    suspend fun deleteExerciseById(exerciseEntryId: Long)

    @Query("DELETE FROM workout_session WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM workout_day WHERE id = :dayId AND NOT EXISTS (SELECT 1 FROM workout_session WHERE dayId = :dayId)")
    suspend fun deleteDayIfEmpty(dayId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM workout_day WHERE id = :dayId)")
    suspend fun hasDay(dayId: Long): Boolean

    @Query("SELECT exerciseEntryId FROM set_entry WHERE id = :setId LIMIT 1")
    suspend fun getExerciseIdBySet(setId: Long): Long?

    @Query("SELECT sessionId FROM exercise_entry WHERE id = :exerciseEntryId LIMIT 1")
    suspend fun getSessionIdByExercise(exerciseEntryId: Long): Long?

    @Query("SELECT dayId FROM workout_session WHERE id = :sessionId LIMIT 1")
    suspend fun getDayIdBySession(sessionId: Long): Long?

    @Query(
        """
        SELECT workout_session.dayId
        FROM set_entry
        INNER JOIN exercise_entry ON exercise_entry.id = set_entry.exerciseEntryId
        INNER JOIN workout_session ON workout_session.id = exercise_entry.sessionId
        WHERE set_entry.id = :setId
        LIMIT 1
        """
    )
    suspend fun getDayIdBySet(setId: Long): Long?

    @Query("SELECT COUNT(*) FROM set_entry WHERE exerciseEntryId = :exerciseEntryId")
    suspend fun getSetCountForExercise(exerciseEntryId: Long): Int

    @Query("SELECT COUNT(*) FROM exercise_entry WHERE sessionId = :sessionId")
    suspend fun getExerciseCountForSession(sessionId: Long): Int

    @Query(
        """
        SELECT DISTINCT exerciseName
        FROM exercise_entry
        WHERE exerciseName LIKE :prefix ESCAPE '\'
        ORDER BY exerciseName COLLATE NOCASE
        LIMIT 8
        """
    )
    fun observeExerciseSuggestions(prefix: String): Flow<List<String>>
}
