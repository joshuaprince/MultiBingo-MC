package com.jtprince.bingo.plugin;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface BingoPlayer {
    /**
     * The name that should be used on the WebSocket, and will be displayed on the webpage.
     */
    @NotNull String getName();

    /**
     * The name that should be used on the WebSocket, and will be displayed on the webpage.
     */
    @NotNull BaseComponent  getFormattedName();

    /**
     * Get a list of {@link org.bukkit.entity.Player}s that are online playing as this BingoPlayer.
     */
    @NotNull Collection<Player> getBukkitPlayers();
}
