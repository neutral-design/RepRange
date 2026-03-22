package com.example.reprange.features.diary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reprange.core.data.WorkoutRepository
import com.example.reprange.core.model.ExerciseHistory
import com.example.reprange.core.model.SetRecommendation
import com.example.reprange.core.settings.AppPreferencesRepository
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
    private val repository: WorkoutRepository,
    private val preferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val chromeState = MutableStateFlow(DiaryChromeState())
    private val historyExerciseName = MutableStateFlow<String?>(null)

    private val dayFlow = selectedDate.flatMapLatest { repository.observeDay(it) }
    private val workoutDatesFlow = repository.observeWorkoutDates()
    private val targetRepsFlow = preferencesRepository.targetRepsFlow
    private val historyFlow = historyExerciseName.flatMapLatest { exerciseName ->
        if (exerciseName == null) {
            flowOf(null)
        } else {
            repository.observeExerciseHistory(exerciseName)
        }
    }
    private val recommendationHistoryFlow = chromeState.flatMapLatest { state ->
        val exerciseName = state.setEditorTarget?.exerciseName
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
        targetRepsFlow,
        historyFlow,
        recommendationHistoryFlow,
        suggestionFlow,
        chromeState
    ) { values ->
        val date = values[0] as LocalDate
        val workoutDay = values[1] as com.example.reprange.core.model.WorkoutDay?
        @Suppress("UNCHECKED_CAST")
        val workoutDates = values[2] as Set<LocalDate>
        val targetReps = values[3] as Int
        val exerciseHistory = values[4] as ExerciseHistory?
        val recommendationHistory = values[5] as ExerciseHistory?
        @Suppress("UNCHECKED_CAST")
        val suggestions = values[6] as List<String>
        val chrome = values[7] as DiaryChromeState
        DiaryUiState(
            selectedDate = date,
            workoutDay = workoutDay,
            workoutDates = workoutDates,
            targetReps = targetReps,
            exerciseHistory = exerciseHistory,
            exerciseSuggestions = suggestions,
            showDatePicker = chrome.showDatePicker,
            showAddExerciseDialog = chrome.showAddExerciseDialog,
            addExerciseTargetSessionId = chrome.addExerciseTargetSessionId,
            setEditorTarget = chrome.setEditorTarget,
            setRecommendation = buildRecommendation(
                history = recommendationHistory,
                inputWeight = chrome.setWeightInput,
                targetReps = targetReps
            ),
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
                setWeightInput = "",
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
                setWeightInput = if (weightKg > 0.0) weightKg.toString() else "",
                message = null
            )
        }
    }

    fun dismissSetEditor() {
        chromeState.update { it.copy(setEditorTarget = null, setWeightInput = "") }
    }

    fun onSetWeightInputChange(value: String) {
        chromeState.update { it.copy(setWeightInput = value) }
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
            chromeState.update { it.copy(setEditorTarget = null, setWeightInput = "") }
        }
    }

    fun deleteEditedSet() {
        val target = chromeState.value.setEditorTarget ?: return
        val setId = target.setId ?: return
        viewModelScope.launch {
            repository.deleteSet(setId)
            chromeState.update { it.copy(setEditorTarget = null, setWeightInput = "") }
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
    val setWeightInput: String = "",
    val exerciseEditorTarget: ExerciseEditorTarget? = null,
    val deleteSessionTarget: DeleteSessionTarget? = null,
    val exerciseQuery: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
)

class DiaryViewModelFactory(
    private val repository: WorkoutRepository,
    private val preferencesRepository: AppPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryViewModel(repository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun buildRecommendation(
    history: ExerciseHistory?,
    inputWeight: String,
    targetReps: Int
): SetRecommendation? {
    val recentEntries = history?.entries?.take(5).orEmpty()
    if (recentEntries.isEmpty()) {
        return null
    }

    val oneRmValues = recentEntries.flatMap { entry ->
        entry.sets.mapNotNull { it.estimatedOneRmKg }
    }.filter { it > 0.0 }

    if (oneRmValues.isEmpty()) {
        return null
    }

    val averageOneRm = oneRmValues.average()
    val weight = inputWeight.replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull()

    val estimatedRepsText = if (weight != null && weight > 0.0) {
        val estimatedReps = estimateRepsFromOneRm(weight, averageOneRm)
        if (estimatedReps != null) {
            "Recent estimate: about $estimatedReps reps at ${formatWeight(weight)}"
        } else {
            null
        }
    } else {
        null
    }

    val suggestedWeight = estimateWeightForTargetReps(targetReps, averageOneRm)
    val suggestedWeightText = suggestedWeight?.let {
        "Suggested for $targetReps reps: ${formatWeight(it)}"
    }

    return if (estimatedRepsText == null && suggestedWeightText == null) {
        null
    } else {
        SetRecommendation(
            estimatedRepsText = estimatedRepsText,
            suggestedWeightText = suggestedWeightText,
            basedOnText = "Based on ${recentEntries.size} recent logged exercise entries"
        )
    }
}

private fun estimateRepsFromOneRm(weight: Double, oneRm: Double): String? {
    if (weight <= 0.0 || oneRm <= weight) {
        return if (weight > 0.0 && oneRm > 0.0) "1-2" else null
    }
    val epley = (30.0 * (oneRm / weight - 1.0)).coerceIn(1.0, 30.0)
    val low = epley.toInt().coerceAtLeast(1)
    val high = kotlin.math.ceil(epley + 1.5).toInt().coerceAtMost(30)
    return if (high <= low) low.toString() else "$low-$high"
}

private fun estimateWeightForTargetReps(targetReps: Int, oneRm: Double): Double? {
    if (targetReps <= 0 || oneRm <= 0.0) {
        return null
    }
    return oneRm / (1.0 + targetReps / 30.0)
}

private fun formatWeight(weight: Double): String = "${"%.1f".format(weight)} kg"
