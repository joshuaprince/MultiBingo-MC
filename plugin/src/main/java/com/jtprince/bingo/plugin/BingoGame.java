package com.jtprince.bingo.plugin;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BingoGame {
    final MCBingoPlugin plugin;
    final Messages messages;
    final AutoActivation autoActivation;
    final BingoWebSocketClient wsClient;
    final GameBoard gameBoard = new GameBoard();

    protected final String gameCode;
    protected final Map<Player, WorldManager.WorldSet> playerWorldSetMap = new HashMap<>();
    protected final Map<Player, PlayerBoard> playerBoardMap = new HashMap<>();
    protected int countdown;

    public BingoGame(MCBingoPlugin plugin, String gameCode) {
        this.plugin = plugin;
        this.gameCode = gameCode;
        this.messages = new Messages(this);
        this.autoActivation = new AutoActivation(this);

        URI uri = plugin.getWebsocketUrl(this.gameCode);
        this.wsClient = new BingoWebSocketClient(this, uri);
    }

    public void prepare(Collection<Player> players) {
        this.messages.announcePreparingGame();

        this.prepareWorldSets(players);
        this.preparePlayerBoards(players);
        this.wsClient.connect();

        this.messages.announceGameReady(players);
    }

    public void start() {
        this.wipePlayers(this.playerWorldSetMap.keySet());
        this.teleportPlayersToWorlds(this.playerWorldSetMap.keySet());

        // Countdown is from 7, but only numbers from 5 and below are displayed
        if (this.wsClient != null) {
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin,
                this.wsClient::sendRevealBoard, 7 * 20);
        }
        this.countdown = 7;
        this.applyStartingEffects(this.playerWorldSetMap.keySet(), 7 * 20);
        this.doCountdown();
    }

    protected void prepareWorldSets(Collection<Player> players) {
        for (Player p : players) {
            WorldManager.WorldSet ws = this.plugin.worldManager.createWorlds(
                gameCode + "_" + p.getName(), gameCode);
            this.playerWorldSetMap.put(p, ws);
        }
    }

    protected void preparePlayerBoards(Collection<Player> players) {
        for (Player p : players) {
            this.playerBoardMap.put(p, new PlayerBoard(p, this));
        }
    }

    protected void teleportPlayersToWorlds(Collection<Player> players) {
        for (Player p : players) {
            World overworld = this.playerWorldSetMap.get(p).getWorld(World.Environment.NORMAL);
            overworld.setTime(0);
            p.teleport(overworld.getSpawnLocation());
        }
    }

    protected void wipePlayers(Collection<Player> players) {
        CommandSender console = this.plugin.getServer().getConsoleSender();

        for (Player p : players) {
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            for (PotionEffect e : p.getActivePotionEffects()) {
                p.removePotionEffect(e.getType());
            }
        }

        this.plugin.getServer().dispatchCommand(console, "advancement revoke @a everything");
        this.plugin.getServer().dispatchCommand(console, "clear @a");
    }

    protected void doCountdown() {
        if (this.countdown <= 5 && this.countdown > 0) {
            for (Player p : this.playerWorldSetMap.keySet()) {
                p.sendTitle(this.countdown + "", null, 2, 16, 2);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 4.0f, 4.0f);
            }
        }
        this.countdown--;
        if (this.countdown > 0) {
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, this::doCountdown, 20);
        }
    }

    protected void applyStartingEffects(Collection<Player> players, int ticks) {
        for (Player p : players) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ticks, 6));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ticks, 128));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, ticks, 5));
        }
    }

    /**
     * Find which player is associated to a world.
     * @param world The world to check.
     * @return The player, or null if this world is not part of this game.
     */
    public Player getPlayerInWorld(@NotNull World world) {
        for (Player p : this.playerWorldSetMap.keySet()) {
            WorldManager.WorldSet worldSet = this.playerWorldSetMap.get(p);
            for (World w : worldSet.map.values()) {
                if (w.equals(world)) {
                    return p;
                }
            }
        }

        this.plugin.getLogger().finer("getPlayerInWorld did not find a player for " + world.getName());
        return null;
    }

    public Player getPlayerByName(String name) {
        for (Player p : this.playerBoardMap.keySet()) {
            if (p.getName().equals(name)) {
                return p;
            }
        }

        return null;
    }
}
