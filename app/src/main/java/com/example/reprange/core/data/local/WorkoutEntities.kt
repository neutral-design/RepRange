package com.example.reprange.core.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "workout_day",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "workout_session",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayId")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val startedAtMillis: Long,
    val sortOrder: Int
)

@Entity(
    tableName = "exercise_entry",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseName")]
)
data class ExerciseEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String,
    val sortOrder: Int
)

@Entity(
    tableName = "set_entry",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseEntryId")]
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseEntryId: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRmKg: Double?,
    val sortOrder: Int
)

data class ExerciseEntryWithSets(
    @Embedded val exercise: ExerciseEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseEntryId"
    )
    val sets: List<SetEntryEntity>
)

data class WorkoutSessionWithDetails(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = ExerciseEntryEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exercises: List<ExerciseEntryWithSets>
)

data class WorkoutDayWithDetails(
    @Embedded val day: WorkoutDayEntity,
    @Relation(
        entity = WorkoutSessionEntity::class,
        parentColumn = "id",
        entityColumn = "dayId"
    )
    val sessions: List<WorkoutSessionWithDetails>
)

data class ExerciseHistoryRow(
    val dateEpochDay: Long,
    val sessionStartedAtMillis: Long,
    val sessionSortOrder: Int,
    val exerciseEntryId: Long,
    val setId: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRmKg: Double?,
    val setSortOrder: Int
)

data class ExportSetRow(
    val dateEpochDay: Long,
    val sessionSortOrder: Int,
    val sessionStartedAtMillis: Long,
    val exerciseName: String,
    val exerciseSortOrder: Int,
    val setSortOrder: Int,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRmKg: Double?
)
