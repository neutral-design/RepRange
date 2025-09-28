package com.example.reprange.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reprange.presentation.PredictorViewModel
import com.example.reprange.presentation.PredictionRow
import com.example.reprange.presentation.PredictorUiState

@Composable
fun RepsPredictorRoute(vm: PredictorViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    RepsPredictorScreen(
        state = state,
        onWeightChange = vm::onWeightChange,
        onRepsChange = vm::onRepsChange,
        onCalculate = vm::calculate
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepsPredictorScreen(
    state: PredictorUiState,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onCalculate: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Rep range-kalkylator") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.weightInput,
                onValueChange = onWeightChange,
                label = { Text("Vikt (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.repsInput,
                onValueChange = onRepsChange,
                label = { Text("Reps") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = onCalculate, modifier = Modifier.fillMaxWidth()) {
                Text("Beräkna")
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            state.oneRm?.let { est ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Estimerad 1RM (snitt): ${est.avg.format(1)} kg")
                        Text(
                            "Spann: ${est.min.format(1)} – ${est.max.format(1)} kg",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (state.predictions.isNotEmpty()) {
                Text("Prediktioner", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    items(state.predictions) { row ->
                        PredictionItem(row)
                        Divider()
                    }
                }

                Text(
                    "Uppskattningar. Använd som riktmärke – känn efter i passet.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PredictionItem(row: PredictionRow) {
    ListItem(
        headlineContent = { Text("${row.weight.format(1)} kg") },
        supportingContent = { Text("${row.minReps}–${row.maxReps} reps") }
    )
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)