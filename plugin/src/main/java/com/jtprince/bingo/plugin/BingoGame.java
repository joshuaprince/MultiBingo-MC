package com.jtprince.bingo.plugin;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BingoGame {
    protected final MCBingoPlugin plugin;
    protected final String gameCode;
    protected BingoWebSocketClient wsClient;

    protected final Map<Player, WorldManager.WorldSet> playerWorldSetMap;

    public BingoGame(MCBingoPlugin plugin, String gameCode) {
        this.plugin = plugin;
        this.gameCode = gameCode;
        this.playerWorldSetMap = new HashMap<>();
    }

    public void prepareWorldSets(Iterable<Player> players) {
        for (Player p : players) {
            WorldManager.WorldSet ws = this.plugin.worldManager.createWorlds(
                gameCode + "_" + p.getName(), gameCode);
            this.playerWorldSetMap.put(p, ws);
        }
    }

    public void start() {
        this.wipeAdvancements();
        this.teleportPlayersToWorlds();
    }

    protected void teleportPlayersToWorlds() {
        for (Player p : playerWorldSetMap.keySet()) {
            World overworld = this.playerWorldSetMap.get(p).getWorld(World.Environment.NORMAL);
            overworld.setTime(0);
            p.teleport(overworld.getSpawnLocation());
        }
    }

    protected void wipeAdvancements() {
        CommandSender sender = this.plugin.getServer().getConsoleSender();
        this.plugin.getServer().dispatchCommand(sender, "advancement revoke @a everything");
    }
}
