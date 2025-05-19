package ru.hse.miem.cryptotrendreader.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DeribitApi(private val client: HttpClient) {
    suspend fun ticker(instr: String): TickerDto =
        client.get("https://test.deribit.com/api/v2/public/ticker") {
            parameter("instrument_name", instr)
        }.body()
    @Serializable
    data class TickerDto(@SerialName("result") val result: Result) {
        @Serializable
        data class Result(@SerialName("last_price") val price: Double)
    }
}