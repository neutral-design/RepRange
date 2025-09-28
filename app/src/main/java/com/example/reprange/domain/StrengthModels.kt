package com.example.reprange.domain

data class OneRmEstimate(val epley: Double, val brzycki: Double, val oconner: Double) {
    val avg get() = (epley + brzycki + oconner) / 3.0
    val min get() = minOf(epley, brzycki, oconner)
    val max get() = maxOf(epley, brzycki, oconner)
}

data class RepRange(val minReps: Int, val maxReps: Int)