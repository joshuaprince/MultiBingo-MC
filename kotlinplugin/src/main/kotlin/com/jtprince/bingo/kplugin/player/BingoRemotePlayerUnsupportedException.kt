package com.jtprince.bingo.kplugin.player

/**
 * Raised when attempting to pass a Remote Bingo Player where a Local one is needed.
 */
class BingoRemotePlayerUnsupportedException(msg: String) : Exception(msg)
