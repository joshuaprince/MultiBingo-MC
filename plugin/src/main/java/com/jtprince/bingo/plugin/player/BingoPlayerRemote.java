package com.jtprince.bingo.plugin.player;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents a Bingo Player that the web server knows about, but that this plugin does not know
 * about. No Bukkit player will ever exist for this player, and it should only used for referencing
 * a remote player (such as printing messages when a square is marked).
 */
public class BingoPlayerRemote extends BingoPlayer {
    private final String name;

    public BingoPlayerRemote(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getName() {
        return this.name;
    }

    @Override
    public @NotNull BaseComponent getFormattedName() {
        return new TextComponent(this.getName());
    }

    @Override
    public @NotNull Collection<Player> getBukkitPlayers() {
        return Collections.emptySet();
    }
}
