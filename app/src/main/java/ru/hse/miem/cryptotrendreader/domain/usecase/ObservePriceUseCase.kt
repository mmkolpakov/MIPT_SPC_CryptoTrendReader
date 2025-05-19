package ru.hse.miem.cryptotrendreader.domain.usecase

import android.util.Base64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import ru.hse.miem.cryptotrendreader.core.RingBuffer
import ru.hse.miem.cryptotrendreader.data.DeribitRepository
import ru.hse.miem.cryptotrendreader.domain.model.CryptoUpdate
import ru.hse.miem.cryptotrendreader.domain.trend.TrendCalculator
import kotlin.math.*

class ObservePriceUseCase(
    private val repo: DeribitRepository,
    val windowSize: Int = 200,
    private val logThrottle: Long = 1000L,
    private val uiInterval: Long = 300L
) {
    private val buf = RingBuffer<Double>(windowSize)
    private val errBuf = RingBuffer<Double>(windowSize / 2)
    private val trend = TrendCalculator()
    private var pred: Double? = null
    private var lastLog = 0L
    private val instr = MutableStateFlow<String?>(null)
    private val minTrend = windowSize / 10
    private val minChart = 5

    fun setInstrument(i: String) = i.trim().uppercase().takeIf { it.isNotBlank() }?.let { instr.value = it }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    operator fun invoke(initial: String): Flow<CryptoUpdate> {
        setInstrument(initial)
        return instr.filterNotNull().flatMapLatest { ins ->
            buf.clear(); errBuf.clear(); pred = null
            repo.tickerFlow(ins)
                .conflate()
                .map { p ->
                    pred?.let { if (buf.size > 1) errBuf.add(abs(p - it)) }
                    buf.add(p)
                    val pts = buf.toListOrdered()
                    val res = if (pts.size >= 2) trend.calculate(pts) else null
                    val slope = res?.slope ?: 0.0
                    val forecast = res?.prediction ?: p
                    if (res != null) pred = forecast
                    logIfNeeded(res, pts.size)
                    CryptoUpdate(
                        instrumentNameDisplay = ins,
                        price = "%.2f".format(p),
                        trendColorHex = pickColor(slope, pts.size),
                        trendArrow = pickArrow(slope, pts.size),
                        trendIconName = pickIcon(slope, pts.size),
                        trendIconColorString = pickIconColor(slope, pts.size),
                        sparklineDataUri = spark(pts, pickLine(slope, pts.size)),
                        mae = if (errBuf.isEmpty || pts.size < minTrend) "N/A" else "%.2f".format(errBuf.toListOrdered().average()),
                        pointsInWindowDisplay = "${pts.size}/$windowSize",
                        rawPrice = p,
                        trendSlope = slope
                    )
                }
                .sample(uiInterval)
        }
    }

    private fun logIfNeeded(r: TrendCalculator.Result?, n: Int) {
        if (r == null) return
        val t = System.currentTimeMillis()
        if (n > minTrend && t - lastLog >= logThrottle) lastLog = t
    }

    private fun threshold(vals: List<Double>): Double {
        if (vals.size < minTrend) return 0.0001
        val ret = vals.zipWithNext { a, b -> (b - a) / a }
        val m = ret.average()
        val sd = sqrt(ret.sumOf { (it - m).pow(2) } / ret.size)
        return sd * 0.5
    }

    private fun pickColor(s: Double, n: Int): String = when {
        n < minTrend -> "#FFE0E0E0"
        s > threshold(buf.toListOrdered()) -> "#FF4CAF50"
        s < -threshold(buf.toListOrdered()) -> "#FFB3261E"
        else -> "#FFE0E0E0"
    }

    private fun pickLine(s: Double, n: Int): String = when {
        n < minTrend -> "#625B71"
        s > threshold(buf.toListOrdered()) -> "#4CAF50"
        s < -threshold(buf.toListOrdered()) -> "#B3261E"
        else -> "#625B71"
    }

    private fun pickArrow(s: Double, n: Int): String = when {
        n < minTrend -> "…"
        s > threshold(buf.toListOrdered()) -> "▲"
        s < -threshold(buf.toListOrdered()) -> "▼"
        else -> "–"
    }

    private fun pickIcon(s: Double, n: Int): String = when {
        n < minTrend -> "ic_hourglass_empty"
        s > threshold(buf.toListOrdered()) -> "ic_arrow_upward"
        s < -threshold(buf.toListOrdered()) -> "ic_arrow_downward"
        else -> "ic_horizontal_rule"
    }

    private fun pickIconColor(s: Double, n: Int): String = when {
        n < minTrend -> "#79747E"
        s > threshold(buf.toListOrdered()) -> "#386A20"
        s < -threshold(buf.toListOrdered()) -> "#B3261E"
        else -> "#79747E"
    }

    private fun spark(vals: List<Double>, c: String): String {
        if (vals.size < minChart) return ""
        val w = 600
        val h = 100
        val min = vals.minOrNull() ?: return ""
        val max = vals.maxOrNull() ?: return ""
        val rng = max - min
        if (rng == 0.0) return ""
        val pts = vals.mapIndexed { i, v ->
            val x = i * w.toFloat() / (vals.size - 1)
            val y = h - ((v - min) / rng * h).toFloat()
            "${x.roundToInt()},${y.roundToInt()}"
        }
        val area = "M0,$h L${pts.joinToString(" L")} L$w,$h Z"
        val poly = pts.joinToString(" ")
        val svg = "<svg xmlns='http://www.w3.org/2000/svg' width='$w' height='$h' viewBox='0 0 $w $h'><defs><linearGradient id='g' x1='0' y1='0' x2='0' y2='1'><stop offset='0%' stop-color='$c' stop-opacity='0.3'/><stop offset='100%' stop-color='$c' stop-opacity='0'/></linearGradient></defs><path d='$area' fill='url(#g)'/><polyline points='$poly' fill='none' stroke='$c' stroke-width='4' stroke-linejoin='round' stroke-linecap='round'/></svg>"
        val data = Base64.encodeToString(svg.toByteArray(), Base64.NO_WRAP)
        return "data:image/svg+xml;base64,$data"
    }
}
