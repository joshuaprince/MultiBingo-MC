package com.jtprince.multibingo.core.platform

import com.jtprince.multibingo.core.automark.AutoMarkTriggerFactory
import org.slf4j.Logger

interface BingoPlatform {
    val logger: Logger
    val config: BingoConfig
    val autoMarkFactory: AutoMarkTriggerFactory
}
