package com.jtprince.bingo.plugin;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class BingoPlayer {
    private WorldManager.WorldSet worldSet = null;
    private PlayerBoard playerBoard = null;

    /**
     * The name that should be used on the WebSocket, and will be displayed on the webpage.
     */
    abstract @NotNull String getName();

    /**
     * The name that should be used on the WebSocket, and will be displayed on the webpage.
     */
    abstract @NotNull BaseComponent  getFormattedName();

    /**
     * Get a list of {@link org.bukkit.entity.Player}s that are online playing as this BingoPlayer.
     */
    abstract @NotNull Collection<Player> getBukkitPlayers();

    public synchronized WorldManager.WorldSet getWorldSet() {
        return worldSet;
    }

    synchronized void setWorldSet(WorldManager.WorldSet worldSet) {
        this.worldSet = worldSet;
    }

    public synchronized PlayerBoard getPlayerBoard() {
        return playerBoard;
    }

    synchronized void setPlayerBoard(PlayerBoard playerBoard) {
        this.playerBoard = playerBoard;
    }

    /**
     * Get the name with Spaces stripped out
     */
    public String getSlugName() {
        return this.getName().replace(" ", "");
    }
}
