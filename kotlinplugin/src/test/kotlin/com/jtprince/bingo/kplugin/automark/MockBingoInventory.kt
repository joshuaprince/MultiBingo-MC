package com.jtprince.bingo.kplugin.automark

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * Mock class for passing items to auto mark triggers. All fields are mutable.
 */
class MockBingoInventory(vararg items: ItemStack) : BingoInventory {
    override val items = mutableListOf(*items)
    override val playerInventories = mutableListOf<PlayerInventory>()
}
