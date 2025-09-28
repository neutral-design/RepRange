package com.example.reprange.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.example.reprange.domain.StrengthEstimator
import com.example.reprange.domain.OneRmEstimate

class PredictorViewModel : ViewModel() {

    private val _state = MutableStateFlow(PredictorUiState())
    val state: StateFlow<PredictorUiState> = _state

    fun onWeightChange(text: String) {
        _state.update { it.copy(weightInput = text, isCalculated = false, error = null) }
    }

    fun onRepsChange(text: String) {
        _state.update { it.copy(repsInput = text, isCalculated = false, error = null) }
    }

    fun calculate() {
        val s = _state.value
        val weight = s.weightInput.replace(',', '.').toDoubleOrNull()
        val reps = s.repsInput.toIntOrNull()

        if (weight == null || weight <= 0) {
            _state.update { it.copy(error = "Ange en giltig vikt > 0.", isCalculated = false) }
            return
        }
        if (reps == null || reps <= 0) {
            _state.update { it.copy(error = "Ange giltigt antal reps (> 0).", isCalculated = false) }
            return
        }

        val oneRm: OneRmEstimate = StrengthEstimator.estimate1Rm(weight, reps)
        val preds = s.increments.map { inc ->
            val w = weight + inc
            val rr = StrengthEstimator.predictRepsAtWeight(w, oneRm)
            PredictionRow(weight = w, minReps = rr.minReps, maxReps = rr.maxReps)
        }

        _state.update {
            it.copy(
                predictions = preds,
                oneRm = oneRm,
                error = null,
                isCalculated = true
            )
        }
    }
}