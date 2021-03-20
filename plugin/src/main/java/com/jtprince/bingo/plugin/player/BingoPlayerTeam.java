package com.jtprince.bingo.plugin.player;

import com.jtprince.util.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class BingoPlayerTeam extends BingoPlayer {
    private final Collection<UUID> playerUuids;
    private final String teamName;
    private final @Nullable TextColor color;

    public BingoPlayerTeam(String teamName, Collection<OfflinePlayer> teammates,
                           @Nullable TextColor color) {
        this.playerUuids = teammates.stream().map(OfflinePlayer::getUniqueId)
            .collect(Collectors.toUnmodifiableSet());
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
    public @NotNull TextComponent getFormattedName() {
        TextComponent playerNames = ChatUtils.commaSeparated(
            playerUuids.stream().map(u -> Component.text(
                Objects.requireNonNull(Bukkit.getOfflinePlayer(u).getName()))
            ).collect(Collectors.toUnmodifiableSet())
        );
        return Component.text(this.teamName)
            .color(color)
            .hoverEvent(HoverEvent.showText(playerNames));
    }

    @Override
    public @NotNull Collection<Player> getBukkitPlayers() {
        return this.playerUuids.stream().map(Bukkit::getPlayer)
            .filter(Objects::nonNull).filter(OfflinePlayer::isOnline)
            .collect(Collectors.toUnmodifiableSet());
    }
}
