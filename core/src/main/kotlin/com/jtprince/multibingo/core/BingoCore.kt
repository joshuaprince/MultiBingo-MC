package com.jtprince.multibingo.core

import com.jtprince.multibingo.core.platform.BingoConfig
import com.jtprince.multibingo.core.platform.BingoPlatform
import com.jtprince.multibingo.core.webclient.BingoUrlFormatter
import com.jtprince.multibingo.core.webclient.WebHttpClient

class BingoCore(val platform: BingoPlatform) {
    val config: BingoConfig = platform.config
    val urlFormatter = BingoUrlFormatter(platform.config.webUrl)
    val httpClient = WebHttpClient(urlFormatter)

    companion object {
        lateinit var INSTANCE: BingoCore
    }

    init {
        INSTANCE = this
    }

    fun enable() {
        httpClient.pingBackend()
    }
}
