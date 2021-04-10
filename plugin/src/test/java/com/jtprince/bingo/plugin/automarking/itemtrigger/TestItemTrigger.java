package com.jtprince.bingo.plugin.automarking.itemtrigger;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify the accuracy of Item Triggers. Ensure that they only activate when the player
 * actually has items that should satisfy them.
 */
public class TestItemTrigger {
    final ItemTriggerYaml yaml;
    final Map<String, Integer> testVariables = new HashMap<>();

    public TestItemTrigger() {
        yaml = new TestItemTriggerYaml().yaml;
        testVariables.put("var", 15);  // jm_ender_pearls
        testVariables.put("varFish", 3);  // jm_different_fish
    }

    @Test
    void testSingleItem() {
        ItemTrigger t = makeTrigger("jm_book_quill");  // 1 Book and Quill

        ArrayList<ItemStack> inv = new ArrayList<>();
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.WRITABLE_BOOK, 1));
        assertTrue(t.isSatisfiedBy(inv));
    }

    @Test
    void testMultipleUnique() {
        ItemTrigger t = makeTrigger("jm_gold_items");  // 4 Unique Gold Armor Pieces

        ArrayList<ItemStack> inv = new ArrayList<>();
        inv.add(new ItemStack(Material.GOLDEN_CHESTPLATE, 1));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.GOLDEN_HELMET, 1));
        inv.add(new ItemStack(Material.GOLDEN_BOOTS, 1));
        assertFalse(t.isSatisfiedBy(inv));

        // Another Golden Boots is not unique, so it should not count as an acceptable 4th item.
        inv.add(new ItemStack(Material.GOLDEN_BOOTS, 1));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.GOLDEN_LEGGINGS, 1));
        assertTrue(t.isSatisfiedBy(inv));
    }

    @Test
    void testStack() {
        ItemTrigger t = makeTrigger("jm_coarse_dirt");  // 64 Coarse Dirt

        ArrayList<ItemStack> inv = new ArrayList<>();
        inv.add(new ItemStack(Material.COARSE_DIRT, 32));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.COARSE_DIRT, 32));
        assertTrue(t.isSatisfiedBy(inv));
    }

    @Test
    void testRegexName() {
        ItemTrigger t = makeTrigger("jm_wool_colors");  // 5 Colors of Wool

        ArrayList<ItemStack> inv = new ArrayList<>();
        inv.add(new ItemStack(Material.BLACK_WOOL, 64));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.RED_WOOL, 64));
        inv.add(new ItemStack(Material.ORANGE_WOOL, 64));
        inv.add(new ItemStack(Material.YELLOW_WOOL, 64));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.YELLOW_WOOL, 64));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.PURPLE_TERRACOTTA, 64));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.GREEN_WOOL, 32));
        assertTrue(t.isSatisfiedBy(inv));
    }

    @Test
    void testOptionsStack() {
        ItemTrigger t = makeTrigger("jm_pistons");  // 10 Pistons

        ArrayList<ItemStack> inv = new ArrayList<>();
        inv.add(new ItemStack(Material.PISTON, 9));
        assertFalse(t.isSatisfiedBy(inv));
        inv.add(new ItemStack(Material.STICKY_PISTON, 1));
        assertTrue(t.isSatisfiedBy(inv));

        inv.clear();
        inv.add(new ItemStack(Material.STICKY_PISTON, 10));
        assertTrue(t.isSatisfiedBy(inv));

        inv.clear();
        for (int i = 0; i < 10; i++) {
            assertFalse(t.isSatisfiedBy(inv));
            inv.add(new ItemStack(Material.PISTON, 1));
        }
        assertTrue(t.isSatisfiedBy(inv));
    }

    @Test
    void testVariables() {
        ItemTrigger t = makeTrigger("jm_ender_pearls");  // "$var" (15) Ender Pearls

        ArrayList<ItemStack> inv = new ArrayList<>();
        inv.add(new ItemStack(Material.ENDER_PEARL, 14));
        assertFalse(t.isSatisfiedBy(inv));
        inv.add(new ItemStack(Material.ENDER_PEARL, 1));
        assertTrue(t.isSatisfiedBy(inv));
    }

    @Test
    void testGroupsBasicWithVariable() {
        ItemTrigger t = makeTrigger("jm_different_fish");  // "$varFish" (3) Different Fish

        ArrayList<ItemStack> inv = new ArrayList<>();
        inv.add(new ItemStack(Material.COD, 10));
        inv.add(new ItemStack(Material.COOKED_COD, 10));
        inv.add(new ItemStack(Material.COD_BUCKET, 10));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.SALMON, 1));
        inv.add(new ItemStack(Material.COOKED_SALMON, 1));
        inv.add(new ItemStack(Material.SALMON_BUCKET, 1));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.TROPICAL_FISH_BUCKET, 1));
        assertTrue(t.isSatisfiedBy(inv));
    }

    @Test
    void testGroupsWithRootAndRegex() {
        ItemTrigger t = makeTrigger("jm_different_edible");  // 6 Different Edible Items

        ArrayList<ItemStack> inv = new ArrayList<>();
        inv.add(new ItemStack(Material.APPLE, 64));
        inv.add(new ItemStack(Material.BEETROOT, 64));
        inv.add(new ItemStack(Material.MUSHROOM_STEW, 1));
        inv.add(new ItemStack(Material.POTATO, 1));
        inv.add(new ItemStack(Material.BAKED_POTATO, 1));
        inv.add(new ItemStack(Material.BEEF, 1));
        inv.add(new ItemStack(Material.COOKED_BEEF, 1));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.RABBIT_STEW, 1));
        assertTrue(t.isSatisfiedBy(inv));
    }

    @Test
    void testGroupsTotalAndUnique() {
        ItemTrigger t = makeTrigger("jm_roses_dandelions");  // 10 Roses + 15 Dandelions

        ArrayList<ItemStack> inv = new ArrayList<>();
        inv.add(new ItemStack(Material.ROSE_BUSH, 9));
        inv.add(new ItemStack(Material.DANDELION, 14));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.DANDELION, 64));
        assertFalse(t.isSatisfiedBy(inv));

        inv.add(new ItemStack(Material.ROSE_BUSH, 1));
        assertTrue(t.isSatisfiedBy(inv));
    }

    private @NotNull ItemTrigger makeTrigger(@NotNull String goalId) {
        return ItemTrigger.createTriggers(goalId, testVariables, yaml).stream().findFirst().orElseThrow();
    }
}
