package com.example.reprange.presentation

import com.example.reprange.domain.OneRmEstimate

data class PredictionRow(val weight: Double, val minReps: Int, val maxReps: Int)

data class PredictorUiState(
    val weightInput: String = "",
    val repsInput: String = "",
    val increments: List<Double> = listOf(2.5, 5.0, 7.5, 10.0, 12.5, 15.0, 17.5, 20.0),
    val predictions: List<PredictionRow> = emptyList(),
    val oneRm: OneRmEstimate? = null,
    val error: String? = null,
    val isCalculated: Boolean = false
)