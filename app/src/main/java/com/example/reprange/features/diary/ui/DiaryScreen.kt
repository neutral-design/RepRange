package com.example.reprange.features.diary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reprange.core.model.ExerciseLog
import com.example.reprange.core.model.WorkoutDay
import com.example.reprange.core.model.WorkoutSession
import com.example.reprange.features.diary.presentation.DeleteSessionTarget
import com.example.reprange.features.diary.presentation.DiaryUiState
import com.example.reprange.features.diary.presentation.DiaryViewModel
import com.example.reprange.features.diary.presentation.DiaryViewModelFactory
import com.example.reprange.features.diary.presentation.SetEditorTarget
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DiaryRoute(
    factory: DiaryViewModelFactory
) {
    val viewModel: DiaryViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    DiaryScreen(
        state = state,
        onPreviousDay = viewModel::goToPreviousDay,
        onNextDay = viewModel::goToNextDay,
        onShowDatePicker = viewModel::showDatePicker,
        onDismissDatePicker = viewModel::dismissDatePicker,
        onDatePicked = viewModel::onDatePicked,
        onAddExercise = viewModel::openAddExercise,
        onStartNewSession = viewModel::startNewSession,
        onDismissAddExercise = viewModel::dismissAddExercise,
        onExerciseQueryChange = viewModel::onExerciseQueryChange,
        onSaveExercise = viewModel::saveExercise,
        onAddSet = viewModel::openAddSet,
        onEditSet = viewModel::openEditSet,
        onDismissSetEditor = viewModel::dismissSetEditor,
        onSaveSet = viewModel::saveSet,
        onDeleteSet = viewModel::deleteEditedSet,
        onDeleteSessionRequest = viewModel::confirmDeleteSession,
        onDismissDeleteSession = viewModel::dismissDeleteSession,
        onDeleteSessionConfirmed = viewModel::deleteSessionConfirmed,
        onMessageShown = viewModel::clearMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryScreen(
    state: DiaryUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onShowDatePicker: () -> Unit,
    onDismissDatePicker: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onAddExercise: (Long?) -> Unit,
    onStartNewSession: () -> Unit,
    onDismissAddExercise: () -> Unit,
    onExerciseQueryChange: (String) -> Unit,
    onSaveExercise: (String, Int, Double) -> Unit,
    onAddSet: (Long, String) -> Unit,
    onEditSet: (Long, String, Long, Int, Double) -> Unit,
    onDismissSetEditor: () -> Unit,
    onSaveSet: (Int, Double) -> Unit,
    onDeleteSet: () -> Unit,
    onDeleteSessionRequest: (Long, String) -> Unit,
    onDismissDeleteSession: () -> Unit,
    onDeleteSessionConfirmed: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Diary") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DiaryDateHeader(
                date = state.selectedDate,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onShowDatePicker = onShowDatePicker
            )

            DiaryContent(
                workoutDay = state.workoutDay,
                onAddExercise = onAddExercise,
                onStartNewSession = onStartNewSession,
                onAddSet = onAddSet,
                onEditSet = onEditSet,
                onDeleteSessionRequest = onDeleteSessionRequest
            )
        }
    }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToEpochMillis(state.selectedDate)
        )
        DatePickerDialog(
            onDismissRequest = onDismissDatePicker,
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis ?: return@TextButton
                        onDatePicked(epochMillisToLocalDate(picked))
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDatePicker) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (state.showAddExerciseDialog) {
        AddExerciseDialog(
            suggestions = state.exerciseSuggestions,
            initialName = state.exerciseQuery,
            isSaving = state.isSaving,
            onDismiss = onDismissAddExercise,
            onNameChange = onExerciseQueryChange,
            onSave = onSaveExercise
        )
    }

    state.setEditorTarget?.let { target ->
        SetEditorDialog(
            target = target,
            onDismiss = onDismissSetEditor,
            onSave = onSaveSet,
            onDelete = onDeleteSet
        )
    }

    state.deleteSessionTarget?.let { target ->
        DeleteSessionDialog(
            target = target,
            onDismiss = onDismissDeleteSession,
            onConfirmDelete = onDeleteSessionConfirmed
        )
    }
}

@Composable
private fun DiaryDateHeader(
    date: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onShowDatePicker: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPreviousDay) {
            Text("<")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (date == LocalDate.now()) {
                    "Today"
                } else {
                    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                },
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onShowDatePicker) {
                Text("Choose date")
            }
        }
        TextButton(onClick = onNextDay) {
            Text(">")
        }
    }
}

@Composable
private fun DiaryContent(
    workoutDay: WorkoutDay?,
    onAddExercise: (Long?) -> Unit,
    onStartNewSession: () -> Unit,
    onAddSet: (Long, String) -> Unit,
    onEditSet: (Long, String, Long, Int, Double) -> Unit,
    onDeleteSessionRequest: (Long, String) -> Unit
) {
    if (workoutDay == null || workoutDay.sessions.isEmpty()) {
        EmptyDiaryState(onAddExercise = { onAddExercise(null) })
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onAddExercise(null) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Add Exercise")
            }
            OutlinedButton(onClick = onStartNewSession) {
                Text("New Session")
            }
        }

        Text(
            text = "Volume ${formatDouble(workoutDay.totalVolume, 0)} kg",
            style = MaterialTheme.typography.bodyMedium
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(workoutDay.sessions, key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    showHeader = workoutDay.sessions.size > 1,
                    onAddExercise = onAddExercise,
                    onAddSet = onAddSet,
                    onEditSet = onEditSet,
                    onDeleteSessionRequest = onDeleteSessionRequest
                )
            }
        }
    }
}

@Composable
private fun EmptyDiaryState(
    onAddExercise: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("No training logged for this day yet.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap below to create the first session automatically and start logging.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onAddExercise) {
                    Text("+ Add Exercise")
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: WorkoutSession,
    showHeader: Boolean,
    onAddExercise: (Long?) -> Unit,
    onAddSet: (Long, String) -> Unit,
    onEditSet: (Long, String, Long, Int, Double) -> Unit,
    onDeleteSessionRequest: (Long, String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showHeader) {
                Text(session.label(), style = MaterialTheme.typography.titleMedium)
            }
            session.exercises.forEachIndexed { index, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onAddSet = onAddSet,
                    onEditSet = onEditSet
                )
                if (index < session.exercises.lastIndex) {
                    HorizontalDivider()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onAddExercise(session.id) }) {
                    Text("Add exercise")
                }
                TextButton(onClick = { onDeleteSessionRequest(session.id, session.label()) }) {
                    Text("Delete session", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseLog,
    onAddSet: (Long, String) -> Unit,
    onEditSet: (Long, String, Long, Int, Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Vol ${formatDouble(exercise.totalVolume, 0)}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        exercise.sets.forEachIndexed { index, set ->
            ListItem(
                modifier = Modifier.clickable {
                    onEditSet(
                        exercise.id,
                        exercise.name,
                        set.id,
                        set.reps,
                        set.weightKg
                    )
                },
                headlineContent = {
                    Text("Set ${index + 1}: ${formatDouble(set.weightKg, 1)} kg x ${set.reps}")
                },
                supportingContent = {
                    Text(
                        set.estimatedOneRmKg?.let { "Estimated 1RM ${formatDouble(it, 1)} kg" }
                            ?: "Estimated 1RM unavailable"
                    )
                }
            )
        }

        TextButton(onClick = { onAddSet(exercise.id, exercise.name) }) {
            Text("Add set")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExerciseDialog(
    suggestions: List<String>,
    initialName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onSave: (String, Int, Double) -> Unit
) {
    var repsInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var localName by remember(initialName) { mutableStateOf(initialName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val reps = repsInput.toIntOrNull()
                    val weight = weightInput.replace(',', '.').toDoubleOrNull()
                    if (localName.isBlank() || reps == null || reps <= 0 || weight == null || weight <= 0) {
                        errorMessage = "Enter exercise name, reps, and weight."
                    } else {
                        onSave(localName.trim(), reps, weight)
                    }
                },
                enabled = !isSaving
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        },
        title = { Text("Add Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = localName,
                    onValueChange = {
                        localName = it
                        onNameChange(it)
                    },
                    label = { Text("Exercise") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestions.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            AssistChip(
                                onClick = {
                                    localName = suggestion
                                    onNameChange(suggestion)
                                },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}

@Composable
private fun SetEditorDialog(
    target: SetEditorTarget,
    onDismiss: () -> Unit,
    onSave: (Int, Double) -> Unit,
    onDelete: () -> Unit
) {
    var repsInput by remember(target) { mutableStateOf(target.reps?.toString().orEmpty()) }
    var weightInput by remember(target) {
        mutableStateOf(target.weightKg?.let { formatDouble(it, 1) }.orEmpty())
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val reps = repsInput.toIntOrNull()
                    val weight = weightInput.replace(',', '.').toDoubleOrNull()
                    if (reps == null || reps <= 0 || weight == null || weight <= 0) {
                        errorMessage = "Enter reps and weight."
                    } else {
                        onSave(reps, weight)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (target.setId != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        title = { Text(if (target.setId == null) "Add Set" else "Edit Set") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(target.exerciseName)
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}

@Composable
private fun DeleteSessionDialog(
    target: DeleteSessionTarget,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Delete Session") },
        text = {
            Text("Delete ${target.sessionLabel}? All exercises and sets in this session will be removed.")
        }
    )
}

private fun localDateToEpochMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun epochMillisToLocalDate(value: Long): LocalDate {
    return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun formatDouble(value: Double, digits: Int): String {
    return "%.${digits}f".format(value)
}
