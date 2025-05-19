package ru.hse.miem.cryptotrendreader.data

import kotlinx.coroutines.flow.Flow
import ru.hse.miem.cryptotrendreader.data.remote.DeribitWs
import timber.log.Timber

class DeribitRepository(
    private val ws: DeribitWs
) {
    fun tickerFlow(instr: String): Flow<Double> {
        Timber.e("DeribitRepository tickerFlow CALLED for $instr")
        return ws.flowPrice(instr)
    }

}