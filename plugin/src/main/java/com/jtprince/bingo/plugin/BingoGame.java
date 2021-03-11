package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.player.BingoPlayer;
import com.jtprince.bingo.plugin.player.BingoPlayerRemote;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class BingoGame {
    public final MCBingoPlugin plugin;
    public final Messages messages;
    public BingoWebSocketClient wsClient;
    public final GameBoard gameBoard;

    public String gameCode;
    public State state;
    private int countdown;

    private final Set<BingoPlayer> players;
    private final HashMap<BingoPlayer, WorldManager.WorldSet> playerWorldSetMap;
    private final HashMap<BingoPlayer, PlayerBoard> playerBoardMap;

    public BingoGame(MCBingoPlugin plugin, GameSettings settings, Collection<BingoPlayer> players) {
        this.plugin = plugin;

        this.players = new HashSet<>(players);
        this.playerWorldSetMap = new HashMap<>();
        this.playerBoardMap = new HashMap<>();

        this.messages = new Messages(this);
        this.gameBoard = new GameBoard(this);

        this.generateBoard(settings);
    }

    /**
     * Generate a board with the backend and kick off the next steps when it's done
     */
    public void generateBoard(GameSettings settings) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.state = State.CREATING_BOARD;
            String gameCode;
            try {
                gameCode = settings.generateBoardBlocking();
            } catch (IOException | ParseException e) {
                e.printStackTrace();
                return;
            }

            synchronized (this) {
                this.gameCode = gameCode;
                URI wsUrl = MCBConfig.getWebsocketUrl(this.gameCode, this.getLocalPlayers());
                this.wsClient = new BingoWebSocketClient(this, wsUrl);

                this.state = State.PREPARING;
                this.messages.announcePreparingGame();
                this.messages.tellPlayerTeams(this.getLocalPlayers());

                // Start connecting to the websocket and then preparing worldsets simultaneously.
                // Neither call blocks.
                this.wsClient.connect();
                this.prepareWorldSets();
            }
        });
    }

    public void destroy() {
        this.state = State.DONE;
        MCBingoPlugin.logger().info("Destroying game " + gameCode);
        this.messages.basicAnnounce("The game has ended!");

        if (this.gameBoard != null) {
            this.gameBoard.destroy();
        }
        if (this.wsClient != null) {
            this.wsClient.close();
        }
        this.unloadWorldSets();
    }

    public synchronized void transitionToReady() {
        if (this.state == State.PREPARING
            && this.wsClient.isOpen()
            && this.gameBoard.isReady()
            && this.getLocalPlayers().stream().allMatch(p -> this.getWorldSet(p) != null)) {
            this.messages.announceGameReady(this.getLocalPlayers());
            this.state = State.READY;
        }
    }

    public synchronized void start() {
        boolean debug = MCBConfig.getDebug() && this.players.size() == 1;

        if (!debug) {
            this.wipePlayers(this.getBukkitPlayers());
            this.applyStartingEffects(this.getBukkitPlayers(), 7 * 20);
        }
        this.teleportPlayersToWorlds(this.getLocalPlayers());

        if (debug) {
            this.wsClient.sendRevealBoard();
        } else {
            // Countdown is from 7, but only numbers from 5 and below are displayed
            if (this.wsClient != null) {
                this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin,
                    this.wsClient::sendRevealBoard, 7 * 20);
            }
            this.countdown = 7;
            this.doCountdown();
        }

        this.state = State.RUNNING;
    }

    private void prepareWorldSets() {
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            for (BingoPlayer p : this.getLocalPlayers()) {
                WorldManager.WorldSet ws = this.plugin.worldManager.createWorlds(
                    gameCode + "_" + p.getSlugName(), gameCode);
                this.setWorldSet(p, ws);
            }

            MCBingoPlugin.logger().info("Finished generating " + this.getLocalPlayers().size() + " worlds");
            this.messages.announceWorldsGenerated(this.getLocalPlayers());
            this.transitionToReady();
        });
    }

    private void unloadWorldSets() {
        for (BingoPlayer player : this.getLocalPlayers()) {
            this.plugin.worldManager.unloadWorlds(this.getWorldSet(player));
            this.setWorldSet(player, null);
        }
    }

    private void teleportPlayersToWorlds(Collection<BingoPlayer> players) {
        for (BingoPlayer bp : players) {
            if (bp instanceof BingoPlayerRemote) {
                continue;
            }

            World overworld = this.getWorldSet(bp).getWorld(World.Environment.NORMAL);
            overworld.setTime(0);
            for (Player p : bp.getBukkitPlayers()) {
                p.teleport(overworld.getSpawnLocation());
            }
        }
    }

    private void wipePlayers(Collection<Player> players) {
        CommandSender console = this.plugin.getServer().getConsoleSender();

        for (Player p : players) {
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            p.setExhaustion(0);
            p.setExp(0);
            p.setLevel(0);
            p.setStatistic(Statistic.TIME_SINCE_REST, 0);  // reset Phantom spawns
            p.setGameMode(GameMode.SURVIVAL);
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
     * Return a list of BingoPlayers that are participating in this game. This includes all
     * remote players (i.e. that are not logged in to this server).
     */
    public @NotNull Collection<BingoPlayer> getAllPlayers() {
        return this.players;
    }

    /**
     * Return a list of BingoPlayers that are participating in this game. This includes all
     * remote players (i.e. that are not logged in to this server).
     */
    public @NotNull Collection<BingoPlayer> getLocalPlayers() {
        return this.players.stream().filter(p -> !(p instanceof BingoPlayerRemote))
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Return a list of Players that are participating in this game and connected to the server.
     */
    public @NotNull Collection<Player> getBukkitPlayers() {
        ArrayList<Player> list = new ArrayList<>();
        for (BingoPlayer p : this.getLocalPlayers()) {
            list.addAll(p.getBukkitPlayers());
        }
        return list;
    }

    /**
     * Find which BingoPlayer is associated to a world.
     * @param world The world to check.
     * @return The BingoPlayer object, or null if this world is not part of this game.
     */
    public @Nullable BingoPlayer getBingoPlayer(@NotNull World world) {
        WorldManager.WorldSet ws = this.plugin.worldManager.findWorldSet(world);
        for (BingoPlayer p : this.getLocalPlayers()) {
            if (this.getWorldSet(p).equals(ws)) {
                return p;
            }
        }
        MCBingoPlugin.logger().fine(
            "getPlayerInWorld did not find a player for World " + world.getName());
        return null;
    }

    /**
     * Find which BingoPlayer is associated to a Bukkit Player.
     * @param player The Bukkit Player to check.
     * @return The BingoPlayer object, or null if this Player is not part of this game.
     */
    public @Nullable BingoPlayer getBingoPlayer(@NotNull Player player) {
        for (BingoPlayer p : this.getLocalPlayers()) {
            if (p.getBukkitPlayers().contains(player)) {
                return p;
            }
        }
        MCBingoPlugin.logger().fine(
            "getBingoPlayer did not find a player for Player " + player.getName());
        return null;
    }

    /**
     * Find a BingoPlayer with a given name. If there is no player with the given name on this
     * server, a BingoPlayerRemote will be returned.
     * @param name String to look for.
     * @return The BingoPlayer object.
     */
    public @NotNull BingoPlayer getBingoPlayer(@NotNull String name) {
        for (BingoPlayer p : this.getAllPlayers()) {
            if (p.getName().equals(name) || p.getSlugName().equals(name)) {
                return p;
            }
        }

        BingoPlayerRemote newPlayer = new BingoPlayerRemote(name);
        this.players.add(newPlayer);
        MCBingoPlugin.logger().info("Added Remote Player " + newPlayer.getName());
        return newPlayer;
    }

    /**
     * Find a BingoPlayer with a given name. If there is no player with the given name on this
     * server, null will be returned.
     * @param name String to look for.
     * @return The BingoPlayer object or null.
     */
    public @Nullable BingoPlayer getLocalPlayer(@NotNull String name) {
        for (BingoPlayer p : this.getLocalPlayers()) {
            if (p.getName().equals(name) || p.getSlugName().equals(name)) {
                return p;
            }
        }

        return null;
    }

    public synchronized WorldManager.WorldSet getWorldSet(BingoPlayer player) {
        if (player instanceof BingoPlayerRemote) {
            throw new UnsupportedOperationException("Cannot call getWorldSet for a Remote player.");
        }
        return playerWorldSetMap.get(player);
    }

    private synchronized void setWorldSet(BingoPlayer player, WorldManager.WorldSet worldSet) {
        if (player instanceof BingoPlayerRemote) {
            throw new UnsupportedOperationException("Cannot call setWorldSet for a Remote player.");
        }
        playerWorldSetMap.put(player, worldSet);
    }

    public enum State {
        CREATING_BOARD,
        PREPARING,
        READY,
        RUNNING,
        DONE,
        FAILED,
    }

    public synchronized @NotNull PlayerBoard getPlayerBoard(@NotNull BingoPlayer player) {
        PlayerBoard pb = this.playerBoardMap.get(player);
        if (pb == null) {
            pb = new PlayerBoard(player, this);
            this.playerBoardMap.put(player, pb);
        }

        return pb;
    }
}
