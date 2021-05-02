package com.jtprince.util

import kotlin.math.max

object KotlinUtils {
    /**
     * Increment the value in a map by a given amount, adding the key to the map and behaving as
     * though it was previously set to 0 if the key is not present.
     *
     * Example: `mutableMapOf("a" to 3).increment("a") -> ("a" to 4)`
     *
     * Example: `mutableMapOf("a" to 3).increment("b", by = 2) -> ("a" to 3, "b" to 2)`
     *
     * @return The new value associated to the key.
     */
    fun <T> MutableMap<T, Int>.increment(key: T, by: Int = 1): Int {
        return when (val count = this[key]) {
            null -> { this[key] = by; by }
            else -> {
                val new = count + by
                this[key] = new
                new
            }
        }
    }

    /**
     * Decrement the value in a map by a given amount if present, not allowing the value to go
     * below zero. If the key is not present in the map, does nothing and returns 0.
     *
     * Example: `mutableMapOf("a" to 3).decrement("a") -> ("a" to 2)`
     *
     * Example: `mutableMapOf("a" to 3).decrement("a", by = 4) -> ("a" to 0)`
     *
     * @return The new value associated to the key.
     */
    fun <T> MutableMap<T, Int>.decrement(key: T, by: Int = 1): Int {
        return when (val count = this[key]) {
            null -> 0
            else -> {
                val new = max(count - by, 0)
                this[key] = new
                new
            }
        }
    }
}
