package com.jtprince.bingo.bukkit.automark

import com.jtprince.bingo.core.automark.itemtrigger.BingoItemStack
import org.bukkit.inventory.ItemStack

/**
 * Wrapper for Bukkit's [ItemStack] so that the Bingo core can process it and calculate Item Trigger
 * completion.
 */
class BukkitBingoItemStack(
    val itemStack: ItemStack
) : BingoItemStack {
    override val namespacedId: String
        get() = itemStack.type.key.asString()
    override val quantity: Int
        get() = itemStack.amount

    companion object {
        fun ItemStack.toBingoItem(): BukkitBingoItemStack = BukkitBingoItemStack(this)

        fun Iterable<ItemStack>.toBingoItems(): Collection<BukkitBingoItemStack> {
            return this.map { it.toBingoItem() }
        }
    }
}
