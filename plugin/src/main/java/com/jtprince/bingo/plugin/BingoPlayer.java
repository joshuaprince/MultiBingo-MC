package com.jtprince.bingo.plugin;

import org.bukkit.entity.Player;

import java.util.Collection;

public interface BingoPlayer {
    /**
     * The name that should be used on the WebSocket, and will be displayed on the webpage.
     */
    String getName();

    /**
     * Get a list of {@link org.bukkit.entity.Player}s that are online playing as this BingoPlayer.
     */
    Collection<Player> getBukkitPlayers();
}
