package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.BingoGame;
import com.jtprince.bingo.plugin.automarking.itemtrigger.ItemTrigger;
import com.jtprince.bingo.plugin.player.BingoPlayer;
import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.player.PlayerBoard;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.*;

/**
 * Container for all Bukkit Event Listeners.
 *
 * Rather than each Goal having its own listener that hooks into Bukkit, we register all
 * Bukkit Event Listeners here. Individual goal triggers can register here to be called back
 * whenever that event occurs.
 */
public class EventTriggerBukkitListener implements Listener {
    final MCBingoPlugin plugin;

    /**
     * A map from Event *class objects* to a list of EventTriggers that are active in the current
     * game.
     *
     * For example, if a game has a "Never Use an Axe" space, this map would contain key
     * BlockBreakEvent, and value a list containing EventTrigger object with jm_never_n_axe38071
     * and the Space that is described "Never Use an Axe".
     */
    private final Map<Class<? extends Event>, Set<EventTrigger>> activeEventListenerMap = new HashMap<>();

    /**
     * A set of ItemTriggers that are currently being tracked. These will be impulsed whenever
     * a Bukkit event is raised that changes a player inventory.
     */
    private final Set<ItemTrigger> itemTriggers = new HashSet<>();

    public EventTriggerBukkitListener(MCBingoPlugin plugin) {
        this.plugin = plugin;
    }

    void register(EventTrigger eventTrigger) {
        if (!activeEventListenerMap.containsKey(eventTrigger.eventType)) {
            activeEventListenerMap.put(eventTrigger.eventType, new HashSet<>());
        }
        boolean added = activeEventListenerMap.get(eventTrigger.eventType).add(eventTrigger);
        if (!added) {
            plugin.getLogger().severe(
                "Registering " + eventTrigger.getSpace().goalId + " EventTrigger already registered");
        }
    }

    public void register(ItemTrigger itemTrigger) {
        boolean added = itemTriggers.add(itemTrigger);
        if (!added) {
            plugin.getLogger().severe(
                "Registering " + itemTrigger.getSpace().goalId + " ItemTrigger already registered");
        }
    }

    void unregister(EventTrigger eventTrigger) {
        boolean existed = activeEventListenerMap.get(eventTrigger.eventType).remove(eventTrigger);

        if (!existed) {
            plugin.getLogger().severe(
                "Unregistering " + eventTrigger.getSpace().goalId + " EventTrigger did not exist");
        }
    }

    public void unregister(ItemTrigger itemTrigger) {
        boolean existed = itemTriggers.remove(itemTrigger);

        if (!existed) {
            plugin.getLogger().severe(
                "Unregistering " + itemTrigger.getSpace().goalId + " ItemTrigger did not exist");
        }
    }

    /**
     * Return whether to ignore event associated with this player, because they are not part of
     * a game.
     */
    @Contract("null -> true")
    private boolean ignore(LivingEntity player) {
        if (plugin.getCurrentGame() == null
            || plugin.getCurrentGame().state != BingoGame.State.RUNNING) {
            return true;
        }

        if (!(player instanceof Player)) {
            return true;
        }

        BingoPlayer bp = plugin.getCurrentGame().playerManager.getBingoPlayer((Player) player);
        boolean ret = !plugin.getCurrentGame().playerManager.getLocalPlayers().contains(bp);
        if (ret) {
            MCBingoPlugin.logger().fine("ActivationListener ignored player " + player.getName());
        }
        return ret;
    }

    /**
     * Return whether to ignore event in this world, because it is not part of a game.
     */
    private boolean ignore(World world) {
        if (plugin.getCurrentGame() == null
            || plugin.getCurrentGame().state != BingoGame.State.RUNNING
            || world == null) {
            return true;
        }

        BingoPlayer p = plugin.getCurrentGame().playerManager.getBingoPlayer(world);

        if (p == null) {
            MCBingoPlugin.logger().finest("ActivationListener ignored world " + world.getName());
        }
        return (p == null);
    }


    /**
     * Scan an Event that has been fired by Bukkit, to check if any MethodTriggers on the board
     * should be triggered and cause a space to be marked.
     * @param event Event that was raised by Bukkit.
     * @param player The BingoPlayer whose board should be marked if this Event triggers any
     *               markings.
     */
    private void impulseEvent(Event event, BingoPlayer player) {
        Set<EventTrigger> methods = activeEventListenerMap.get(event.getClass());
        if (methods == null) {
            return;
        }

        for (EventTrigger gal : methods) {
            if (gal.satisfiedBy(event)) {
                this.plugin.getCurrentGame().playerManager.getPlayerBoard(player).autoMark(gal.getSpace());
            }
        }
    }

    /**
     * Scan an Event that has been fired by Bukkit, to check if any MethodTriggers on the board
     * should be triggered and cause a space to be marked.
     * @param event Event that was raised by Bukkit.
     * @param player The Bukkit Player that triggered this event, used to match to a BingoPlayer
     *               whose board should be marked.
     */
    private void impulseEvent(Event event, Player player) {
        this.impulseEvent(event, plugin.getCurrentGame().playerManager.getBingoPlayer(player));
    }

    /**
     * Scan an Event that has been fired by Bukkit, to check if any MethodTriggers on the board
     * should be triggered and cause a space to be marked.
     * @param event Event that was raised by Bukkit.
     * @param world The world this Event occurred in, used to match to a BingoPlayer whose board
     *              should be marked.
     */
    private void impulseEvent(Event event, World world) {
        this.impulseEvent(event, plugin.getCurrentGame().playerManager.getBingoPlayer(world));
    }

    /**
     * Scan the inventory of a given player, to check if any ItemTriggers on the board should
     * be triggered and cause a space to be marked.
     *
     * @param player The player whose inventory should be scanned.
     */
    private void impulseInventory(Player player) {
        BingoPlayer bp = plugin.getCurrentGame().playerManager.getBingoPlayer(player);
        if (bp == null) {
            return;
        }

        PlayerBoard pb = this.plugin.getCurrentGame().playerManager.getPlayerBoard(bp);

        // Collection of all items in all Bukkit Player inventory/ies
        Collection<@NotNull ItemStack> items = new ArrayList<>();
        for (Player p : bp.getBukkitPlayers()) {
            for (ItemStack itemStack : p.getInventory()) {
                if (itemStack != null) {
                    items.add(itemStack);
                }
            }
        }

        for (ItemTrigger trigger : itemTriggers) {
            if (trigger.isSatisfiedBy(items)) {
                pb.autoMark(trigger.getSpace());
            } else {
                pb.autoRevert(trigger.getSpace());
            }
        }
    }

    static boolean listenerExists(Class<? extends Event> listenerType) {
        return Arrays.stream(EventTriggerBukkitListener.class.getDeclaredMethods()).anyMatch(method ->
            method.getAnnotation(EventHandler.class) != null
                && method.getParameterTypes()[0].equals(listenerType)
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreakBlock(BlockBreakEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (ignore(event.getWhoClicked())) {
            return;
        }

        Player p = (Player) event.getWhoClicked();

        impulseEvent(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (ignore(event.getLocation().getWorld())) {
            return;
        }

        impulseEvent(event, event.getLocation().getWorld());
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

        impulseEvent(event, damager);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (ignore(event.getLocation().getWorld())) {
            return;
        }

        impulseEvent(event, event.getLocation().getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityMount(EntityMountEvent event) {
        if (ignore(event.getEntity().getWorld())) {
            return;
        }

        impulseEvent(event, event.getEntity().getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (ignore(event.getEntity())) {
            return;
        }

        Player p = (Player) event.getEntity();
        // 1 tick later so the item is in the player's inventory
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(
            plugin, () -> impulseInventory(p), 1);
        impulseEvent(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) {
            return;
        }

        Player owner = (Player) event.getOwner();
        if (ignore(owner)) {
            return;
        }

        impulseEvent(event, owner);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFurnaceStartBurning(FurnaceBurnEvent event) {
        if (ignore(event.getBlock().getWorld())) {
            return;
        }

        impulseEvent(event, event.getBlock().getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        Player p = (Player) event.getPlayer();
        impulseInventory(p);
        impulseEvent(event, p);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerSleep(PlayerBedLeaveEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (ignore(event.getEntity())) {
            return;
        }

        Player p = event.getEntity();
        impulseEvent(event, p);
        // 1 tick later so player's inventory is empty
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(
            plugin, () -> impulseInventory(p), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }

    // No ignoreCancelled because Bukkit cancels air interact events for some reason...
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLevelChange(PlayerLevelChangeEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTrade(PlayerTradeEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPortalCreate(PortalCreateEvent event) {
        if (ignore(event.getWorld())) {
            return;
        }

        impulseEvent(event, event.getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStructureGrow(StructureGrowEvent event) {
        if (ignore(event.getPlayer())) {
            return;
        }

        impulseEvent(event, event.getPlayer());
    }
}
