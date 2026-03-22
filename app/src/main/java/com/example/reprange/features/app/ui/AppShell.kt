package com.example.reprange.features.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.reprange.core.data.WorkoutRepository
import com.example.reprange.core.export.WorkoutCsvExporter
import com.example.reprange.core.model.AppStats
import com.example.reprange.core.model.ExerciseHistory
import com.example.reprange.core.settings.AppPreferencesRepository
import com.example.reprange.features.diary.presentation.DiaryViewModelFactory
import com.example.reprange.features.diary.ui.DiaryRoute
import com.example.reprange.features.shared.ui.ExerciseHistoryOverview
import kotlinx.coroutines.launch

private enum class AppTab(val label: String) {
    Diary("Diary"),
    Stats("Stats"),
    Settings("Settings")
}

@Composable
fun RepRangeApp(
    repository: WorkoutRepository,
    preferencesRepository: AppPreferencesRepository
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Diary) }
    val diaryFactory = remember(repository, preferencesRepository) {
        DiaryViewModelFactory(repository, preferencesRepository)
    }
    val stats by repository.observeAppStats().collectAsState(initial = AppStats())
    val allExerciseNames by repository.observeAllExerciseNames().collectAsState(initial = emptyList())
    val targetReps by preferencesRepository.targetRepsFlow.collectAsState(initial = AppPreferencesRepository.DEFAULT_TARGET_REPS)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exporter = remember(repository) { WorkoutCsvExporter(repository) }
    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.label.take(1)) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.Diary -> DiaryRoute(
                factory = diaryFactory,
                modifier = Modifier.padding(padding)
            )
            AppTab.Stats -> StatsScreen(
                stats = stats,
                allExerciseNames = allExerciseNames,
                repository = repository,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
            AppTab.Settings -> SettingsScreen(
                isExporting = isExporting,
                targetReps = targetReps,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                onTargetRepsChange = { value ->
                    scope.launch {
                        preferencesRepository.setTargetReps(value)
                    }
                },
                onExportCsv = {
                    scope.launch {
                        isExporting = true
                        runCatching {
                            val file = exporter.export(context)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export workout CSV"))
                        }.onSuccess {
                            snackbarHostState.showSnackbar("CSV export ready to share.")
                        }.onFailure {
                            snackbarHostState.showSnackbar("Could not export CSV.")
                        }
                        isExporting = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsScreen(
    stats: AppStats,
    allExerciseNames: List<String>,
    repository: WorkoutRepository,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedExercise by rememberSaveable { mutableStateOf<String?>(null) }
    val filteredExercises = remember(allExerciseNames, query) {
        if (query.isBlank()) {
            allExerciseNames.take(12)
        } else {
            allExerciseNames.filter { it.contains(query.trim(), ignoreCase = true) }.take(12)
        }
    }
    val selectedHistory = selectedExercise?.let { exerciseName ->
        repository.observeExerciseHistory(exerciseName)
            .collectAsState(initial = ExerciseHistory(exerciseName, emptyList()))
            .value
    }

    LaunchedEffect(selectedExercise) {
        if (selectedExercise != null) {
            query = selectedExercise.orEmpty()
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Stats", style = MaterialTheme.typography.headlineMedium)
        }
        item { StatCard("Workout days", stats.workoutDays.toString()) }
        item { StatCard("Sessions", stats.sessions.toString()) }
        item { StatCard("Exercises logged", stats.exercises.toString()) }
        item { StatCard("Sets logged", stats.sets.toString()) }
        item { StatCard("Total volume", "${"%.0f".format(stats.totalVolumeKg)} kg") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Exercise stats", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.isBlank()) selectedExercise = null
                        },
                        label = { Text("Choose exercise") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (filteredExercises.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredExercises.forEach { exerciseName ->
                                AssistChip(
                                    onClick = {
                                        selectedExercise = exerciseName
                                        query = exerciseName
                                    },
                                    label = { Text(exerciseName) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            val history = selectedHistory
            if (selectedExercise == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Pick an exercise", style = MaterialTheme.typography.titleMedium)
                        Text("Select one of your logged exercises to see charts and detailed history.")
                    }
                }
            } else if (history != null) {
                ExerciseHistoryOverview(history = history)
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun SettingsScreen(
    isExporting: Boolean,
    targetReps: Int,
    onTargetRepsChange: (Int) -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    var targetRepsInput by remember(targetReps) { mutableStateOf(targetReps.toString()) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Training suggestions", style = MaterialTheme.typography.titleMedium)
                Text("Set dialogs can suggest a target weight based on your recent logged sets.")
                OutlinedTextField(
                    value = targetRepsInput,
                    onValueChange = {
                        targetRepsInput = it
                        it.toIntOrNull()?.takeIf { reps -> reps in 1..30 }?.let(onTargetRepsChange)
                    },
                    label = { Text("Target reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Your Data", style = MaterialTheme.typography.titleMedium)
                Text(
                    "RepRange stores your workouts locally on your device. You can export your data as CSV and share it with apps like Drive, Files, or mail."
                )
                Button(
                    onClick = onExportCsv,
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp)
                        )
                    }
                    Text("Export CSV")
                }
            }
        }
    }
}
