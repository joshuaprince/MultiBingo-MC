package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.automarking.AutoMarking;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;

public class BingoGame {
    public final MCBingoPlugin plugin;
    public final Messages messages;
    public final AutoMarking autoMarking;
    public final BingoWebSocketClient wsClient;
    public final GameBoard gameBoard;

    public final String gameCode;
    public State state;
    private int countdown;

    private Map<UUID, WorldManager.WorldSet> playerWorldSetMap;
    private final Map<UUID, PlayerBoard> playerBoardMap = new HashMap<>();

    public BingoGame(MCBingoPlugin plugin, String gameCode, Collection<Player> players) {
        this.plugin = plugin;
        this.gameCode = gameCode;
        this.messages = new Messages(this);
        this.autoMarking = new AutoMarking(this);
        this.gameBoard = new GameBoard(this);

        URI uri = plugin.getWebsocketUrl(this.gameCode);
        this.wsClient = new BingoWebSocketClient(this, uri);

        this.state = State.PREPARING;
        this.messages.announcePreparingGame();

        // Start connecting to the websocket and then preparing worldsets simultaneously.
        this.wsClient.connect();  // does not block
        this.prepareWorldSets(players);

        // Wait until both WS is connected and worlds are ready, then mark the game as ready.
        this.transitionToReady();
    }

    public void destroy() {
        this.state = State.DONE;
        this.plugin.getLogger().info("Destroying game " + gameCode);
        this.messages.basicAnnounce("The game has ended!");

        if (this.wsClient != null) {
            this.wsClient.close();
        }
        this.unloadWorldSets();
        this.autoMarking.destroy();
    }

    public synchronized void transitionToReady() {
        if (this.state == State.PREPARING
            && this.wsClient.isOpen()
            && this.gameBoard.isReady()
            && this.playerWorldSetMap != null) {
            this.preparePlayerBoards(this.getPlayers());
            this.messages.announceGameReady(this.getPlayers());
            this.state = State.READY;
        }
    }

    public void start(CommandSender commander) {
        if (this.state != State.READY) {
            this.messages.basicTell(commander, "Game is not yet ready to be started!");
            return;
        }

        this.wipePlayers(this.getPlayers());
        this.applyStartingEffects(this.getPlayers(), 7 * 20);
        this.teleportPlayersToWorlds(this.getPlayers());

        // Countdown is from 7, but only numbers from 5 and below are displayed
        if (this.wsClient != null) {
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin,
                this.wsClient::sendRevealBoard, 7 * 20);
        }
        this.countdown = 7;
        this.doCountdown();
        this.state = State.RUNNING;
    }

    private void prepareWorldSets(Collection<Player> players) {
        Map<UUID, WorldManager.WorldSet> newWorldSetMap = new HashMap<>();
        for (Player p : players) {
            WorldManager.WorldSet ws = this.plugin.worldManager.createWorlds(
                gameCode + "_" + p.getName(), gameCode);
            newWorldSetMap.put(p.getUniqueId(), ws);
        }

        synchronized (this) {
            this.playerWorldSetMap = newWorldSetMap;
        }

        this.plugin.getLogger().info("Finished generating " + players.size() + " worlds");
    }

    private void unloadWorldSets() {
        for (UUID playerUuid : this.playerWorldSetMap.keySet()) {
            this.plugin.worldManager.unloadWorlds(this.playerWorldSetMap.get(playerUuid));
            this.playerWorldSetMap.put(playerUuid, null);
        }
    }

    private void preparePlayerBoards(Collection<Player> players) {
        for (Player p : players) {
            this.playerBoardMap.put(p.getUniqueId(),
                new PlayerBoard(p.getUniqueId(), p.getName(), this));
        }
    }

    private void teleportPlayersToWorlds(Collection<Player> players) {
        for (Player p : players) {
            World overworld = this.playerWorldSetMap.get(p.getUniqueId()).getWorld(World.Environment.NORMAL);
            overworld.setTime(0);
            p.teleport(overworld.getSpawnLocation());
        }
    }

    private void wipePlayers(Collection<Player> players) {
        CommandSender console = this.plugin.getServer().getConsoleSender();

        // For testing purposes, don't wipe some things if I am the only one in this game.
        boolean debugConvenience = (this.plugin.debug && players.size() == 1);

        for (Player p : players) {
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            p.setExhaustion(0);
            p.setExp(0);
            p.setLevel(0);
            p.setStatistic(Statistic.TIME_SINCE_REST, 0);  // reset Phantom spawns
            if (!debugConvenience) {
                p.setGameMode(GameMode.SURVIVAL);
            }
            for (PotionEffect e : p.getActivePotionEffects()) {
                p.removePotionEffect(e.getType());
            }
        }

        this.plugin.getServer().dispatchCommand(console, "advancement revoke @a everything");
        this.plugin.getServer().dispatchCommand(console, "clear @a");
    }

    private void doCountdown() {
        if (this.countdown <= 5 && this.countdown > 0) {
            for (Player p : this.getPlayers()) {
                p.sendTitle(this.countdown + "", null, 2, 16, 2);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 4.0f, 4.0f);
            }
        }
        this.countdown--;
        if (this.countdown > 0) {
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, this::doCountdown, 20);
        }
    }

    private void applyStartingEffects(Collection<Player> players, int ticks) {
        for (Player p : players) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ticks, 6));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ticks, 128));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, ticks, 5));
        }
    }

    /**
     * Return a list of players that are participating in this game and connected to the server.
     */
    public Collection<Player> getPlayers() {
        ArrayList<Player> list = new ArrayList<>();
        for (UUID uuid : this.playerWorldSetMap.keySet()) {
            Player p = this.plugin.getServer().getPlayer(uuid);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Find which player is associated to a world.
     * @param world The world to check.
     * @return The player, or null if this world is not part of this game.
     */
    public Player getPlayerInWorld(@NotNull World world) {
        WorldManager.WorldSet ws = this.plugin.worldManager.findWorldSet(world);
        for (UUID p : this.playerWorldSetMap.keySet()) {
            if (this.playerWorldSetMap.get(p).equals(ws)) {
                return this.plugin.getServer().getPlayer(p);
            }
        }

        this.plugin.getLogger().warning("getPlayerInWorld did not find a player for " + world.getName());
        return null;
    }

    public PlayerBoard getPlayerBoard(String name) {
        OfflinePlayer p = this.plugin.getServer().getOfflinePlayerIfCached(name);
        if (p == null) {
            return null;
        }

        return this.playerBoardMap.get(p.getUniqueId());
    }

    public enum State {
        PREPARING,
        READY,
        RUNNING,
        DONE,
        FAILED,
    }
}
