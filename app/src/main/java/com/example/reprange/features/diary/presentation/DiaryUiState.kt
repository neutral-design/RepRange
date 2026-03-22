package com.example.reprange.features.diary.presentation

import com.example.reprange.core.model.WorkoutDay
import java.time.LocalDate

data class SetEditorTarget(
    val exerciseId: Long,
    val exerciseName: String,
    val setId: Long? = null,
    val reps: Int? = null,
    val weightKg: Double? = null
)

data class DeleteSessionTarget(
    val sessionId: Long,
    val sessionLabel: String
)

data class DiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val workoutDay: WorkoutDay? = null,
    val exerciseSuggestions: List<String> = emptyList(),
    val showDatePicker: Boolean = false,
    val showAddExerciseDialog: Boolean = false,
    val addExerciseTargetSessionId: Long? = null,
    val setEditorTarget: SetEditorTarget? = null,
    val deleteSessionTarget: DeleteSessionTarget? = null,
    val exerciseQuery: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
)
