package com.jtprince.bingo.plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.URI;
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

    public void prepare(Iterable<Player> players) {
        this.prepareWorldSets(players);
        this.connectWebSocket();
        this.sendGameLinksToPlayers(players);
    }

    protected void prepareWorldSets(Iterable<Player> players) {
        for (Player p : players) {
            WorldManager.WorldSet ws = this.plugin.worldManager.createWorlds(
                gameCode + "_" + p.getName(), gameCode);
            this.playerWorldSetMap.put(p, ws);
        }
    }

    public void start() {
        this.wipeAdvancements();
        this.teleportPlayersToWorlds();
        if (this.wsClient != null) {
            this.wsClient.sendRevealBoard();
        }
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

    protected void connectWebSocket() {
        URI uri = plugin.getWebsocketUrl(this.gameCode);
        this.wsClient = new BingoWebSocketClient(this, uri);
        this.wsClient.connect();
        this.plugin.getServer().getLogger().info("Successfully connected to websocket for game " + this.gameCode);
    }

    protected void sendGameLinksToPlayers(Iterable<Player> players) {
        for (Player p : players) {
            TextComponent t = new TextComponent("Test!");
            t.setText("Click here to open the board!");
            t.setColor(ChatColor.AQUA);
            URI url = this.plugin.getGameUrl(this.gameCode, p);
            t.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url.toString()));
            p.sendMessage(t);
        }
    }
}
