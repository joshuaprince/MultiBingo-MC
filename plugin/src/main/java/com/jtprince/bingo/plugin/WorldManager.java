package com.jtprince.bingo.plugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;

public class WorldManager implements Listener {
    protected MCBingoPlugin plugin;
    protected Map<String, WorldSet> worldSetMap = new HashMap<>();

    public WorldManager(MCBingoPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean createWorlds(String worldCode) {
        WorldCreator wc_overworld = WorldCreator.name("world_bingo_" + worldCode + "_overworld");
        WorldCreator wc_nether = WorldCreator.name("world_bingo_" + worldCode + "_nether");
        WorldCreator wc_end = WorldCreator.name("world_bingo_" + worldCode + "_the_end");

        wc_overworld.copy(plugin.getServer().getWorlds().get(0));
        wc_nether.copy(plugin.getServer().getWorlds().get(1));
        wc_end.copy(plugin.getServer().getWorlds().get(2));

        wc_overworld.seed(worldCode.hashCode());
        wc_nether.seed(worldCode.hashCode());
        wc_end.seed(worldCode.hashCode());

        WorldSet ws = new WorldSet(wc_overworld.createWorld(), wc_nether.createWorld(), wc_end.createWorld());
        this.worldSetMap.put(worldCode, ws);
        return true;
    }

    public boolean putInWorld(Player p, String worldCode) {
        WorldSet ws = this.worldSetMap.get(worldCode);
        if (ws == null) {
            return false;
        }

        Location loc = ws.getWorld(Environment.NORMAL).getSpawnLocation();
        p.teleport(loc);
        return true;
    }

    public static class WorldSet {
        protected Map<Environment, World> map;

        public WorldSet(World overworld, World nether, World end) {
            this.map = new HashMap<>();
            this.map.put(Environment.NORMAL, overworld);
            this.map.put(Environment.NETHER, nether);
            this.map.put(Environment.THE_END, end);
        }

        public World getWorld(Environment env) {
            return map.get(env);
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        WorldSet ws = findWorldSet(event.getFrom().getWorld());
        if (ws == null) {
            String msg = "No WorldSet found for teleport from " + event.getFrom().getWorld();
            this.plugin.getServer().getConsoleSender().sendMessage(msg);
            return;
        }

        World targetWorld = null;
        switch (event.getCause()) {
            case NETHER_PORTAL:
                if (event.getFrom().getWorld().getEnvironment() == Environment.NETHER) {
                    targetWorld = ws.getWorld(Environment.NORMAL);

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
                } else {
                    targetWorld = ws.getWorld(Environment.NETHER);
                }
                break;
            case END_PORTAL:
                if (event.getFrom().getWorld().getEnvironment() == Environment.NORMAL) {
                    targetWorld = ws.getWorld(Environment.THE_END);
                } else {
                    targetWorld = ws.getWorld(Environment.NORMAL);
                }
                break;
        }

        if (targetWorld != null) {
            Location to = event.getTo();
            to.setWorld(targetWorld);
            event.setTo(to);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.isAnchorSpawn() || event.isBedSpawn()) {
            return;
        }

        WorldSet ws = findWorldSet(event.getPlayer().getWorld());
        event.setRespawnLocation(ws.getWorld(Environment.NORMAL).getSpawnLocation());
    }

    protected WorldSet findWorldSet(World world) {
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
