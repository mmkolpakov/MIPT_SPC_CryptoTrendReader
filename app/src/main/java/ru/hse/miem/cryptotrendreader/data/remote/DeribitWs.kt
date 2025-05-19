package ru.hse.miem.cryptotrendreader.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

@Serializable
data class DeribitRequest(
    val jsonrpc: String = "2.0",
    val id: Long,
    val method: String,
    val params: JsonElement? = null
)

@Serializable
data class DeribitTickerData(
    val last_price: Double,
    val instrument_name: String,
    val timestamp: Long
)

@Serializable
data class DeribitSubscriptionParams<T>(
    val channel: String,
    val data: T
)

@Serializable
data class DeribitBaseIncomingMessage(
    val jsonrpc: String? = null,
    val method: String? = null,
    val params: DeribitSubscriptionParams<DeribitTickerData>? = null,
    val error: DeribitResponseError? = null,
    val id: Long? = null,
    val result: JsonElement? = null
)

@Serializable
data class DeribitResponseError(
    val code: Int,
    val message: String
)

class DeribitWs(private val client: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val reqId = AtomicLong(System.currentTimeMillis())

    fun flowPrice(instrumentName: String): Flow<Double> = channelFlow {
        val ch = "ticker.$instrumentName.100ms"
        client.webSocketSession("wss://test.deribit.com/ws/api/v2").apply {
            sendJson(
                DeribitRequest(
                    id = next(),
                    method = "public/set_heartbeat",
                    params = buildJsonObject { put("interval", JsonPrimitive(10)) }
                )
            )
            sendJson(
                DeribitRequest(
                    id = next(),
                    method = "public/subscribe",
                    params = buildJsonObject { put("channels", JsonArray(listOf(JsonPrimitive(ch)))) }
                )
            )
            launch {
                while (isActive) {
                    delay(10_000)
                    sendJson(DeribitRequest(id = next(), method = "public/test"))
                }
            }
            incoming.consumeAsFlow()
                .filterIsInstance<Frame.Text>()
                .mapNotNull { handleFrame(it.readText(), ch, this) }
                .retryWhen { e, a ->
                    if (e is CancellationException) false else {
                        delay(min(2000 * (a + 1), 15000))
                        true
                    }
                }
                .collect { send(it) }
        }
    }

    private suspend fun handleFrame(raw: String, ch: String, s: io.ktor.websocket.WebSocketSession): Double? {
        val m = runCatching { json.decodeFromString(DeribitBaseIncomingMessage.serializer(), raw) }.getOrNull() ?: return null
        return when {
            m.method == "subscription" && m.params?.channel == ch -> m.params.data.last_price
            m.method == "test_request" -> { s.sendJson(DeribitRequest(id = next(), method = "public/test")); null }
            m.method == "heartbeat" -> null
            else -> null
        }
    }

    private suspend fun io.ktor.websocket.WebSocketSession.sendJson(r: DeribitRequest) =
        send(Frame.Text(json.encodeToString(DeribitRequest.serializer(), r)))

    private fun next(): Long = reqId.getAndIncrement()
}
