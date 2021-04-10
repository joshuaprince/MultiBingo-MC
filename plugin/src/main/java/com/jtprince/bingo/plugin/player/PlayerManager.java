package com.jtprince.bingo.plugin.player;

import com.jtprince.bingo.plugin.BingoGame;
import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.WorldManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Single-game container for all players in that game and functionality relating to them.
 */
public class PlayerManager {
    private final BingoGame game;

    private final Set<BingoPlayer> players;
    private final ConcurrentHashMap<BingoPlayer, WorldManager.WorldSet> playerWorldSetMap;
    private final ConcurrentHashMap<BingoPlayer, PlayerBoard> playerBoardMap;

    public PlayerManager(BingoGame game, Collection<BingoPlayer> players) {
        this.game = game;
        this.players = new HashSet<>(players);
        this.playerWorldSetMap = new ConcurrentHashMap<>();
        this.playerBoardMap = new ConcurrentHashMap<>();
    }

    public void destroy() {
        this.unloadWorldSets();
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
        WorldManager.WorldSet ws = game.plugin.worldManager.findWorldSet(world);
        if (ws != null) {
            for (BingoPlayer p : this.getLocalPlayers()) {
                if (ws.equals(this.getWorldSet(p))) {
                    return p;
                }
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

    /**
     * Find the set of worlds this Bingo Player is given to play in.
     * @param player A Local BingoPlayer.
     * @return The player's WorldSet
     */
    public @Nullable WorldManager.WorldSet getWorldSet(BingoPlayer player) {
        if (player instanceof BingoPlayerRemote) {
            throw new UnsupportedOperationException("Cannot call getWorldSet for a Remote player.");
        }
        return playerWorldSetMap.get(player);
    }

    /**
     * Create worlds for a given player.
     * @param player Player to create worlds for.
     */
    public void prepareWorldSet(BingoPlayer player) {
        if (player instanceof BingoPlayerRemote) {
            throw new UnsupportedOperationException("Cannot call setWorldSet for a Remote player.");
        }
        WorldManager.WorldSet ws = game.plugin.worldManager.createWorlds(
            game.gameCode + "_" + player.getSlugName(), game.gameCode);
        playerWorldSetMap.put(player, ws);
    }

    /**
     * Unload all worlds the game is being played in.
     */
    private void unloadWorldSets() {
        for (BingoPlayer player : getLocalPlayers()) {
            WorldManager.WorldSet ws = getWorldSet(player);
            if (ws != null) {
                game.plugin.worldManager.unloadWorlds(ws);
            }
        }
    }

    /**
     * Get the board owned by a player, representing that player's markings. If a board does not
     * already exist for this player, one will be created.
     * @param player BingoPlayer (local or remote) to own this board.
     * @return Board belonging to this player.
     */
    public synchronized @NotNull PlayerBoard getPlayerBoard(@NotNull BingoPlayer player) {
        PlayerBoard pb = this.playerBoardMap.get(player);
        if (pb == null) {
            pb = new PlayerBoard(player, this.game);
            this.playerBoardMap.put(player, pb);
        }

        return pb;
    }
}
