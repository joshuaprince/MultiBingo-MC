package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.player.BingoPlayer
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestItemTrigger {
    private val yaml = ItemTriggerYaml.fromFile(javaClass.getResourceAsStream("/test_item_triggers.yml"))

    @Test
    fun `A single item is detected only when it is present`() {
        val t = makeTrigger("jm_book_quill") // 1 Book and Quill
        val inv = MockBingoInventory()
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.WRITABLE_BOOK, 1)
        assertTrue(t.satisfiedBy(inv))
    }

    @Test
    fun `Multiple unique items are detected only when unique`() {
        val t = makeTrigger("jm_gold_items") // 4 Unique Gold Armor Pieces
        val inv = MockBingoInventory(
            ItemStack(Material.GOLDEN_CHESTPLATE, 1)
        )
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.GOLDEN_HELMET, 1)
        inv.items += ItemStack(Material.GOLDEN_BOOTS, 1)
        assertFalse(t.satisfiedBy(inv))

        // Another Golden Boots is not unique, so it should not count as an acceptable 4th item.
        inv.items += ItemStack(Material.GOLDEN_BOOTS, 1)
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.GOLDEN_LEGGINGS, 1)
        assertTrue(t.satisfiedBy(inv))
    }

    @Test
    fun `Multiple stacks of items are counted together`() {
        val t = makeTrigger("jm_coarse_dirt") // 64 Coarse Dirt
        val inv = MockBingoInventory(
            ItemStack(Material.COARSE_DIRT, 32)
        )
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.COARSE_DIRT, 32)
        assertTrue(t.satisfiedBy(inv))
    }

    @Test
    fun `Regular expression item names detect unique items separately`() {
        val t = makeTrigger("jm_wool_colors") // 5 Colors of Wool
        val inv = MockBingoInventory(
            ItemStack(Material.BLACK_WOOL, 64)
        )
        assertFalse(t.satisfiedBy(inv))

        inv.items += ItemStack(Material.RED_WOOL, 64)
        inv.items += ItemStack(Material.ORANGE_WOOL, 64)
        inv.items += ItemStack(Material.YELLOW_WOOL, 64)
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.YELLOW_WOOL, 64)
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.PURPLE_TERRACOTTA, 64)
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.GREEN_WOOL, 32)
        assertTrue(t.satisfiedBy(inv))
    }

    @Test
    fun `Items with multiple options to collect and a total greater than 1 can count together`() {
        val t = makeTrigger("jm_pistons") // 10 Pistons
        var inv = MockBingoInventory(
            ItemStack(Material.PISTON, 9)
        )
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.STICKY_PISTON, 1)
        assertTrue(t.satisfiedBy(inv))

        inv = MockBingoInventory(
            ItemStack(Material.STICKY_PISTON, 10)
        )
        assertTrue(t.satisfiedBy(inv))

        inv = MockBingoInventory()
        for (i in 0..9) {
            assertFalse(t.satisfiedBy(inv))
            inv.items += ItemStack(Material.PISTON, 1)
        }
        assertTrue(t.satisfiedBy(inv))
    }

    @Test
    fun `Named variables work`() {
        val t = makeTrigger("jm_ender_pearls", mapOf("var" to 15)) // "$var" Ender Pearls
        val inv = MockBingoInventory(
            ItemStack(Material.ENDER_PEARL, 14)
        )
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.ENDER_PEARL, 1)
        assertTrue(t.satisfiedBy(inv))
    }

    @Test
    fun `Basic item groups work with variables`() {
        val t = makeTrigger("jm_different_fish", mapOf("varFish" to 3)) // "$varFish" Different Fish
        val inv = MockBingoInventory(
            ItemStack(Material.COD, 10),
            ItemStack(Material.COOKED_COD, 10),
            ItemStack(Material.COD_BUCKET, 10),
        )
        assertFalse(t.satisfiedBy(inv))

        inv.items += ItemStack(Material.SALMON, 1)
        inv.items += ItemStack(Material.COOKED_SALMON, 1)
        inv.items += ItemStack(Material.SALMON_BUCKET, 1)
        assertFalse(t.satisfiedBy(inv))

        inv.items += ItemStack(Material.TROPICAL_FISH_BUCKET, 1)
        assertTrue(t.satisfiedBy(inv))
    }

    @Test
    fun `Complex item groups with regex work`() {
        val t = makeTrigger("jm_different_edible") // 6 Different Edible Items
        val inv = MockBingoInventory(
            ItemStack(Material.APPLE, 64),
            ItemStack(Material.BEETROOT, 64),
            ItemStack(Material.MUSHROOM_STEW, 1),
            ItemStack(Material.POTATO, 1),
            ItemStack(Material.BAKED_POTATO, 1),
            ItemStack(Material.BEEF, 1),
            ItemStack(Material.COOKED_BEEF, 1),
        )
        assertFalse(t.satisfiedBy(inv))
        inv.items += ItemStack(Material.RABBIT_STEW, 1)
        assertTrue(t.satisfiedBy(inv))
    }

    @Test
    fun `Mixture of total and unique requirements work`() {
        val t = makeTrigger("jm_roses_dandelions") // 10 Roses + 15 Dandelions
        val inv = MockBingoInventory(
            ItemStack(Material.ROSE_BUSH, 9),
            ItemStack(Material.DANDELION, 14),
        )
        assertFalse(t.satisfiedBy(inv))

        inv.items += ItemStack(Material.DANDELION, 64)
        assertFalse(t.satisfiedBy(inv))

        inv.items += ItemStack(Material.ROSE_BUSH, 1)
        assertTrue(t.satisfiedBy(inv))
    }

    private val mockEventPlayerMapper = object: EventPlayerMapper {
        /* No need for a full implementation, we're just testing satisfiedBy in this test */
        override fun mapEvent(event: Event): BingoPlayer? = null
        override val allPlayers: Collection<BingoPlayer> = emptyList()
    }

    private fun makeTrigger(goalId: String, vars: SetVariables = emptyMap()): ItemTrigger {
        return ItemTrigger(goalId, 0, vars, mockEventPlayerMapper, null, null, yaml[goalId])
    }
}
