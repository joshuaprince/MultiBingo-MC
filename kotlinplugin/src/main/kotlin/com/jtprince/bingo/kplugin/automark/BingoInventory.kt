package com.jtprince.bingo.kplugin.automark

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

interface BingoInventory {
    /**
     * All ItemStacks belonging to any players' inventory, armor slots and cursor item included.
     */
    val items: Collection<ItemStack>

    /**
     * All PlayerInventory objects, which includes information about item positions.
     */
    val playerInventories: Collection<PlayerInventory>
}
