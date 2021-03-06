package com.jtprince.bingo.plugin;

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

public class BingoGame {
    public final MCBingoPlugin plugin;
    public final Messages messages;
    public BingoWebSocketClient wsClient;
    public final GameBoard gameBoard;

    public String gameCode;
    public State state;
    private int countdown;

    private final Set<BingoPlayer> players;

    public BingoGame(MCBingoPlugin plugin, GameSettings settings, Collection<BingoPlayer> players) {
        this.plugin = plugin;

        this.players = new HashSet<>(players);

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
                URI wsUrl = MCBConfig.getWebsocketUrl(this.gameCode);
                this.wsClient = new BingoWebSocketClient(this, wsUrl);

                this.state = State.PREPARING;
                this.messages.announcePreparingGame();
                this.messages.tellPlayerTeams(players);

                // Start connecting to the websocket and then preparing worldsets simultaneously.
                // Neither call blocks.
                this.wsClient.connect();
                this.prepareWorldSets(players);
            }
        });
    }

    public void destroy() {
        this.state = State.DONE;
        MCBingoPlugin.logger().info("Destroying game " + gameCode);
        this.messages.basicAnnounce("The game has ended!");

        this.gameBoard.destroy();
        if (this.wsClient != null) {
            this.wsClient.close();
        }
        this.unloadWorldSets();
    }

    public synchronized void transitionToReady() {
        if (this.state == State.PREPARING
            && this.wsClient.isOpen()
            && this.gameBoard.isReady()
            && this.players.stream().allMatch(p -> p.getWorldSet() != null)) {
            this.preparePlayerBoards(this.getPlayers());
            this.messages.announceGameReady(this.getPlayers());
            this.state = State.READY;
        }
    }

    public void start(CommandSender commander, boolean debug) {
        if (this.state != State.READY) {
            this.messages.basicTell(commander, "Game is not yet ready to be started!");
            return;
        }

        if (!debug) {
            this.wipePlayers(this.getBukkitPlayers());
            this.applyStartingEffects(this.getBukkitPlayers(), 7 * 20);
        }
        this.teleportPlayersToWorlds(this.getPlayers());

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

    private void prepareWorldSets(Collection<BingoPlayer> players) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            for (BingoPlayer p : players) {
                WorldManager.WorldSet ws = this.plugin.worldManager.createWorlds(
                    gameCode + "_" + p.getSlugName(), gameCode);
                p.setWorldSet(ws);
            }

            MCBingoPlugin.logger().info("Finished generating " + players.size() + " worlds");
            this.transitionToReady();
        });
    }

    private void unloadWorldSets() {
        for (BingoPlayer player : this.players) {
            this.plugin.worldManager.unloadWorlds(player.getWorldSet());
            player.setWorldSet(null);
        }
    }

    private void preparePlayerBoards(Collection<BingoPlayer> players) {
        for (BingoPlayer p : players) {
            p.setPlayerBoard(new PlayerBoard(p, this));
        }
    }

    private void teleportPlayersToWorlds(Collection<BingoPlayer> players) {
        for (BingoPlayer bp : players) {
            World overworld = bp.getWorldSet().getWorld(World.Environment.NORMAL);
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
     * Return a list of BingoPlayers that are participating in this game.
     */
    public @NotNull Collection<BingoPlayer> getPlayers() {
        return this.players;
    }

    /**
     * Return a list of Players that are participating in this game and connected to the server.
     */
    public @NotNull Collection<Player> getBukkitPlayers() {
        ArrayList<Player> list = new ArrayList<>();
        for (BingoPlayer p : this.players) {
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
        for (BingoPlayer p : this.players) {
            if (p.getWorldSet().equals(ws)) {
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
        for (BingoPlayer p : this.players) {
            if (p.getBukkitPlayers().contains(player)) {
                return p;
            }
        }
        MCBingoPlugin.logger().fine(
            "getBingoPlayer did not find a player for Player " + player.getName());
        return null;
    }

    /**
     * Find a BingoPlayer with a given name.
     * @param name String to look for.
     * @return The BingoPlayer object, or null if no BingoPlayer exists of this name.
     */
    public @Nullable BingoPlayer getBingoPlayer(@NotNull String name) {
        for (BingoPlayer p : this.players) {
            if (p.getName().equals(name) || p.getSlugName().equals(name)) {
                return p;
            }
        }
        MCBingoPlugin.logger().fine(
            "getBingoPlayer did not find a player for String " + name);
        return null;
    }

    public enum State {
        CREATING_BOARD,
        PREPARING,
        READY,
        RUNNING,
        DONE,
        FAILED,
    }
}
