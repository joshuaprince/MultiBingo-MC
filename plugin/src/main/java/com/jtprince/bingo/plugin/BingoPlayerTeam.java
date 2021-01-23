package com.jtprince.bingo.plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class BingoPlayerTeam implements BingoPlayer {
    private final Collection<UUID> playerUuids;
    private final String teamName;
    private final @Nullable ChatColor color;

    public BingoPlayerTeam(String teamName, Collection<OfflinePlayer> teammates,
                           @Nullable ChatColor color) {
        this.playerUuids = teammates.stream().map(OfflinePlayer::getUniqueId)
            .collect(Collectors.toCollection(ArrayList::new));
        this.teamName = teamName;
        this.color = color;
    }

    public BingoPlayerTeam(String teamName, Collection<OfflinePlayer> teammates) {
        this(teamName, teammates, null);
    }

    @Override
    public @NotNull String getName() {
        return this.teamName;
    }

    @Override
    public @NotNull BaseComponent getFormattedName() {
        TextComponent component = new TextComponent(this.teamName);
        component.setColor(color);
        return component;
    }

    @Override
    public @NotNull Collection<Player> getBukkitPlayers() {
        return this.playerUuids.stream().map(Bukkit::getPlayer)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
