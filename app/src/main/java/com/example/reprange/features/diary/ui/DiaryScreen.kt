package com.example.reprange.features.diary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reprange.core.model.ExerciseHistory
import com.example.reprange.core.model.ExerciseHistoryEntry
import com.example.reprange.core.model.ExerciseLog
import com.example.reprange.core.model.ExerciseProgressPoint
import com.example.reprange.core.model.WorkoutDay
import com.example.reprange.core.model.WorkoutSession
import com.example.reprange.features.diary.presentation.DeleteSessionTarget
import com.example.reprange.features.diary.presentation.DiaryUiState
import com.example.reprange.features.diary.presentation.DiaryViewModel
import com.example.reprange.features.diary.presentation.DiaryViewModelFactory
import com.example.reprange.features.diary.presentation.ExerciseEditorTarget
import com.example.reprange.features.diary.presentation.SetEditorTarget
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters

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
        onOpenExerciseEditor = viewModel::openExerciseEditor,
        onDismissSetEditor = viewModel::dismissSetEditor,
        onSaveSet = viewModel::saveSet,
        onDeleteSet = viewModel::deleteEditedSet,
        onDismissExerciseEditor = viewModel::dismissExerciseEditor,
        onRenameExercise = viewModel::renameExercise,
        onDeleteExercise = viewModel::deleteExercise,
        onOpenExerciseHistory = viewModel::openExerciseHistory,
        onCloseExerciseHistory = viewModel::closeExerciseHistory,
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
    onOpenExerciseEditor: (Long, String) -> Unit,
    onDismissSetEditor: () -> Unit,
    onSaveSet: (Int, Double) -> Unit,
    onDeleteSet: () -> Unit,
    onDismissExerciseEditor: () -> Unit,
    onRenameExercise: (String) -> Unit,
    onDeleteExercise: () -> Unit,
    onOpenExerciseHistory: (String) -> Unit,
    onCloseExerciseHistory: () -> Unit,
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

    val history = state.exerciseHistory

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(history?.exerciseName ?: "Diary") },
                navigationIcon = {
                    if (history != null) {
                        IconButton(onClick = onCloseExerciseHistory) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (history != null) {
            ExerciseHistoryScreen(
                history = history,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        } else {
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
                    onOpenExerciseEditor = onOpenExerciseEditor,
                    onDeleteSessionRequest = onDeleteSessionRequest
                )
            }
        }
    }

    if (state.showDatePicker) {
        WorkoutCalendarDialog(
            selectedDate = state.selectedDate,
            workoutDates = state.workoutDates,
            onDismiss = onDismissDatePicker,
            onDatePicked = onDatePicked
        )
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

    state.exerciseEditorTarget?.let { target ->
        ExerciseEditorDialog(
            target = target,
            onDismiss = onDismissExerciseEditor,
            onRename = onRenameExercise,
            onDelete = onDeleteExercise,
            onOpenHistory = { onOpenExerciseHistory(target.exerciseName) }
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
    onOpenExerciseEditor: (Long, String) -> Unit,
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
                    onOpenExerciseEditor = onOpenExerciseEditor,
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
    onOpenExerciseEditor: (Long, String) -> Unit,
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
                    onEditSet = onEditSet,
                    onOpenExerciseEditor = onOpenExerciseEditor
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
    onEditSet: (Long, String, Long, Int, Double) -> Unit,
    onOpenExerciseEditor: (Long, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clickable { onOpenExerciseEditor(exercise.id, exercise.name) }
            )
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

@Composable
private fun WorkoutCalendarDialog(
    selectedDate: LocalDate,
    workoutDates: Set<LocalDate>,
    onDismiss: () -> Unit,
    onDatePicked: (LocalDate) -> Unit
) {
    var displayedMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                    Text("<")
                }
                Text(
                    text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                    Text(">")
                }
            }
        },
        text = {
            WorkoutCalendarMonth(
                displayedMonth = displayedMonth,
                selectedDate = selectedDate,
                workoutDates = workoutDates,
                onDatePicked = {
                    onDatePicked(it)
                }
            )
        }
    )
}

@Composable
private fun WorkoutCalendarMonth(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    workoutDates: Set<LocalDate>,
    onDatePicked: (LocalDate) -> Unit
) {
    val firstOfMonth = displayedMonth.atDay(1)
    val calendarStart = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = List(42) { calendarStart.plusDays(it.toLong()) }
    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val cellSpacing = 4.dp

    BoxWithConstraints {
        val totalSpacing = cellSpacing * 6
        val cellWidth = (maxWidth - totalSpacing) / 7

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(cellSpacing)
            ) {
                weekdays.forEach { weekday ->
                    Text(
                        text = weekday,
                        modifier = Modifier.width(cellWidth),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                ) {
                    week.forEach { date ->
                        CalendarDayCell(
                            date = date,
                            isCurrentMonth = date.month == displayedMonth.month,
                            isSelected = date == selectedDate,
                            hasWorkout = workoutDates.contains(date),
                            onClick = { onDatePicked(date) },
                            cellWidth = cellWidth
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    hasWorkout: Boolean,
    onClick: () -> Unit,
    cellWidth: androidx.compose.ui.unit.Dp
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }
    val dotColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .width(cellWidth)
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Box(
                modifier = Modifier.size(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasWorkout) {
                    Canvas(modifier = Modifier.size(6.dp)) {
                        drawCircle(color = dotColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseEditorDialog(
    target: ExerciseEditorTarget,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenHistory: () -> Unit
) {
    var nameInput by remember(target) { mutableStateOf(target.exerciseName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (nameInput.isBlank()) {
                        errorMessage = "Enter an exercise name."
                    } else {
                        onRename(nameInput.trim())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onOpenHistory) {
                    Text("View history")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete exercise", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        title = { Text("Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}

@Composable
private fun ExerciseHistoryScreen(
    history: ExerciseHistory,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("History overview", style = MaterialTheme.typography.titleMedium)
                Text("Logged ${history.entries.size} time(s)")
                Text(
                    history.highestWeightKg?.let { "Highest weight ${formatDouble(it, 1)} kg" }
                        ?: "Highest weight unavailable"
                )
                Text(
                    history.highestEstimatedOneRmKg?.let { "Best estimated 1RM ${formatDouble(it, 1)} kg" }
                        ?: "Best estimated 1RM unavailable"
                )
                Text("Total volume ${formatDouble(history.totalVolume, 0)} kg")
            }
        }

        if (history.oneRmPoints.isNotEmpty()) {
            ProgressChartCard(
                title = "Estimated 1RM over time",
                points = history.oneRmPoints,
                valueFormatter = { "${formatDouble(it, 1)} kg" }
            )
        }

        if (history.volumePoints.isNotEmpty()) {
            ProgressChartCard(
                title = "Volume over time",
                points = history.volumePoints,
                valueFormatter = { "${formatDouble(it, 0)} kg" }
            )
        }

        if (history.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history yet for this exercise.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history.entries, key = { it.exerciseEntryId }) { entry ->
                    ExerciseHistoryEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun ProgressChartCard(
    title: String,
    points: List<ExerciseProgressPoint>,
    valueFormatter: (Double) -> String
) {
    val latest = points.lastOrNull()
    val minValue = points.minOfOrNull { it.value }
    val maxValue = points.maxOfOrNull { it.value }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            latest?.let {
                Text(
                    text = "Latest ${valueFormatter(it.value)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (minValue != null && maxValue != null) {
                Text(
                    text = "Range ${valueFormatter(minValue)} - ${valueFormatter(maxValue)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            MiniLineChart(
                points = points,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = points.firstOrNull()?.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)).orEmpty(),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = points.lastOrNull()?.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)).orEmpty(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MiniLineChart(
    points: List<ExerciseProgressPoint>,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        if (points.isEmpty()) {
            return@Canvas
        }

        val horizontalPadding = 18.dp.toPx()
        val verticalPadding = 20.dp.toPx()
        val chartWidth = size.width - (horizontalPadding * 2)
        val chartHeight = size.height - (verticalPadding * 2)
        if (chartWidth <= 0f || chartHeight <= 0f) {
            return@Canvas
        }

        val minValue = points.minOf { it.value }
        val maxValue = points.maxOf { it.value }
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

        val offsets = points.mapIndexed { index, point ->
            val fractionX = if (points.size == 1) 0.5f else index.toFloat() / (points.lastIndex.toFloat())
            val normalizedY = ((point.value - minValue) / range).toFloat()
            Offset(
                x = horizontalPadding + chartWidth * fractionX,
                y = verticalPadding + chartHeight * (1f - normalizedY)
            )
        }

        drawRect(
            color = surfaceVariant.copy(alpha = 0.5f),
            topLeft = Offset(horizontalPadding, verticalPadding),
            size = Size(chartWidth, chartHeight)
        )

        val linePath = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { offset ->
                lineTo(offset.x, offset.y)
            }
        }
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 3.dp.toPx())
        )

        offsets.forEach { offset ->
            drawCircle(
                color = color,
                radius = 5.dp.toPx(),
                center = offset
            )
        }
    }
}

@Composable
private fun ExerciseHistoryEntryCard(
    entry: ExerciseHistoryEntry
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = entry.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.titleSmall
            )
            if (entry.sessionSortOrder > 0) {
                Text(
                    text = "Session ${entry.sessionSortOrder + 1}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            entry.sets.forEachIndexed { index, set ->
                Text(
                    "Set ${index + 1}: ${formatDouble(set.weightKg, 1)} kg x ${set.reps}  |  1RM ${set.estimatedOneRmKg?.let { formatDouble(it, 1) } ?: "-"}"
                )
            }
            Text(
                text = "Volume ${formatDouble(entry.totalVolume, 0)} kg",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatDouble(value: Double, digits: Int): String {
    return "%.${digits}f".format(value)
}
