package ru.hse.miem.cryptotrendreader.core

import android.content.Context
import org.json.JSONObject
import java.io.IOException

class DivAssetReader(
    private val context: Context,
    private val offlineMode: Boolean = false
) {

    fun read(filename: String): JSONObject {
        val data = IOUtils.toString(context.assets.open(filename))
            .replaceOnlineUrls()
            .replaceUnsupportedUnicodeSymbols()
        return JSONObject(data)
    }

    fun tryRead(filename: String): JSONObject? {
        return try {
            read(filename)
        } catch (e: IOException) {
            null
        }
    }

    private fun String.replaceOnlineUrls(): String {
        return if (offlineMode) {
            replace("https://alicekit.s3.yandex.net", "file:///android_asset/div-screenshots")
        } else {
            this
        }
    }

    private fun String.replaceUnsupportedUnicodeSymbols(): String {
        return this
    }

}