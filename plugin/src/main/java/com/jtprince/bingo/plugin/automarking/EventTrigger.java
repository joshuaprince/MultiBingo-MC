package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.Space;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
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
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.spigotmc.event.entity.EntityMountEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * Container class that maps a Space on a board to a Method that can check if that space should be
 * activated when a Bukkit Event is fired.
 *
 * Event Trigger methods for individual goals are at the bottom of this file.
 */
class EventTrigger extends AutoMarkTrigger {
    final Space space;
    private final Method method;
    final Class<? extends Event> eventType;
    private final EventTriggerBukkitListener bukkitListener;

    private EventTrigger(Space space, Method method, Class<? extends Event> eventType) {
        this.space = space;
        this.method = method;
        this.eventType = eventType;
        this.bukkitListener = space.board.game.plugin.autoMarkListener;
    }

    @SuppressWarnings("unchecked")  // Reflection + generics is lots of fun...
    static ArrayList<EventTrigger> createTriggers(Space space) {
        ArrayList<EventTrigger> ret = new ArrayList<>();

        // Register goals with Method triggers
        for (Method method : EventTrigger.class.getDeclaredMethods()) {
            GoalEventTriggerListener anno = method.getAnnotation(GoalEventTriggerListener.class);
            if (anno == null) {
                continue;
            }

            // TODO Sanity check each method - return type, params, static, etc
            // TODO Move that sanity check to an onEnable callback, rather than log spam 25x on
            //   every board receive

            // Determine which Event to listen for and register this Space in a new EventTrigger.
            Class<?> expectedType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(expectedType)) {
                MCBingoPlugin.logger().severe(
                    "Parameter in Listener method " + method.getName() + " is not an Event.");
                continue;
            }
            if (!EventTriggerBukkitListener.listenerExists((Class<? extends Event>) expectedType)) {
                MCBingoPlugin.logger().severe(
                    "Event trigger method " + method.getName()
                        + " does not have a corresponding Event Listener.");
                continue;
            }

            // Find all goals that this method can track
            Set<String> goalsTrackedByMethod = new HashSet<>();
            goalsTrackedByMethod.add(method.getName());
            goalsTrackedByMethod.addAll(Arrays.asList(anno.extraGoals()));
            if (!goalsTrackedByMethod.contains(space.goalId)) {
                continue;
            }

            Class<? extends Event> expectedEventType = (Class<? extends Event>) expectedType;
            EventTrigger et = new EventTrigger(space, method, expectedEventType);
            et.bukkitListener.register(et);
            ret.add(et);
        }

        return ret;
    }

    @Override
    public void destroy() {
        bukkitListener.unregister(this);
    }

    boolean satisfiedBy(Event event) {
        Object result = this.invoke(event);
        if (result == null) {
            return false;
        } else {
            return (boolean) result;
        }
    }

    private Object invoke(Event event) {
        try {
            return this.method.invoke(this, event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            MCBingoPlugin.logger().log(Level.SEVERE,
                "Failed to pass " + event.getClass().getName() + " to listeners", e);
            return null;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface GoalEventTriggerListener {
        String[] extraGoals() default {};
    }

    /* Definitions */

    private static final Material[] MEATS = { // Invalidate Vegetarian
        Material.CHICKEN, Material.COOKED_CHICKEN, Material.COD, Material.COOKED_COD,
        Material.BEEF, Material.COOKED_BEEF, Material.MUTTON, Material.COOKED_MUTTON,
        Material.RABBIT, Material.COOKED_RABBIT, Material.SALMON, Material.COOKED_SALMON,
        Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.TROPICAL_FISH, Material.PUFFERFISH,
        Material.RABBIT_STEW, Material.ROTTEN_FLESH, Material.SPIDER_EYE
    };

    private static final Material[] NON_MEATS = {  // Invalidate Carnivore
        Material.APPLE, Material.BAKED_POTATO, Material.BEETROOT, Material.BEETROOT_SOUP,
        Material.BREAD, Material.CARROT, Material.CHORUS_FRUIT, Material.COOKIE,
        Material.DRIED_KELP, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_APPLE,
        Material.GOLDEN_CARROT, Material.MELON_SLICE, Material.MUSHROOM_STEW,
        Material.POISONOUS_POTATO, Material.POTATO, Material.PUMPKIN_PIE, Material.SUSPICIOUS_STEW,
        Material.SWEET_BERRIES
    };

    /* Consumables that do not invalidate either of the above: Potions, Honey, Milk, Cake (block) */

    private static final Material[] TORCHES = {
        Material.TORCH, Material.WALL_TORCH, Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
        Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH
    };

    private static final TreeType[] TREES = {
        TreeType.ACACIA, TreeType.BIG_TREE, TreeType.BIRCH, TreeType.COCOA_TREE,
        TreeType.DARK_OAK, TreeType.JUNGLE, TreeType.JUNGLE_BUSH, TreeType.MEGA_REDWOOD,
        TreeType.REDWOOD, TreeType.SMALL_JUNGLE, TreeType.SWAMP, TreeType.TALL_BIRCH,
        TreeType.TALL_REDWOOD, TreeType.TREE
    };

    private static final TreeType[] MUSHROOMS = {
        TreeType.BROWN_MUSHROOM, TreeType.RED_MUSHROOM
    };

    private static final EntityType[] FISH_ENTITIES = {
        EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH
    };

    /* Listeners */

    @GoalEventTriggerListener
    private boolean jm_never_sword96977(BlockBreakEvent event) {
        // Never use a sword
        // See also EntityDamageByEntityEvent variant
        return event.getPlayer().getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_sword");
    }

    @GoalEventTriggerListener
    private boolean jm_never_n_axe38071(BlockBreakEvent event) {
        // Never use an axe
        // See also EntityDamageByEntityEvent variant
        return event.getPlayer().getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_axe");
    }

    @GoalEventTriggerListener
    private boolean jm_destr_awner87999(BlockBreakEvent event) {
        // Destroy a monster spawner
        return event.getBlock().getType() == Material.SPAWNER;
    }

    @GoalEventTriggerListener
    private boolean jm_never_rches51018(BlockPlaceEvent event) {
        // Never place torches
        return Arrays.stream(TORCHES).anyMatch(m -> event.getBlock().getType() == m);
    }

    @GoalEventTriggerListener
    private boolean jm_never_ticks40530(CraftItemEvent event) {
        // Never craft sticks
        ItemStack result = event.getInventory().getResult();
        return (result != null && result.getType() == Material.STICK);
    }

    @GoalEventTriggerListener
    private boolean jm_never__coal44187(CraftItemEvent event) {
        // Never use coal
        // See also FurnaceBurnEvent variant
        //noinspection ConstantConditions - Matrix array members can be null if no ingredient in that slot
        return Arrays.stream(event.getInventory().getMatrix())
            .anyMatch(i -> i != null && i.getType() == Material.COAL);
    }

    @GoalEventTriggerListener
    private boolean jm_creat_golem39717(CreatureSpawnEvent event) {
        // Create a Snow Golem
        return event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN;
    }

    @GoalEventTriggerListener
    private boolean jm_creat_golem39114(CreatureSpawnEvent event) {
        // Create an Iron Golem
        return event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM;
    }

    @GoalEventTriggerListener
    private boolean jm_get_a_ether66387(EntityAirChangeEvent event) {
        // Get a fish into the nether
        // TODO Fix marking spam as long as the fish is alive
        return event.getEntity().getWorld().getEnvironment() == World.Environment.NETHER
            && Arrays.stream(FISH_ENTITIES).anyMatch(t -> event.getEntity().getType() == t);
    }

    @GoalEventTriggerListener
    private boolean jm_never_sword96977(EntityDamageByEntityEvent event) {
        // Never use a sword
        // See also BlockBreakEvent variant
        return ((Player) event.getDamager()).getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_sword");
    }

    @GoalEventTriggerListener
    private boolean jm_never_n_axe38071(EntityDamageByEntityEvent event) {
        // Never use an axe
        // See also BlockBreakEvent variant
        return ((Player) event.getDamager()).getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_axe");
    }

    @GoalEventTriggerListener
    private boolean jm_deton_ecart39576(EntityExplodeEvent event) {
        // Detonate a TNT minecart
        return event.getEntity().getType() == EntityType.MINECART_TNT;
    }

    @GoalEventTriggerListener
    private boolean jm_2_cre__boat97078(EntityMountEvent event) {
        // 2 Creepers in the same Boat
        if (!(event.getMount() instanceof Boat)) {
            return false;
        }
        if (!(event.getEntity() instanceof Creeper)) {
            return false;
        }
        Boat boat = (Boat) event.getMount();

        int creepers = 1;  // The creeper entering on this Event
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof Creeper) {
                creepers++;
            }
        }

        return creepers >= 2;
    }

    @GoalEventTriggerListener
    private boolean jm_never__boat85417(EntityMountEvent event) {
        // Never use (enter) boats
        return event.getEntity() instanceof Player
            && event.getMount() instanceof Boat;
    }

    @GoalEventTriggerListener
    private boolean jm_tame__horse50063(EntityTameEvent event) {
        // Tame a horse
        return event.getEntity().getType() == EntityType.HORSE;
    }

    @GoalEventTriggerListener
    private boolean jm_tame___wolf12580(EntityTameEvent event) {
        // Tame a wolf
        return event.getEntity().getType() == EntityType.WOLF;
    }

    @GoalEventTriggerListener
    private boolean jm_tame__arrot29264(EntityTameEvent event) {
        // Tame a parrot
        return event.getEntity().getType() == EntityType.PARROT;
    }

    @GoalEventTriggerListener
    private boolean jm_tame__celot19643(EntityTameEvent event) {
        // Tame an ocelot
        return event.getEntity().getType() == EntityType.OCELOT;
    }

    @GoalEventTriggerListener
    private boolean jm_tame__onkey63865(EntityTameEvent event) {
        // Tame a donkey
        return event.getEntity().getType() == EntityType.DONKEY;
    }

    @GoalEventTriggerListener
    private boolean jm_never__coal44187(FurnaceBurnEvent event) {
        // Never use coal
        // See also CraftItemEvent variant
        return event.getFuel().getType() == Material.COAL
            || event.getFuel().getType() == Material.COAL_BLOCK;
    }

    @GoalEventTriggerListener(extraGoals = {"jm_never_ields14785"})
    private boolean jm_never_rmour42273(InventoryCloseEvent event) {
        // Never use armor
        // Never use armor or shields
        Player p = (Player) event.getPlayer();
        //noinspection ConstantConditions - Array members can be null if no armor in that slot
        return Arrays.stream(p.getInventory().getArmorContents()).anyMatch(Objects::nonNull);
    }

    @GoalEventTriggerListener
    private boolean jm_never_lates77348(InventoryCloseEvent event) {
        // Never wear chestplates
        Player p = (Player) event.getPlayer();
        //noinspection ConstantConditions - Array members can be null if no armor in that slot
        return p.getInventory().getArmorContents()[2] != null;
    }

    @GoalEventTriggerListener(extraGoals = {"jm_never_sleep35022"})
    private boolean jm_sleep_a_bed24483(PlayerBedLeaveEvent event) {
        // Just sleep
        return event.getPlayer().getWorld().getTime() < 1000;
    }

    @GoalEventTriggerListener
    private boolean jm_sleep_llage18859(PlayerBedLeaveEvent event) {
        // Sleep in a village
        return jm_sleep_a_bed24483(event)
            && ActivationHelpers.inVillage(event.getPlayer().getLocation());
    }

    @SuppressWarnings("SameReturnValue")
    @GoalEventTriggerListener
    private boolean jm_never_die_37813(PlayerDeathEvent event) {
        // Never die
        return true;
    }

    @GoalEventTriggerListener
    private boolean jm_use_a_abbit23802(PlayerInteractEntityEvent event) {
        // Use a lead on a rabbit
        ItemStack hand = event.getPlayer().getInventory().getItem(event.getHand());
        return event.getRightClicked().getType() == EntityType.RABBIT
            && hand != null && hand.getType() == Material.LEAD;
    }

    @GoalEventTriggerListener
    private boolean jm_try__nether11982(PlayerInteractEvent event) {
        // Nether bed
        return event.getClickedBlock() != null
            && event.getClickedBlock().getWorld().getEnvironment() == World.Environment.NETHER
            && event.getClickedBlock().getType().getKey().toString().contains("_bed")
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @GoalEventTriggerListener
    private boolean jm_cauld_water24040(PlayerInteractEvent event) {
        // Cauldron with water (put water in a cauldron)
        return event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.CAULDRON
            && event.getItem() != null
            && event.getItem().getType() == Material.WATER_BUCKET
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @GoalEventTriggerListener
    private boolean jm_place_r_pot79183(PlayerInteractEvent event) {
        // Place a cactus in a flower pot
        return event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.FLOWER_POT
            && event.getItem() != null
            && event.getItem().getType() == Material.CACTUS
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @GoalEventTriggerListener
    private boolean jm_never_g_rod73476(PlayerInteractEvent event) {
        // Never use a fishing rod
        return event.getItem() != null
            && event.getItem().getType() == Material.FISHING_ROD
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @GoalEventTriggerListener(extraGoals = {"jm_never_ields14785"})
    private boolean jm_never_hield82710(PlayerInteractEvent event) {
        // Never use a shield
        return event.getItem() != null
            && event.getItem().getType() == Material.SHIELD
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @GoalEventTriggerListener
    private boolean jm_never_ckets96909(PlayerInteractEvent event) {
        // Never use buckets
        return event.getItem() != null
            && event.getItem().getType().getKey().toString().contains("bucket")
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @GoalEventTriggerListener
    private boolean jm_carnivore_30882(PlayerItemConsumeEvent event) {
        // Only eat meat (i.e. trigger if NOT meat)
        return Arrays.stream(NON_MEATS).anyMatch(f -> event.getItem().getType() == f);
    }

    @GoalEventTriggerListener
    private boolean jm_vegetarian_67077(PlayerItemConsumeEvent event) {
        // Never eat meat (i.e. trigger if meat)
        return Arrays.stream(MEATS).anyMatch(f -> event.getItem().getType() == f);
    }

    @GoalEventTriggerListener(extraGoals = {"jm_level__53191", "jm_level__62503",
                                        "jm_level__50255", "jm_level__27398"})
    private boolean jm_level__71448(PlayerLevelChangeEvent event) {
        // Level <x>
        int requiredLevel = this.space.variables.get("var");
        return event.getNewLevel() >= requiredLevel;
    }

    @GoalEventTriggerListener
    private boolean jm_trade_lager37854(PlayerTradeEvent event) {
        // Trade a villager
        return event.getVillager() instanceof Villager;
    }

    @GoalEventTriggerListener
    private boolean jm_activ_llage72436(PortalCreateEvent event) {
        // Portal in village
        return event.getEntity() instanceof Player
            && ActivationHelpers.inVillage(event.getBlocks().get(0).getLocation());
    }

    @GoalEventTriggerListener
    private boolean jm_grow__ether38694(StructureGrowEvent event) {
        // Grow a tree in the nether
        return event.getWorld().getEnvironment() == World.Environment.NETHER
            && Arrays.stream(TREES).anyMatch(t -> t == event.getSpecies());
    }

    @GoalEventTriggerListener
    private boolean jm_grow__hroom76894(StructureGrowEvent event) {
        // Grow a huge mushroom
        return Arrays.stream(MUSHROOMS).anyMatch(t -> t == event.getSpecies());
    }

    @GoalEventTriggerListener
    private boolean jm_grow___tree94140(StructureGrowEvent event) {
        // Grow a full jungle tree
        return event.getSpecies() == TreeType.JUNGLE;
    }
}
