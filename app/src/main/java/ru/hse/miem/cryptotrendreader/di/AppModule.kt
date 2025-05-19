package ru.hse.miem.cryptotrendreader.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.hse.miem.cryptotrendreader.data.DeribitRepository
import ru.hse.miem.cryptotrendreader.data.remote.DeribitApi
import ru.hse.miem.cryptotrendreader.data.remote.DeribitWs
import ru.hse.miem.cryptotrendreader.domain.usecase.ObservePriceUseCase
import ru.hse.miem.cryptotrendreader.ui.CryptoViewModel
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

val appModule = module {
    single {
        HttpClient(CIO) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(WebSockets) {
                pingInterval = 20.seconds
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.tag("KtorHttpClient").v(message)
                    }
                }
                level = LogLevel.ALL
            }
        }
    }
    single { DeribitApi(get()) }
    single { DeribitWs(get()) }
    single { DeribitRepository(get()) }
    single { ObservePriceUseCase(get()) }
    viewModel { CryptoViewModel(get()) }
}