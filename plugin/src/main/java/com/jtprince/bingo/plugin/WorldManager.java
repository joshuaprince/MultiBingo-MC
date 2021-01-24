package com.jtprince.bingo.plugin;

import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;

public class WorldManager implements Listener {
    private final MCBingoPlugin plugin;
    private final Map<String, WorldSet> worldSetMap = new HashMap<>();

    private static final Environment[] ENVIRONMENTS = {
        Environment.NORMAL, Environment.NETHER, Environment.THE_END
    };

    public WorldManager(MCBingoPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    public WorldSet createWorlds(String worldCode, String seed) {
        worldCode = worldCode.replace(" ", "_");

        WorldCreator wc_overworld = WorldCreator.name("world_bingo_" + worldCode + "_overworld");
        WorldCreator wc_nether = WorldCreator.name("world_bingo_" + worldCode + "_nether");
        WorldCreator wc_end = WorldCreator.name("world_bingo_" + worldCode + "_the_end");

        wc_overworld.copy(plugin.getServer().getWorlds().get(0));
        wc_nether.copy(plugin.getServer().getWorlds().get(1));
        wc_end.copy(plugin.getServer().getWorlds().get(2));

        wc_overworld.seed(seed.hashCode());
        wc_nether.seed(seed.hashCode());
        wc_end.seed(seed.hashCode());

        WorldSet ws = new WorldSet(worldCode,
            wc_overworld.createWorld(), wc_nether.createWorld(), wc_end.createWorld());
        this.worldSetMap.put(worldCode, ws);
        return ws;
    }

    public void unloadWorlds(WorldSet ws) {
        MCBingoPlugin.logger().info("Unloading WorldSet " + ws.worldSetCode);
        for (Environment env : ENVIRONMENTS) {
            World world = ws.getWorld(env);

            // Move all players in this world to the spawn world
            for (Player p : world.getPlayers()) {
                p.teleport(WorldManager.getSpawnWorld().getSpawnLocation());
            }

            this.plugin.getServer().unloadWorld(world, true);
        }

        //noinspection SuspiciousMethodCalls
        this.worldSetMap.remove(ws);
    }

    public static class WorldSet {
        private final String worldSetCode;
        private final Map<Environment, World> map;

        private WorldSet(String worldSetCode, World overworld, World nether, World end) {
            this.worldSetCode = worldSetCode;
            this.map = new HashMap<>();
            this.map.put(Environment.NORMAL, overworld);
            this.map.put(Environment.NETHER, nether);
            this.map.put(Environment.THE_END, end);
        }

        public World getWorld(Environment env) {
            return map.get(env);
        }
    }

    /**
     * Get the world that players spawn into when first joining the server, independent of any
     * Bingo games.
     */
    public static World getSpawnWorld() {
        return Bukkit.getWorlds().get(0);
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        WorldSet ws = findWorldSet(event.getFrom().getWorld());
        if (ws == null) {
            String msg = "No WorldSet found for teleport from " + event.getFrom().getWorld();
            MCBingoPlugin.logger().fine(msg);
            return;
        }

        Environment from = event.getFrom().getWorld().getEnvironment();
        Environment to = event.getTo().getWorld().getEnvironment();

        if (from == Environment.NETHER) {
            /* Special handling for nether -> overworld:
             *
             * When going from bingo_overworld to bingo_nether, the coordinates we are
             * handed are pre-divided by 8. From is bingo_overworld, but To is the_nether.
             * Therefore we just have to update the To world, and we are done.
             *
             * But when we go from bingo_nether to bingo_overworld, the coordinates are not
             * scaled at all! The From dimension is bingo_nether, but the To dimension is
             * incorrectly still the_nether instead of the_overworld.
             *
             * It seems like the game's internal checks for which dimension to send a portal
             * to looks like: "Is To in the_nether ? If so, send to the_overworld. Else,
             * send to the_nether." That means that for any bingo_* world, we will be sent
             * to the_nether.
             *
             * My theory: coming from *any* nether to the_nether, the environment is not
             * changing, so the coordinates calculated and handed to us are not scaled.
             */
            event.getTo().setX(event.getTo().getX() * 8);
            event.getTo().setZ(event.getTo().getZ() * 8);
            event.setSearchRadius(128);
            to = Environment.NORMAL;
        }

        World targetWorld = ws.getWorld(to);
        if (targetWorld != null) {
            Location toLoc = event.getTo();
            toLoc.setWorld(targetWorld);
            event.setTo(toLoc);
        }
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        WorldSet ws = findWorldSet(event.getFrom().getWorld());
        if (ws == null) {
            String msg = "No WorldSet found for Entity teleport from " + event.getFrom().getWorld();
            this.plugin.getServer().getConsoleSender().sendMessage(msg);
            return;
        }

        Environment from = event.getFrom().getWorld().getEnvironment();
        Environment to = event.getTo() == null ? null : event.getTo().getWorld().getEnvironment();

        if (from == Environment.NETHER && to != null) {
            /* Special handling for nether -> overworld: see onPortal */
            event.getTo().setX(event.getTo().getX() * 8);
            event.getTo().setZ(event.getTo().getZ() * 8);
            event.setSearchRadius(128);
            to = Environment.NORMAL;
        }

        World targetWorld = ws.getWorld(to);
        if (targetWorld != null) {
            Location toLoc = event.getTo();
            toLoc.setWorld(targetWorld);
            event.setTo(toLoc);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.isAnchorSpawn() || event.isBedSpawn()) {
            if (!event.getRespawnLocation().getWorld().equals(WorldManager.getSpawnWorld())) {
                return;
            }
        }

        WorldSet ws = findWorldSet(event.getPlayer().getWorld());
        if (ws != null) {
            event.setRespawnLocation(ws.getWorld(Environment.NORMAL).getSpawnLocation());
        }
    }

    WorldSet findWorldSet(World world) {
        for (WorldSet ws : worldSetMap.values()) {
            for (Environment env : Environment.values()) {
                if (world.equals(ws.getWorld(env))) {
                    return ws;
                }
            }
        }

        return null;
    }
}
