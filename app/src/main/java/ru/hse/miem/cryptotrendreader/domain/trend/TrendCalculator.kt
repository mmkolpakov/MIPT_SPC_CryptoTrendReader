package ru.hse.miem.cryptotrendreader.domain.trend

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class TrendCalculator {
    data class Result(val slope: Double, val intercept: Double, val r2: Double, val prediction: Double, val stdErrSlope: Double)

    fun calculate(values: List<Double>): Result {
        val n = values.size
        if (n < 2) {
            return Result(0.0, values.firstOrNull() ?: 0.0, 0.0, values.firstOrNull() ?: 0.0, 0.0)
        }

        val sumX = (0 until n).sumOf { it.toDouble() }
        val sumY = values.sum()
        val sumX2 = (0 until n).sumOf { it.toDouble().pow(2) }
        val sumY2 = values.sumOf { it.pow(2) }
        val sumXY = values.withIndex().sumOf { (i, y) -> i.toDouble() * y }

        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX.pow(2))
        val intercept = (sumY - slope * sumX) / n

        if (slope.isNaN() || slope.isInfinite() || intercept.isNaN() || intercept.isInfinite()) {
            return Result(0.0, values.lastOrNull() ?: 0.0, 0.0, values.lastOrNull() ?: 0.0, 0.0)
        }

        val prediction = slope * n + intercept

        val sst = values.sumOf { (it - (sumY / n)).pow(2) }
        val ssr = values.withIndex().sumOf { (i, y) ->
            val predictedY = slope * i + intercept
            (predictedY - (sumY / n)).pow(2)
        }
        val sse = values.withIndex().sumOf { (i, y) ->
            val predictedY = slope * i + intercept
            (y - predictedY).pow(2)
        }

        val r2 = if (sst == 0.0) 1.0 else ssr / sst

        val stdErrSlope = if (n > 2) {
            val mse = sse / (n - 2)
            val sxx = sumX2 - sumX.pow(2) / n
            if (sxx > 0) sqrt(mse / sxx) else 0.0
        } else {
            0.0
        }


        return Result(slope, intercept, if(r2.isNaN()) 0.0 else r2, prediction, stdErrSlope)
    }
}