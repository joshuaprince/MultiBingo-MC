package com.jtprince.bingo.core

import com.jtprince.bingo.core.platform.BingoPlatform
import com.jtprince.bingo.core.webclient.BingoUrlFormatter
import com.jtprince.bingo.core.webclient.WebHttpClient

class BingoCore(platform: BingoPlatform) {
    val config = platform.config
    val urlFormatter = BingoUrlFormatter(platform.config.webUrl)
    val httpClient = WebHttpClient(platform.scheduler, urlFormatter)

    fun onEnable() {
        httpClient.pingBackend()
    }
}
