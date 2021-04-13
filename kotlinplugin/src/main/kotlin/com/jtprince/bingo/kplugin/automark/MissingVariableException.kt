package com.jtprince.bingo.kplugin.automark

/**
 * Raised when an automated trigger relies on a variable, but that variable does not exist in
 * a provided SetVariables map.
 */
class MissingVariableException(val varname: String?) : Exception() {
    override fun toString(): String {
        return "Missing variable $varname"
    }
}
