package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.Space;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Defines an item or set of items that the user can collect to automatically mark a Space as
 * completed.
 */
class ItemTrigger extends AutoMarkTrigger {
    Space space;
    private final int totalMatchesNeeded;
    private final ArrayList<ItemMatchGroup> matchGroups = new ArrayList<>();

    static Collection<ItemTrigger> createTriggers(Space space, JSONArray triggers) {
        ArrayList<ItemTrigger> ret = new ArrayList<>();
        if (triggers != null) {
            for (Object trigger : triggers) {
                JSONObject trig = (JSONObject) trigger;

                ItemTrigger it = new ItemTrigger(space, trig);
                MCBingoPlugin.instance().autoMarkListener.register(it);
                ret.add(it);
            }
        }
        return ret;
    }

    @Override
    public void destroy() {
        MCBingoPlugin.instance().autoMarkListener.unregister(this);
    }

    private ItemTrigger(Space space, JSONObject json) {
        this.space = space;
        JSONObject itemTriggerJson = (JSONObject) json.get("ItemTrigger");

        String needed = (String) itemTriggerJson.get("@needed");
        if (needed != null) {
            this.totalMatchesNeeded = Integer.parseInt(needed);
        } else {
            this.totalMatchesNeeded = 1;
        }

        JSONArray quantityTags = (JSONArray) itemTriggerJson.get("Quantity");
        int defaultQuantity;
        if (quantityTags != null && quantityTags.size() > 0) {
            defaultQuantity = Integer.parseInt((String) quantityTags.get(0));
        } else {
            defaultQuantity = 1;
        }

        JSONArray nameTags = (JSONArray) itemTriggerJson.get("Name");
        if (nameTags != null) {
            for (Object o : nameTags) {
                String name = (String) o;
                matchGroups.add(new ItemMatchGroup(name, defaultQuantity));
            }
        }

        JSONArray itemMatchGroups = (JSONArray) itemTriggerJson.get("ItemMatchGroup");
        if (itemMatchGroups != null) {
            for (Object o : itemMatchGroups) {
                JSONObject matchGroup = (JSONObject) o;
                matchGroups.add(new ItemMatchGroup(matchGroup));
            }
        }
    }

    /**
     * Determine whether the given inventory contains some items that can allow a goal to be
     * considered completed.
     */
    boolean isSatisfied(Inventory inv) {
        int totalMatches = 0;

        for (ItemMatchGroup mg : matchGroups) {
            /* Within a match group, each <Name> tag may count as one match towards totalMatches.
             * However, a match group may have a maxMatches that defines that this group can only
             * count towards totalMatches that many times.
             */

            /* Maps namespaced name -> number of matching items with that name in the inventory */
            Map<String, Integer> itemNameToCountMap = new HashMap<>();

            for (ItemStack itemStack : inv.getContents()) {
                int numItemsMatched = mg.match(itemStack);
                if (numItemsMatched > 0) {
                    String itemName = namespacedName(itemStack);
                    // Increments itemNameToCountMap[item name] by numItemsMatched
                    itemNameToCountMap.merge(itemName, numItemsMatched, Integer::sum);
                }
            }

            int matchesThisGroup = 0;
            for (String itemName : itemNameToCountMap.keySet()) {
                int quantity = itemNameToCountMap.get(itemName);
                if (quantity >= mg.minQuantity) {
                    totalMatches++;
                    matchesThisGroup++;
                    if (matchesThisGroup >= mg.maxMatches) {
                        break;
                    }
                }
            }
        }

        return (totalMatches >= totalMatchesNeeded);
    }

    private static class ItemMatchGroup {
        int maxMatches = Integer.MAX_VALUE;
        final ArrayList<Pattern> nameMatches = new ArrayList<>();
        int minQuantity = 1;

        ItemMatchGroup(JSONObject itemMatchGroup) {
            // ItemMatchGroup is made from an <ItemMatchGroup> tag
            String maxMatches = (String) itemMatchGroup.get("@max-matches");
            if (maxMatches != null) {
                this.maxMatches = Integer.parseInt(maxMatches);
            }

            JSONArray names = (JSONArray) itemMatchGroup.get("Name");
            if (names != null) {
                for (Object o : names) {
                    String name = (String) o;
                    nameMatches.add(Pattern.compile(name));
                }
            }

            JSONArray quantity = (JSONArray) itemMatchGroup.get("Quantity");
            if (quantity != null && quantity.size() > 0) {
                this.minQuantity = Integer.parseInt((String) quantity.get(0));
            }
        }

        ItemMatchGroup(String name, int minQuantity) {
            // ItemMatchGroup is made from a <Name> tag that is a direct child of an ItemTrigger
            this.minQuantity = minQuantity;
            this.nameMatches.add(Pattern.compile(name));
        }

        /**
         * Test whether a single Item Stack in an inventory satisfies this match group.
         * @return The number of items in this item stack that satisfy the match group.
         */
        int match(@Nullable ItemStack itemStack) {
            if (itemStack == null) {
                return 0;
            }

            for (Pattern nameMatch : this.nameMatches) {
                if (nameMatch.matcher(namespacedName(itemStack)).matches()) {
                    return itemStack.getAmount();
                }
            }

            return 0;
        }
    }

    /**
     * Example: "minecraft:cobblestone"
     */
    private static String namespacedName(@NotNull ItemStack itemStack) {
        return itemStack.getType().getKey().toString();
    }
}
