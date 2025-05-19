package ru.hse.miem.cryptotrendreader.domain.model

data class CryptoUpdate(
    val instrumentNameDisplay: String,
    val price: String,
    val trendColorHex: String,
    val trendArrow: String,
    val trendIconName: String,
    val trendIconColorString: String,
    val sparklineDataUri: String,
    val mae: String,
    val pointsInWindowDisplay: String,
    val rawPrice: Double,
    val trendSlope: Double
)
