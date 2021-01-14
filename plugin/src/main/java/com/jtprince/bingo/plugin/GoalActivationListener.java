package com.jtprince.bingo.plugin;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class GoalActivationListener implements Listener {
    protected final AutoActivation autoActivation;

    private static class MethodTrigger {
        ConcreteGoal goal;
        Method method;
    }

    /* Maps i.e. StructureGrowEvent -> [jm_mushroom_grow(), jm_tree_grow()] */
    private Map<Class<? extends Event>, List<MethodTrigger>> activeEventListenerMap = new HashMap<>();

    public GoalActivationListener(AutoActivation aa) {
        this.autoActivation = aa;
        this.autoActivation.game.plugin.getServer().getPluginManager().registerEvents(this,
            this.autoActivation.game.plugin);
    }

    /**
     * Given a list of Goals on the board, create listeners that will respond when the player does
     * something in game that would activate each of those goals.
     * @param goals List of ConcreteGoals on the board.
     * @return A Set of which ConcreteGoals will be auto-activated.
     */
    @SuppressWarnings("unchecked")  // Reflection + generics is lots of fun...
    public Set<ConcreteGoal> registerGoals(Collection<ConcreteGoal> goals) {
        Map<Class<? extends Event>, List<MethodTrigger>> newEventListenerMap = new HashMap<>();
        Set<ConcreteGoal> listenedGoals = new HashSet<>();

        // Register all goals with ItemTriggers
        for (ConcreteGoal cg : goals) {
            if (cg.itemTriggers.size() > 0) {
                listenedGoals.add(cg);
            }
        }

        // Register goals with Method triggers
        for (Method method : GoalListener.class.getDeclaredMethods()) {
            GoalListener.BingoListener anno = method.getAnnotation(GoalListener.BingoListener.class);
            if (anno == null) {
                continue;
            }

            // TODO Sanity check each method - return type, params, static, etc

            // Find all goals that this method can track
            Set<String> goalsTrackedByMethod = new HashSet<>();
            goalsTrackedByMethod.add(method.getName());
            goalsTrackedByMethod.addAll(Arrays.asList(anno.extraGoals()));

            // Find any squares on the board that should be triggered by this method.
            for (ConcreteGoal cg : goals) {
                if (!goalsTrackedByMethod.contains(cg.id)) {
                    continue;
                }

                // The square represented by cg should be triggered by this method.
                listenedGoals.add(cg);

                // FIXME Move me outside of the inner for loop
                Class<?> expectedType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(expectedType)) {
                    this.autoActivation.game.plugin.getLogger().severe(
                        "Parameter in Listener method " + method.getName() + " is not an Event.");
                }

                Class<? extends Event> expectedEventType = (Class<? extends Event>) expectedType;

                if (!newEventListenerMap.containsKey(expectedEventType)) {
                    newEventListenerMap.put(expectedEventType, new ArrayList<>());
                }

                MethodTrigger gal = new MethodTrigger();
                gal.goal = cg;
                gal.method = method;
                newEventListenerMap.get(expectedEventType).add(gal);
            }
        }

        this.activeEventListenerMap = newEventListenerMap;
        return listenedGoals;
    }

    private void trigger(Event event, Player player) {
        List<MethodTrigger> methods = activeEventListenerMap.get(event.getClass());
        if (methods == null) {
            return;
        }

        for (MethodTrigger gal : methods) {
            boolean activate;
            try {
                activate = (boolean) gal.method.invoke(null, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                this.autoActivation.game.plugin.getLogger().log(Level.SEVERE,
                    "Failed to pass " + event.getClass().getName() + " to listeners", e);
                return;
            }

            if (activate) {
                gal.goal.impulse(player);
            }
        }
    }

    private void trigger(Event event, World world) {
        this.trigger(event, this.autoActivation.game.getPlayerInWorld(world));
    }

    /**
     * Return whether to ignore event associated with this player, because they are not part of
     * a game.
     */
    @Contract("null -> true")
    private boolean ignore(LivingEntity player) {
        if (!(player instanceof Player)) {
            return true;
        }

        Player p = (Player) player;
        boolean ret = !autoActivation.game.getPlayers().contains(p);
        if (ret) {
            this.autoActivation.game.plugin.getLogger().fine(
                "ActivationListener ignored player " + player.getName());
        }
        return ret;
    }

    /**
     * Return whether to ignore event in this world, because it is not part of a game.
     */
    private boolean ignore(World world) {
        if (world == null) {
            return true;
        }

        Player p = this.autoActivation.game.getPlayerInWorld(world);

        if (p == null) {
            this.autoActivation.game.plugin.getLogger().fine(
                "ActivationListener ignored world " + world.getName());
        }
        return (p == null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        Player p = (Player) event.getPlayer();
        this.autoActivation.impulseInventory(p);
        this.trigger(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (ignore(event.getEntity())) {
            return;
        }

        Player p = (Player) event.getEntity();
        // 1 tick later so the item is in the player's inventory
        this.autoActivation.game.plugin.getServer().getScheduler().scheduleSyncDelayedTask(
            this.autoActivation.game.plugin, () -> this.autoActivation.impulseInventory(p), 1);
        this.trigger(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerSleep(PlayerBedLeaveEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.trigger(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStructureGrow(StructureGrowEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.trigger(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPortalCreate(PortalCreateEvent event) {
        if (ignore(event.getWorld())) {
            return;
        }

        this.trigger(event, event.getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.trigger(event, event.getPlayer());
    }

    // No ignoreCancelled because Bukkit cancels air interact events for some reason...
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.trigger(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDirectDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player damager = (Player) event.getDamager();
        if (ignore(damager)) {
            return;
        }

        this.trigger(event, damager);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreakBlock(BlockBreakEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.trigger(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.trigger(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        this.trigger(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (ignore(event.getEntity())) {
            return;
        }

        this.trigger(event, event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (ignore(event.getWhoClicked())) {
            return;
        }

        Player p = (Player) event.getWhoClicked();

        this.trigger(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFurnaceStartBurning(FurnaceBurnEvent event) {
        if (ignore(event.getBlock().getWorld())) {
            return;
        }

        this.trigger(event, event.getBlock().getWorld());
    }
}
