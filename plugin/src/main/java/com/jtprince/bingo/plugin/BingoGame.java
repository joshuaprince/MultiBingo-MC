package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.player.BingoPlayer;
import com.jtprince.bingo.plugin.player.BingoPlayerRemote;
import com.jtprince.bingo.plugin.player.PlayerManager;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class BingoGame {
    public final MCBingoPlugin plugin;
    public final PlayerManager playerManager;
    public final Messages messages;
    public BingoWebSocketClient wsClient;
    public final GameBoard gameBoard;

    private final CommandSender creator;
    public String gameCode;
    public State state;
    private int countdown;

    public BingoGame(MCBingoPlugin plugin, GameSettings settings,
                     CommandSender creator, Collection<BingoPlayer> players) {
        this.plugin = plugin;
        this.creator = creator;

        this.playerManager = new PlayerManager(this, players);
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
                URI wsUrl = MCBConfig.getWebsocketUrl(this.gameCode, playerManager.getLocalPlayers());
                this.wsClient = new BingoWebSocketClient(this, wsUrl);

                this.state = State.PREPARING;
                this.messages.announcePreparingGame();
                this.messages.tellPlayerTeams(playerManager.getLocalPlayers());

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
        if (this.playerManager != null) {
            this.playerManager.destroy();
        }
    }

    public synchronized void transitionToReady() {
        if (this.state == State.PREPARING
            && this.wsClient.isOpen()
            && this.gameBoard.isReady()
            && playerManager.getLocalPlayers().stream().allMatch(p -> playerManager.getWorldSet(p) != null)) {
            this.messages.announceGameReady(playerManager.getLocalPlayers(), creator);
            this.state = State.READY;
        }
    }

    public synchronized void start() {
        boolean debug = MCBConfig.getDebug() && playerManager.getAllPlayers().size() == 1;

        if (!debug) {
            this.wipePlayers(playerManager.getBukkitPlayers());
            this.applyStartingEffects(playerManager.getBukkitPlayers(), 7 * 20);
        }
        this.teleportPlayersToWorlds(playerManager.getLocalPlayers());

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
            for (BingoPlayer p : playerManager.getLocalPlayers()) {
                playerManager.prepareWorldSet(p);
            }

            MCBingoPlugin.logger().info("Finished generating " + playerManager.getLocalPlayers().size() + " worlds");
            this.messages.announceWorldsGenerated(playerManager.getLocalPlayers());
            this.transitionToReady();
        });
    }

    private void teleportPlayersToWorlds(Collection<BingoPlayer> players) {
        for (BingoPlayer bp : players) {
            if (bp instanceof BingoPlayerRemote) {
                continue;
            }

            World overworld = playerManager.getWorldSet(bp).getWorld(World.Environment.NORMAL);
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
            for (Player p : playerManager.getBukkitPlayers()) {
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

    public enum State {
        CREATING_BOARD,
        PREPARING,
        READY,
        RUNNING,
        DONE,
        FAILED,
    }
}
