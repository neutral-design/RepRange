package com.example.reprange.features.diary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reprange.core.data.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val chromeState = MutableStateFlow(DiaryChromeState())
    private val historyExerciseName = MutableStateFlow<String?>(null)

    private val dayFlow = selectedDate.flatMapLatest { repository.observeDay(it) }
    private val workoutDatesFlow = repository.observeWorkoutDates()
    private val historyFlow = historyExerciseName.flatMapLatest { exerciseName ->
        if (exerciseName == null) {
            flowOf(null)
        } else {
            repository.observeExerciseHistory(exerciseName)
        }
    }

    @OptIn(FlowPreview::class)
    private val suggestionFlow = chromeState
        .debounce(120)
        .flatMapLatest { state ->
            if (!state.showAddExerciseDialog) {
                repository.observeExerciseSuggestions("")
            } else {
                repository.observeExerciseSuggestions(state.exerciseQuery)
            }
        }

    val uiState = combine(
        selectedDate,
        dayFlow,
        workoutDatesFlow,
        historyFlow,
        suggestionFlow,
        chromeState
    ) { values ->
        val date = values[0] as LocalDate
        val workoutDay = values[1] as com.example.reprange.core.model.WorkoutDay?
        @Suppress("UNCHECKED_CAST")
        val workoutDates = values[2] as Set<LocalDate>
        val exerciseHistory = values[3] as com.example.reprange.core.model.ExerciseHistory?
        @Suppress("UNCHECKED_CAST")
        val suggestions = values[4] as List<String>
        val chrome = values[5] as DiaryChromeState
        DiaryUiState(
            selectedDate = date,
            workoutDay = workoutDay,
            workoutDates = workoutDates,
            exerciseHistory = exerciseHistory,
            exerciseSuggestions = suggestions,
            showDatePicker = chrome.showDatePicker,
            showAddExerciseDialog = chrome.showAddExerciseDialog,
            addExerciseTargetSessionId = chrome.addExerciseTargetSessionId,
            setEditorTarget = chrome.setEditorTarget,
            exerciseEditorTarget = chrome.exerciseEditorTarget,
            deleteSessionTarget = chrome.deleteSessionTarget,
            exerciseQuery = chrome.exerciseQuery,
            isSaving = chrome.isSaving,
            message = chrome.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState()
    )

    fun goToPreviousDay() {
        selectedDate.update { it.minusDays(1) }
    }

    fun goToNextDay() {
        selectedDate.update { it.plusDays(1) }
    }

    fun onDatePicked(date: LocalDate) {
        selectedDate.value = date
        chromeState.update { it.copy(showDatePicker = false) }
    }

    fun showDatePicker() {
        chromeState.update { it.copy(showDatePicker = true) }
    }

    fun dismissDatePicker() {
        chromeState.update { it.copy(showDatePicker = false) }
    }

    fun openAddExercise(sessionId: Long? = null) {
        chromeState.update {
            it.copy(
                showAddExerciseDialog = true,
                addExerciseTargetSessionId = sessionId,
                exerciseQuery = "",
                message = null
            )
        }
    }

    fun onExerciseQueryChange(query: String) {
        chromeState.update { it.copy(exerciseQuery = query) }
    }

    fun dismissAddExercise() {
        chromeState.update {
            it.copy(
                showAddExerciseDialog = false,
                addExerciseTargetSessionId = null,
                exerciseQuery = "",
                isSaving = false
            )
        }
    }

    fun saveExercise(name: String, reps: Int, weightKg: Double) {
        viewModelScope.launch {
            chromeState.update { it.copy(isSaving = true, message = null) }
            repository.addExercise(
                date = selectedDate.value,
                sessionId = chromeState.value.addExerciseTargetSessionId,
                exerciseName = name,
                reps = reps,
                weightKg = weightKg
            )
            chromeState.update {
                it.copy(
                    showAddExerciseDialog = false,
                    addExerciseTargetSessionId = null,
                    exerciseQuery = "",
                    isSaving = false
                )
            }
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            val sessionId = repository.createSession(selectedDate.value)
            chromeState.update {
                it.copy(
                    showAddExerciseDialog = true,
                    addExerciseTargetSessionId = sessionId,
                    exerciseQuery = "",
                    message = null
                )
            }
        }
    }

    fun openAddSet(exerciseId: Long, exerciseName: String) {
        chromeState.update {
            it.copy(
                setEditorTarget = SetEditorTarget(exerciseId = exerciseId, exerciseName = exerciseName),
                message = null
            )
        }
    }

    fun openEditSet(
        exerciseId: Long,
        exerciseName: String,
        setId: Long,
        reps: Int,
        weightKg: Double
    ) {
        chromeState.update {
            it.copy(
                setEditorTarget = SetEditorTarget(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    setId = setId,
                    reps = reps,
                    weightKg = weightKg
                ),
                message = null
            )
        }
    }

    fun dismissSetEditor() {
        chromeState.update { it.copy(setEditorTarget = null) }
    }

    fun saveSet(reps: Int, weightKg: Double) {
        val target = chromeState.value.setEditorTarget ?: return
        viewModelScope.launch {
            val setId = target.setId
            if (setId == null) {
                repository.addSet(target.exerciseId, reps, weightKg)
            } else {
                repository.updateSet(setId, reps, weightKg)
            }
            chromeState.update { it.copy(setEditorTarget = null) }
        }
    }

    fun deleteEditedSet() {
        val target = chromeState.value.setEditorTarget ?: return
        val setId = target.setId ?: return
        viewModelScope.launch {
            repository.deleteSet(setId)
            chromeState.update { it.copy(setEditorTarget = null) }
        }
    }

    fun confirmDeleteSession(sessionId: Long, sessionLabel: String) {
        chromeState.update {
            it.copy(
                deleteSessionTarget = DeleteSessionTarget(
                    sessionId = sessionId,
                    sessionLabel = sessionLabel
                )
            )
        }
    }

    fun openExerciseEditor(exerciseId: Long, exerciseName: String) {
        chromeState.update {
            it.copy(
                exerciseEditorTarget = ExerciseEditorTarget(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName
                ),
                message = null
            )
        }
    }

    fun dismissExerciseEditor() {
        chromeState.update { it.copy(exerciseEditorTarget = null) }
    }

    fun renameExercise(newName: String) {
        val target = chromeState.value.exerciseEditorTarget ?: return
        viewModelScope.launch {
            repository.renameExercise(target.exerciseId, newName)
            chromeState.update { it.copy(exerciseEditorTarget = null) }
            if (historyExerciseName.value == target.exerciseName) {
                historyExerciseName.value = newName.trim()
            }
        }
    }

    fun deleteExercise() {
        val target = chromeState.value.exerciseEditorTarget ?: return
        viewModelScope.launch {
            repository.deleteExercise(target.exerciseId)
            chromeState.update { it.copy(exerciseEditorTarget = null) }
            if (historyExerciseName.value == target.exerciseName) {
                historyExerciseName.value = null
            }
        }
    }

    fun openExerciseHistory(exerciseName: String) {
        historyExerciseName.value = exerciseName
        chromeState.update { it.copy(exerciseEditorTarget = null) }
    }

    fun closeExerciseHistory() {
        historyExerciseName.value = null
    }

    fun dismissDeleteSession() {
        chromeState.update { it.copy(deleteSessionTarget = null) }
    }

    fun deleteSessionConfirmed() {
        val target = chromeState.value.deleteSessionTarget ?: return
        viewModelScope.launch {
            repository.deleteSession(target.sessionId)
            chromeState.update { it.copy(deleteSessionTarget = null) }
        }
    }

    fun clearMessage() {
        chromeState.update { it.copy(message = null) }
    }
}

private data class DiaryChromeState(
    val showDatePicker: Boolean = false,
    val showAddExerciseDialog: Boolean = false,
    val addExerciseTargetSessionId: Long? = null,
    val setEditorTarget: SetEditorTarget? = null,
    val exerciseEditorTarget: ExerciseEditorTarget? = null,
    val deleteSessionTarget: DeleteSessionTarget? = null,
    val exerciseQuery: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
)

class DiaryViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
