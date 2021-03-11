package com.jtprince.bingo.plugin.player;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class BingoPlayer {
    /**
     * The name that should be used on the WebSocket, and will be displayed on the webpage.
     */
    public abstract @NotNull String getName();

    /**
     * The name that should be used on the WebSocket, and will be displayed on the webpage.
     */
    public abstract @NotNull BaseComponent getFormattedName();

    /**
     * Get a list of {@link org.bukkit.entity.Player}s that are online playing as this BingoPlayer.
     * If no Bukkit Players playing as this BingoPlayer are online, returns an empty collection.
     */
    public abstract @NotNull Collection<Player> getBukkitPlayers();

    /**
     * Get the name with Spaces stripped out
     */
    public String getSlugName() {
        return this.getName().replace(" ", "");
    }
}
