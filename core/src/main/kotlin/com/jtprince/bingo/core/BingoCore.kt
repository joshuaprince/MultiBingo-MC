package com.jtprince.bingo.core

import com.jtprince.bingo.core.scheduler.Scheduler
import com.jtprince.bingo.core.webclient.BingoUrlFormatter
import com.jtprince.bingo.core.webclient.WebHttpClient

class BingoCore(
    val config: BingoConfig,
    val scheduler: Scheduler,
) {
    val urlFormatter = BingoUrlFormatter(config.webUrl)
    val httpClient = WebHttpClient(scheduler, urlFormatter)

    init {
        httpClient.pingBackend()
    }
}
