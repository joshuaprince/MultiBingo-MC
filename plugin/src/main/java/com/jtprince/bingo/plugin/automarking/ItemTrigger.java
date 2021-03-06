package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.Space;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Defines an item or set of items that the user can collect to automatically mark a Space as
 * completed.
 */
class ItemTrigger extends AutoMarkTrigger {
    Space space;
    private int neededMatchGroups = 1;
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
            this.neededMatchGroups = Integer.parseInt(needed);
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

    boolean isSatisfied(Inventory inv) {
        int matchesAllGroups = 0;

        for (ItemMatchGroup mg : matchGroups) {
            int matchesThisGroup = 0;

            /* Store a list of item names (i.e. minecraft:cobblestone) that we have matched already.
             * An ItemMatchGroup cannot match on the same Material more than once.
             */
            Set<String> matchedItemNames = new HashSet<>();

            for (ItemStack itemStack : inv.getContents()) {
                if (itemStack != null &&
                        matchesThisGroup < mg.maxMatches &&
                        mg.match(itemStack) &&
                        !matchedItemNames.contains(namespacedName(itemStack))) {
                    matchesAllGroups++;
                    matchesThisGroup++;
                    matchedItemNames.add(namespacedName(itemStack));
                }
            }
        }

        return (matchesAllGroups >= neededMatchGroups);
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

        boolean match(ItemStack itemStack) {
            if (itemStack.getAmount() < this.minQuantity) {
                return false;
            }

            for (Pattern nameMatch : this.nameMatches) {
                if (nameMatch.matcher(namespacedName(itemStack)).matches()) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Example: "minecraft:cobblestone"
     */
    private static String namespacedName(ItemStack itemStack) {
        return itemStack.getType().getKey().toString();
    }
}
