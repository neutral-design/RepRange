package com.example.reprange.domain

import kotlin.math.ceil

object StrengthEstimator {

    fun estimate1Rm(weight: Double, reps: Int): OneRmEstimate {
        require(weight > 0 && reps > 0)
        val epley   = weight * (1.0 + reps / 30.0)
        val brzycki = weight * 36.0 / (37.0 - reps)
        val oconner = weight * (1.0 + reps / 40.0)
        return OneRmEstimate(epley, brzycki, oconner)
    }

    fun predictRepsAtWeight(targetWeight: Double, oneRm: OneRmEstimate): RepRange {
        require(targetWeight > 0)
        fun clamp(x: Double) = x.coerceIn(0.0, 30.0)

        val rEpley   = 30.0 * (oneRm.epley   / targetWeight - 1.0)
        val rBrzycki = 37.0 - 36.0 * targetWeight / oneRm.brzycki
        val rOconner = 40.0 * (oneRm.oconner / targetWeight - 1.0)

        val vals = listOf(rEpley, rBrzycki, rOconner).map(::clamp)
        val minV = vals.minOrNull() ?: 0.0
        val maxV = vals.maxOrNull() ?: 0.0

        return RepRange(minV.toInt(), ceil(maxV).toInt())
    }
}