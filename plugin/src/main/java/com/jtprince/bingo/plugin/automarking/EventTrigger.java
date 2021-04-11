package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.Space;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
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
    private final Method method;
    final Class<? extends Event> eventType;
    private final EventTriggerBukkitListener bukkitListener;

    private EventTrigger(Space space, Method method, Class<? extends Event> eventType) {
        super(space);
        this.method = method;
        this.eventType = eventType;
        this.bukkitListener = MCBingoPlugin.instance().autoMarkListener;
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

    public static Set<String> allAutomatedGoals() {
        Set<String> ret = new HashSet<>();
        for (Method method : EventTrigger.class.getDeclaredMethods()) {
            GoalEventTriggerListener anno = method.getAnnotation(GoalEventTriggerListener.class);
            if (anno == null) {
                continue;
            }
            Collections.addAll(ret, anno.extraGoals());
            ret.add(method.getName());
        }
        return ret;
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface GoalEventTriggerListener {
        String[] extraGoals() default {};
    }

    /* Listeners */

    @GoalEventTriggerListener
    private boolean jm_never_sword(BlockBreakEvent event) {
        // Never use a sword
        // See also EntityDamageByEntityEvent variant
        return event.getPlayer().getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_sword");
    }

    @GoalEventTriggerListener
    private boolean jm_never_axe(BlockBreakEvent event) {
        // Never use an axe
        // See also EntityDamageByEntityEvent variant
        return event.getPlayer().getInventory().getItemInMainHand()
            .getType().getKey().toString().contains("_axe");
    }

    @GoalEventTriggerListener
    private boolean jm_destroy_spawner(BlockBreakEvent event) {
        // Destroy a monster spawner
        return event.getBlock().getType() == Material.SPAWNER;
    }

    @GoalEventTriggerListener
    private boolean jm_fire_village(BlockIgniteEvent event) {
        // Set fire to a Villager's House
        // This is a hard one to make specific...
        return (event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
                && ActivationHelpers.inVillage(event.getBlock().getLocation()));
    }

    @GoalEventTriggerListener
    private boolean jm_ice_magma(BlockPlaceEvent event) {
        // Ice Block on top of a Magma Block
        Material placed = event.getBlock().getType();
        if (placed == Material.MAGMA_BLOCK) {
            return TriggerDefinition.ICE_BLOCKS.contains(event.getBlock().getRelative(BlockFace.UP).getType());
        } else if (TriggerDefinition.ICE_BLOCKS.contains(placed)) {
            return event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.MAGMA_BLOCK;
        } else {
            return false;
        }
    }

    @GoalEventTriggerListener
    private boolean jm_never_rches51018(BlockPlaceEvent event) {
        // Never place torches
        // Not currently in goals.yml
        return TriggerDefinition.TORCHES.contains(event.getBlock().getType());
    }

    @GoalEventTriggerListener
    private boolean jm_never_ticks40530(CraftItemEvent event) {
        // Never craft sticks
        // Not currently in goals.yml
        ItemStack result = event.getInventory().getResult();
        return (result != null && result.getType() == Material.STICK);
    }

    @GoalEventTriggerListener
    private boolean jm_never_coal(CraftItemEvent event) {
        // Never use coal
        // See also FurnaceBurnEvent variant
        //noinspection ConstantConditions - Matrix array members can be null if no ingredient in that slot
        return Arrays.stream(event.getInventory().getMatrix())
            .anyMatch(i -> i != null && i.getType() == Material.COAL);
    }

    @GoalEventTriggerListener
    private boolean jm_build_golem_iron(CreatureSpawnEvent event) {
        // Create an Iron Golem
        return event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM;
    }

    @GoalEventTriggerListener
    private boolean jm_build_golem_snow(CreatureSpawnEvent event) {
        // Create a Snow Golem
        return event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN;
    }

    @GoalEventTriggerListener
    private boolean jm_nether_fish(CreatureSpawnEvent event) {
        // Get a fish into the nether
        // Going through a portal still fires this event, so no need for other EventTriggers.
        return (event.getLocation().getWorld().getEnvironment() == World.Environment.NETHER
            && TriggerDefinition.FISH_ENTITIES.contains(event.getEntityType()));
    }

    @GoalEventTriggerListener
    private boolean jm_enchant_any(EnchantItemEvent event) {
        // Enchant an item
        return true;
    }

    @GoalEventTriggerListener
    private boolean jtp_effect_harming(EntityDamageByEntityEvent event) {
        // Be afflicted by Harming
        if (!(event.getDamager() instanceof ThrownPotion)) {
            return false;
        }
        return ((ThrownPotion)event.getDamager()).getEffects().stream()
            .anyMatch(e -> e.getType().equals(PotionEffectType.HARM));
    }

    @GoalEventTriggerListener
    private boolean jm_never_sword(EntityDamageByEntityEvent event) {
        // Never use a sword
        // See also BlockBreakEvent variant
        return (event.getDamager() instanceof Player
                && ((Player) event.getDamager()).getInventory().getItemInMainHand()
                   .getType().getKey().toString().contains("_sword"));
    }

    @GoalEventTriggerListener
    private boolean jm_never_axe(EntityDamageByEntityEvent event) {
        // Never use an axe
        // See also BlockBreakEvent variant
        return (event.getDamager() instanceof Player
                && ((Player) event.getDamager()).getInventory().getItemInMainHand()
                   .getType().getKey().toString().contains("_axe"));
    }

    @GoalEventTriggerListener
    private boolean jm_get_skeleton_bow(EntityDeathEvent event) {
        // Get a Skeleton's Bow
        // This callback ONLY adds item lore to the dropped bow. See EntityPickupItemEvent
        //  variant for the part that actually activates the goal.
        if (event.getEntity().getType() == EntityType.SKELETON) {
            for (ItemStack i : event.getDrops()) {
                if (i.getType() == Material.BOW) {
                    List<Component> currentLore = i.lore();
                    if (currentLore == null) {
                        currentLore = new ArrayList<>();
                    }
                    currentLore.add(TriggerDefinition.SKELETON_DROPPED_BOW);
                    i.lore(currentLore);
                }
            }
        }
        return false;
    }

    @GoalEventTriggerListener
    private boolean jm_kill_golem_iron(EntityDeathEvent event) {
        // Kill an Iron Golem
        return (event.getEntityType() == EntityType.IRON_GOLEM
                && event.getEntity().getKiller() != null);
    }

    @GoalEventTriggerListener
    private boolean jm_kill_mob_anvil(EntityDeathEvent event) {
        // Kill a hostile mob with an Anvil
        if (!(event.getEntity() instanceof Monster)) {
            return false;
        }

        EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();
        if (!(damageEvent instanceof EntityDamageByEntityEvent)) {
            return false;
        }

        EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) damageEvent;
        Entity damager = damageByEntityEvent.getDamager();
        if (!(damager instanceof FallingBlock)) {
            return false;
        }

        return ((FallingBlock)damager).getBlockData()
            .getMaterial().getKey().toString().contains("anvil");
    }

    @GoalEventTriggerListener
    private boolean jm_tnt_minecart_detonate(EntityExplodeEvent event) {
        // Detonate a TNT minecart
        return event.getEntity().getType() == EntityType.MINECART_TNT;
    }

    @GoalEventTriggerListener
    private boolean jm_2_creepers_boat(EntityMountEvent event) {
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
    private boolean jm_never_boat(EntityMountEvent event) {
        // Never use (enter) boats
        return event.getEntity() instanceof Player
            && event.getMount() instanceof Boat;
    }

    @GoalEventTriggerListener
    private boolean jm_get_skeleton_bow(EntityPickupItemEvent event) {
        // Get a Skeleton's Bow
        // This callback relies on the EntityDeathEvent variant, which adds a custom lore to any
        //  bows dropped by a Skeleton.
        @Nullable List<Component> lore = event.getItem().getItemStack().lore();
        return (lore != null && lore.contains(TriggerDefinition.SKELETON_DROPPED_BOW));
    }

    private static final Map<String, PotionEffectType> goalPotionEffectMap = Map.of(
        "jtp_effect_slowness", PotionEffectType.SLOW,
        "jtp_effect_poison", PotionEffectType.POISON,
        "jtp_effect_weakness", PotionEffectType.WEAKNESS,
        "jtp_effect_mfatigue", PotionEffectType.SLOW_DIGGING,
        "jtp_effect_fire_res", PotionEffectType.FIRE_RESISTANCE,
        "jtp_effect_absorption", PotionEffectType.ABSORPTION
        /* WARNING: When adding a potion effect, also add it to the extraGoals below */
    );

    @GoalEventTriggerListener(extraGoals = {"jtp_effect_poison", "jtp_effect_weakness",
        "jtp_effect_mfatigue", "jtp_effect_fire_res", "jtp_effect_absorption"})
    private boolean jtp_effect_slowness(EntityPotionEffectEvent event) {
        // Be afflicted by <Potion Effect>
        // Instant effects (harming, instant health) must listen to EntityDamageByEntityEvent
        PotionEffectType effectType = goalPotionEffectMap.get(this.getSpace().goalId);
        if (effectType == null) {
            MCBingoPlugin.logger().severe("Potion effect not defined for goal "
                + this.getSpace().goalId);
            return false;
        }
        return event.getModifiedType().equals(effectType);
    }

    @GoalEventTriggerListener
    private boolean jm_tame_horse(EntityTameEvent event) {
        // Tame a horse
        return event.getEntity().getType() == EntityType.HORSE;
    }

    @GoalEventTriggerListener
    private boolean jm_tame_wolf(EntityTameEvent event) {
        // Tame a wolf
        return event.getEntity().getType() == EntityType.WOLF;
    }

    @GoalEventTriggerListener
    private boolean jm_tame_parrot(EntityTameEvent event) {
        // Tame a parrot
        // Not currently in goals.yml
        return event.getEntity().getType() == EntityType.PARROT;
    }

    @GoalEventTriggerListener
    private boolean jm_tame_ocelot(EntityTameEvent event) {
        // Tame an ocelot
         // Not currently in goals.yml
       return event.getEntity().getType() == EntityType.OCELOT;
    }

    @GoalEventTriggerListener
    private boolean jm_tame_donkey(EntityTameEvent event) {
        // Tame a donkey
        // Not currently in goals.yml
        return event.getEntity().getType() == EntityType.DONKEY;
    }

    @GoalEventTriggerListener
    private boolean jtp_tame_cat(EntityTameEvent event) {
        // Tame a cat
        return event.getEntity().getType() == EntityType.CAT;
    }

    @GoalEventTriggerListener
    private boolean jm_never_coal(FurnaceBurnEvent event) {
        // Never use coal
        // See also CraftItemEvent variant
        return event.getFuel().getType() == Material.COAL
            || event.getFuel().getType() == Material.COAL_BLOCK;
    }

    @GoalEventTriggerListener
    private boolean jm_armor_leather_colors(InventoryCloseEvent event) {
        // Wear 4 different color Leather Armor at the same time
        Player p = (Player) event.getPlayer();
        Set<Color> armorColorsFound = new HashSet<>();

        //noinspection NullableProblems - Array members can be null if no armor in that slot
        for (@Nullable ItemStack item : p.getInventory().getArmorContents()) {
            if (item != null && TriggerDefinition.LEATHER_ARMOR.contains(item.getType())) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof LeatherArmorMeta) {
                    armorColorsFound.add(((LeatherArmorMeta)meta).getColor());
                }
            }
        }
        return armorColorsFound.size() >= 4;
    }

    @GoalEventTriggerListener
    private boolean jm_carpet_llama(InventoryCloseEvent event) {
        // Put a Carpet on a Llama
        return (event.getInventory().getHolder() instanceof Llama
                && Arrays.stream(event.getInventory().getContents()).filter(Objects::nonNull)
                   .anyMatch(i -> TriggerDefinition.CARPETS.contains(i.getType())));
    }

    @GoalEventTriggerListener
    private boolean jm_fill_hopper(InventoryCloseEvent event) {
        // Fill a Hopper with 320 items
        return (event.getInventory().getType() == InventoryType.HOPPER
                && ActivationHelpers.inventoryContainsQuantity(event.getInventory(), 320));
    }

    @GoalEventTriggerListener
    private boolean jm_never_armor_any(InventoryCloseEvent event) {
        // Never use armor
        Player p = (Player) event.getPlayer();
        //noinspection ConstantConditions - Array members can be null if no armor in that slot
        return Arrays.stream(p.getInventory().getArmorContents()).anyMatch(Objects::nonNull);
    }

    @GoalEventTriggerListener
    private boolean jm_never_chestplates(InventoryCloseEvent event) {
        // Never wear chestplates
        Player p = (Player) event.getPlayer();
        //noinspection ConstantConditions - Array members can be null if no armor in that slot
        return p.getInventory().getArmorContents()[2] != null;
    }

    @GoalEventTriggerListener
    private boolean jm_never_sleep(PlayerBedLeaveEvent event) {
        // Never Sleep
        return event.getPlayer().getWorld().getTime() < 1000;
    }

    @GoalEventTriggerListener
    private boolean jm_sleep_village(PlayerBedLeaveEvent event) {
        // Sleep in a village
        return jm_never_sleep(event)
            && ActivationHelpers.inVillage(event.getPlayer().getLocation());
    }

    @GoalEventTriggerListener
    private boolean jm_never_die(PlayerDeathEvent event) {
        // Never die
        return true;
    }

    @GoalEventTriggerListener
    private boolean jm_death_msg_escape(PlayerDeathEvent event) {
        // TODO: Figure out how to get a death message string from Adventure.
        @SuppressWarnings("deprecation")
        String msg = event.getDeathMessage();
        return msg != null && msg.contains("trying to escape");
    }

    @GoalEventTriggerListener
    private boolean jm_fish_junk(PlayerFishEvent event) {
        // Fish a Junk Item
        Entity caught = event.getCaught();
        if (!(caught instanceof Item)) {
            return false;
        }
        Item item = (Item) caught;
        Material material = item.getItemStack().getType();

        /* Special case - Fishing Rods are treasure when enchanted, junk when not */
        if (material == Material.FISHING_ROD) {
            return item.getItemStack().getEnchantments().size() == 0;
        }

        return TriggerDefinition.FISHING_JUNK.contains(material);
    }

    @GoalEventTriggerListener
    private boolean jm_fish_treasure(PlayerFishEvent event) {
        // Fish a Treasure Item
        Entity caught = event.getCaught();
        if (!(caught instanceof Item)) {
            return false;
        }
        Item item = (Item) caught;
        Material material = item.getItemStack().getType();

        /* Special case - Fishing Rods are treasure when enchanted, junk when not */
        if (material == Material.FISHING_ROD) {
            return item.getItemStack().getEnchantments().size() > 0;
        }

        return TriggerDefinition.FISHING_TREASURES.contains(material);
    }

    @GoalEventTriggerListener
    private boolean jm_lead_rabbit(PlayerInteractEntityEvent event) {
        // Use a lead on a rabbit
        ItemStack hand = event.getPlayer().getInventory().getItem(event.getHand());
        return event.getRightClicked().getType() == EntityType.RABBIT
            && hand != null && hand.getType() == Material.LEAD;
    }

    @GoalEventTriggerListener
    private boolean jm_sleep_nether(PlayerInteractEvent event) {
        // Nether bed
        return event.getClickedBlock() != null
            && event.getClickedBlock().getWorld().getEnvironment() == World.Environment.NETHER
            && event.getClickedBlock().getType().getKey().toString().contains("_bed")
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @GoalEventTriggerListener
    private boolean jm_map_marker(PlayerInteractEvent event) {
        // Add a Marker to a Map (by right clicking a banner with the map)
        return event.getItem() != null
            && event.getItem().getType() == Material.FILLED_MAP
            && event.getClickedBlock() != null
            && event.getClickedBlock().getType().getKey().toString().contains("_banner")
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @GoalEventTriggerListener
    private boolean jm_cauldron_water(PlayerInteractEvent event) {
        // Cauldron with water (put water in a cauldron)
        return event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.CAULDRON
            && event.getItem() != null
            && event.getItem().getType() == Material.WATER_BUCKET
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @GoalEventTriggerListener
    private boolean jm_flower_pot_cactus(PlayerInteractEvent event) {
        // Place a cactus in a flower pot
        return event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.FLOWER_POT
            && event.getItem() != null
            && event.getItem().getType() == Material.CACTUS
            && event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    @GoalEventTriggerListener
    private boolean jm_never_fish(PlayerInteractEvent event) {
        // Never use a fishing rod
        return event.getItem() != null
            && event.getItem().getType() == Material.FISHING_ROD
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @GoalEventTriggerListener
    private boolean jm_never_shield(PlayerInteractEvent event) {
        // Never use a shield
        return event.getItem() != null
            && event.getItem().getType() == Material.SHIELD
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @GoalEventTriggerListener
    private boolean jm_never_buckets(PlayerInteractEvent event) {
        // Never use buckets
        return event.getItem() != null
            && event.getItem().getType().getKey().toString().contains("bucket")
            && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
    }

    @GoalEventTriggerListener
    private boolean jm_deplete_sword_iron(PlayerItemBreakEvent event) {
        // Deplete an Iron Sword
        return event.getBrokenItem().getType() == Material.IRON_SWORD;
    }

    @GoalEventTriggerListener
    private boolean jm_carnivore(PlayerItemConsumeEvent event) {
        // Only eat meat (i.e. trigger if NOT meat)
        return TriggerDefinition.NON_MEATS.contains(event.getItem().getType());
    }

    @GoalEventTriggerListener
    private boolean jm_vegetarian(PlayerItemConsumeEvent event) {
        // Never eat meat (i.e. trigger if meat)
        return TriggerDefinition.MEATS.contains(event.getItem().getType());
    }

    @GoalEventTriggerListener
    private boolean jm_level(PlayerLevelChangeEvent event) {
        // Level <x>
        int requiredLevel = this.getSpace().variables.get("var");
        return event.getNewLevel() >= requiredLevel;
    }

    @GoalEventTriggerListener
    private boolean jm_trade_any(PlayerTradeEvent event) {
        // Trade a villager
        return event.getVillager() instanceof Villager;
    }

    @GoalEventTriggerListener
    private boolean jm_nether_portal_village(PortalCreateEvent event) {
        // Portal in village
        return event.getEntity() instanceof Player
            && ActivationHelpers.inVillage(event.getBlocks().get(0).getLocation());
    }

    @GoalEventTriggerListener
    private boolean jm_nether_portal_size(PortalCreateEvent event) {
        // Activate a $var1x$var2 Nether Portal (not counting corners)
        int width = this.getSpace().variables.get("var1");
        int height = this.getSpace().variables.get("var2");
        long portalBlocksActivated =
            event.getBlocks().stream().filter(b -> b.getType()==Material.NETHER_PORTAL).count();
        /* Just counting the total blocks is a little bit of a cheat vs actually measuring the
         * width and height, but it's way simpler. */
        return portalBlocksActivated >= ((long) width * height);
    }

    @GoalEventTriggerListener
    private boolean jm_tree_nether(StructureGrowEvent event) {
        // Grow a tree in the nether
        return event.getWorld().getEnvironment() == World.Environment.NETHER
            && TriggerDefinition.TREES.contains(event.getSpecies());
    }

    @GoalEventTriggerListener
    private boolean jm_grow_mushroom(StructureGrowEvent event) {
        // Grow a huge mushroom
        return TriggerDefinition.MUSHROOMS.contains(event.getSpecies());
    }
}
