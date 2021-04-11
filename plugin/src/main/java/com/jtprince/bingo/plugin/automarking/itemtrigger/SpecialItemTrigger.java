package com.jtprince.bingo.plugin.automarking.itemtrigger;

import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.Space;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Extension of ItemTrigger for any items that cannot be specified in YAML - enchantments,
 * potion effects, etc.
 */
public class SpecialItemTrigger extends ItemTrigger {
    private final Method method;

    private SpecialItemTrigger(Space space, Method method) {
        super(space, null, space.variables);
        this.method = method;
    }

    public static ArrayList<ItemTrigger> createTriggers(@NotNull Space space) {
        ArrayList<ItemTrigger> ret = new ArrayList<>();

        // Register goals with Method triggers
        for (Method method : SpecialItemTrigger.class.getDeclaredMethods()) {
            SpecialItemTriggerDef anno = method.getAnnotation(SpecialItemTriggerDef.class);
            if (anno == null) {
                continue;
            }

            // TODO Sanity check each method - return type, params, static, etc
            // TODO Move that sanity check to an onEnable callback, rather than log spam 25x on
            //   every board receive

            if (!space.goalId.equals(method.getName())) {
                continue;
            }

            ret.add(new SpecialItemTrigger(space, method));
        }

        return ret;
    }

    /**
     * Determine whether the given inventory contains some items that can allow a goal to be
     * considered completed.
     */
    @Override
    public boolean isSatisfiedBy(Collection<@NotNull ItemStack> inventory) {
        try {
            Object result = this.method.invoke(this, inventory);
            return (boolean) result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            MCBingoPlugin.logger().log(Level.SEVERE,
                "Failed to check SpecialItemTrigger " + this.method.getName(), e);
            return false;
        }
    }

    public static Set<String> allAutomatedGoals() {
        Set<String> ret = new HashSet<>();
        for (Method method : SpecialItemTrigger.class.getDeclaredMethods()) {
            SpecialItemTriggerDef anno = method.getAnnotation(SpecialItemTriggerDef.class);
            if (anno == null) {
                continue;
            }
            ret.add(method.getName());
        }
        return ret;
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SpecialItemTriggerDef { }

    @SpecialItemTriggerDef
    private boolean jm_enchanted_gold_sword(Collection<@NotNull ItemStack> inventory) {
        for (ItemStack i : inventory) {
            if (i.getType() == Material.GOLDEN_SWORD
                && !i.getEnchantments().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
