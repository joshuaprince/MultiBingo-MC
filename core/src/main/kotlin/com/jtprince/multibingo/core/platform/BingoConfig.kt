package com.jtprince.multibingo.core.platform

/**
 * Describes global configuration to use for all Bingo features. Individual platforms should
 * implement their own strategy of providing this.
 */
interface BingoConfig {
    /**
     * If true, extra debug features (such as verbose logging and goal debugging) should be enabled.
     */
    val debug: Boolean

    /**
     * The URL to use for the backend webserver, in the format "https://my.bingo.com"
     */
    val webUrl: String
}
