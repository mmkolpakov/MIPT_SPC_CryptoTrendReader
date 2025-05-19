package ru.hse.miem.cryptotrendreader.ui

import android.graphics.Color as AndroidColor
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.div.core.expression.variables.DivVariableController
import com.yandex.div.data.Variable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.hse.miem.cryptotrendreader.domain.model.CryptoUpdate
import ru.hse.miem.cryptotrendreader.domain.usecase.ObservePriceUseCase
import timber.log.Timber

class CryptoViewModel(private val useCase: ObservePriceUseCase) : ViewModel() {

    private var vc: DivVariableController? = null
    private var symbol = "BTC-PERPETUAL"
    private val loading = MutableStateFlow(false)

    private val neutralColorHex = "#FFE0E0E0"
    private val onDarkColorHex = "#FFFFFFFF"
    private val onLightColorHex = "#21005D"
    private val bullColorHex = "#386A20"
    private val bearColorHex = "#B3261E"
    private val neutralIconColorHex = "#79747E"
    private val errorIconColorHex = "#B3261E"
    private val errorContainerColorHex = "#F9DEDC"

    fun setupVariables(controller: DivVariableController) {
        vc = controller
        initVars()
    }

    private fun initVars() {
        vc?.apply {
            putOrUpdate(Variable.StringVariable("instrument_name_display", symbol))
            putOrUpdate(Variable.StringVariable("price", "---.--"))
            putOrUpdate(Variable.ColorVariable("trendColor", color(neutralColorHex)))
            putOrUpdate(Variable.ColorVariable("priceCardTextColor", color(onLightColorHex)))
            putOrUpdate(Variable.StringVariable("trendArrow", "…"))
            putOrUpdate(Variable.StringVariable("trendIconUrl", uri("ic_hourglass_empty")))
            putOrUpdate(Variable.ColorVariable("trendIconColor", color(neutralIconColorHex)))
            putOrUpdate(Variable.StringVariable("sparklineDataUri", ""))
            putOrUpdate(Variable.StringVariable("maeValue", "N/A"))
            putOrUpdate(Variable.StringVariable("pointsInWindowDisplay", "0/${useCase.windowSize}"))
            putOrUpdate(Variable.StringVariable("instrument_input", symbol))
            putOrUpdate(Variable.BooleanVariable("isLoading", loading.value))
            putOrUpdate(Variable.StringVariable("loadButtonText", "Загрузить"))
            putOrUpdate(Variable.BooleanVariable("isChartLoading", false))
            putOrUpdate(Variable.BooleanVariable("isChartEmpty", true))
            putOrUpdate(Variable.BooleanVariable("isChartVisible", false))
        }
    }

    fun startObservation(instr: String? = null) {
        val target = instr?.trim()?.uppercase().takeIf { !it.isNullOrBlank() } ?: symbol
        symbol = target
        viewModelScope.launch {
            useCase(target)
                .onStart {
                    setLoadingState(true)
                    withContext(Dispatchers.Main) {
                        vc?.apply {
                            putOrUpdate(Variable.StringVariable("instrument_name_display", target))
                            putOrUpdate(Variable.StringVariable("instrument_input", target))
                            putOrUpdate(Variable.StringVariable("price", "---.--"))
                            putOrUpdate(Variable.ColorVariable("priceCardTextColor", color(onLightColorHex)))
                            putOrUpdate(Variable.StringVariable("trendArrow", "…"))
                            putOrUpdate(Variable.StringVariable("trendIconUrl", uri("ic_hourglass_empty")))
                            putOrUpdate(Variable.ColorVariable("trendIconColor", color(neutralIconColorHex)))
                            putOrUpdate(Variable.StringVariable("sparklineDataUri", ""))
                            putOrUpdate(Variable.StringVariable("maeValue", "---"))
                            putOrUpdate(Variable.StringVariable("pointsInWindowDisplay", "0/${useCase.windowSize}"))
                            putOrUpdate(Variable.ColorVariable("trendColor", color(neutralColorHex)))
                            putOrUpdate(Variable.BooleanVariable("isChartLoading", true))
                            putOrUpdate(Variable.BooleanVariable("isChartEmpty", false))
                            putOrUpdate(Variable.BooleanVariable("isChartVisible", false))
                        }
                    }
                }
                .onEach { update(it) }
                .onCompletion { setLoadingState(false) }
                .launchIn(this)
        }
    }

    fun updateInstrument(input: String) {
        if (loading.value) return
        val up = input.trim().uppercase()
        if (up.isNotBlank() && up != symbol) useCase.setInstrument(up)
    }

    private suspend fun setLoadingState(isLoading: Boolean) {
        loading.value = isLoading
        withContext(Dispatchers.Main) {
            vc?.apply {
                putOrUpdate(Variable.BooleanVariable("isLoading", isLoading))
                putOrUpdate(Variable.StringVariable("loadButtonText", if (isLoading) "Загрузка..." else "Загрузить"))
                putOrUpdate(Variable.BooleanVariable("isChartLoading", isLoading))
                putOrUpdate(Variable.BooleanVariable("isChartEmpty", !isLoading))
                putOrUpdate(Variable.BooleanVariable("isChartVisible", false))
            }
        }
    }

    private suspend fun update(u: CryptoUpdate) {
        loading.value = false
        val textColor = when (u.trendColorHex.uppercase()) {
            bullColorHex.uppercase(), bearColorHex.uppercase() -> onDarkColorHex
            else -> onLightColorHex
        }
        Timber.d("isChartEmpty: ${u.sparklineDataUri.isBlank()}")
        withContext(Dispatchers.Main) {
            vc?.apply {
                putOrUpdate(Variable.StringVariable("instrument_name_display", u.instrumentNameDisplay))
                putOrUpdate(Variable.StringVariable("price", u.price))
                putOrUpdate(Variable.ColorVariable("trendColor", color(u.trendColorHex)))
                putOrUpdate(Variable.ColorVariable("priceCardTextColor", color(textColor)))
                putOrUpdate(Variable.StringVariable("trendArrow", u.trendArrow))
                putOrUpdate(Variable.StringVariable("trendIconUrl", uri(u.trendIconName)))
                putOrUpdate(Variable.ColorVariable("trendIconColor", color(u.trendIconColorString)))
                putOrUpdate(Variable.StringVariable("sparklineDataUri", u.sparklineDataUri))
                putOrUpdate(Variable.StringVariable("maeValue", u.mae))
                putOrUpdate(Variable.StringVariable("pointsInWindowDisplay", u.pointsInWindowDisplay))
                putOrUpdate(Variable.BooleanVariable("isLoading", false))
                putOrUpdate(Variable.StringVariable("loadButtonText", "Загрузить"))
                putOrUpdate(Variable.BooleanVariable("isChartLoading", false))
                putOrUpdate(Variable.BooleanVariable("isChartEmpty", u.sparklineDataUri.isBlank()))
                putOrUpdate(Variable.BooleanVariable("isChartVisible", u.sparklineDataUri.isNotBlank()))
            }
        }
    }

    private suspend fun setUiErrorState(errorMessage: String) {
        loading.value = false
        withContext(Dispatchers.Main) {
            vc?.apply {
                putOrUpdate(Variable.StringVariable("instrument_name_display", symbol))
                putOrUpdate(Variable.StringVariable("price", "Ошибка"))
                putOrUpdate(Variable.ColorVariable("trendColor", color(errorContainerColorHex)))
                putOrUpdate(Variable.ColorVariable("priceCardTextColor", color(onLightColorHex)))
                putOrUpdate(Variable.StringVariable("trendArrow", "⚠️"))
                putOrUpdate(Variable.StringVariable("trendIconUrl", uri("ic_error_outline")))
                putOrUpdate(Variable.ColorVariable("trendIconColor", color(errorIconColorHex)))
                putOrUpdate(Variable.StringVariable("sparklineDataUri", ""))
                putOrUpdate(Variable.StringVariable("maeValue", errorMessage.take(30)))
                putOrUpdate(Variable.StringVariable("pointsInWindowDisplay", "-/-"))
                putOrUpdate(Variable.BooleanVariable("isLoading", false))
                putOrUpdate(Variable.StringVariable("loadButtonText", "Загрузить"))
                putOrUpdate(Variable.BooleanVariable("isChartLoading", false))
                putOrUpdate(Variable.BooleanVariable("isChartEmpty", true))
                putOrUpdate(Variable.BooleanVariable("isChartVisible", false))
            }
        }
    }

    private fun color(hex: String) = runCatching { hex.toColorInt() }.getOrElse { AndroidColor.GRAY }
    private fun uri(name: String) = "android.resource://ru.hse.miem.cryptotrendreader/drawable/$name"
}
