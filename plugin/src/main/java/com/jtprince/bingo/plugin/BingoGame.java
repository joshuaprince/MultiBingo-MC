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

    private Map<BingoPlayer, WorldManager.WorldSet> playerWorldSetMap;
    private final Map<BingoPlayer, PlayerBoard> playerBoardMap = new HashMap<>();

    public BingoGame(MCBingoPlugin plugin, String gameCode, Collection<BingoPlayer> players) {
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

        this.wipePlayers(this.getBukkitPlayers());
        this.applyStartingEffects(this.getBukkitPlayers(), 7 * 20);
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

    private void prepareWorldSets(Collection<BingoPlayer> players) {
        Map<BingoPlayer, WorldManager.WorldSet> newWorldSetMap = new HashMap<>();
        for (BingoPlayer p : players) {
            WorldManager.WorldSet ws = this.plugin.worldManager.createWorlds(
                gameCode + "_" + p.getName(), gameCode);
            newWorldSetMap.put(p, ws);
        }

        synchronized (this) {
            this.playerWorldSetMap = newWorldSetMap;
        }

        this.plugin.getLogger().info("Finished generating " + players.size() + " worlds");
    }

    private void unloadWorldSets() {
        for (BingoPlayer player : this.playerWorldSetMap.keySet()) {
            this.plugin.worldManager.unloadWorlds(this.playerWorldSetMap.get(player));
            this.playerWorldSetMap.put(player, null);
        }
    }

    private void preparePlayerBoards(Collection<BingoPlayer> players) {
        for (BingoPlayer p : players) {
            this.playerBoardMap.put(p, new PlayerBoard(p, this));
        }
    }

    private void teleportPlayersToWorlds(Collection<BingoPlayer> players) {
        for (BingoPlayer bp : players) {
            World overworld = this.playerWorldSetMap.get(bp).getWorld(World.Environment.NORMAL);
            overworld.setTime(0);
            for (Player p : bp.getBukkitPlayers()) {
                p.teleport(overworld.getSpawnLocation());
            }
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
            for (Player p : this.getBukkitPlayers()) {
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
     * Return a list of BingoPlayers that are participating in this game.
     */
    public Collection<BingoPlayer> getPlayers() {
        return this.playerWorldSetMap.keySet();
    }

    /**
     * Return a list of Players that are participating in this game and connected to the server.
     */
    public Collection<Player> getBukkitPlayers() {
        ArrayList<Player> list = new ArrayList<>();
        for (BingoPlayer p : this.playerWorldSetMap.keySet()) {
            list.addAll(p.getBukkitPlayers());
        }
        return list;
    }

    public PlayerBoard getPlayerBoard(BingoPlayer player) {
        return this.playerBoardMap.get(player);
    }

    /**
     * Find which BingoPlayer is associated to a world.
     * @param world The world to check.
     * @return The BingoPlayer object, or null if this world is not part of this game.
     */
    public BingoPlayer getBingoPlayer(@NotNull World world) {
        WorldManager.WorldSet ws = this.plugin.worldManager.findWorldSet(world);
        for (BingoPlayer p : this.playerWorldSetMap.keySet()) {
            if (this.playerWorldSetMap.get(p).equals(ws)) {
                return p;
            }
        }

        this.plugin.getLogger().finest(
            "getPlayerInWorld did not find a player for World " + world.getName());
        return null;
    }

    /**
     * Find which BingoPlayer is associated to a Bukkit Player.
     * @param player The Bukkit Player to check.
     * @return The BingoPlayer object, or null if this Player is not part of this game.
     */
    public BingoPlayer getBingoPlayer(@NotNull Player player) {
        for (BingoPlayer p : this.playerWorldSetMap.keySet()) {
            if (p.getBukkitPlayers().contains(player)) {
                return p;
            }
        }
        this.plugin.getLogger().warning(
            "getBingoPlayer did not find a player for Player " + player.getName());
        return null;
    }

    /**
     * Find a BingoPlayer with a given name.
     * @param name String to look for.
     * @return The BingoPlayer object, or null if no BingoPlayer exists of this name.
     */
    public BingoPlayer getBingoPlayer(@NotNull String name) {
        for (BingoPlayer p : this.playerWorldSetMap.keySet()) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        this.plugin.getLogger().warning(
            "getBingoPlayer did not find a player for String " + name);
        return null;
    }

    public enum State {
        PREPARING,
        READY,
        RUNNING,
        DONE,
        FAILED,
    }
}
