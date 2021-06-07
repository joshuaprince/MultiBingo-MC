package com.jtprince.bingo.core.automark.itemtrigger

/**
 * Adapter class to ItemTrigger that wraps any implementation of a Minecraft Item stack.
 */
interface BingoItemStack {
    /**
     * Example: "minecraft:stone"
     */
    val namespacedId: String

    /**
     * Number of items in this item stack.
     */
    val quantity: Int
}
