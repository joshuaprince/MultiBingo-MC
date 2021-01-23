package com.jtprince.bingo.plugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class BingoPlayerTeam implements BingoPlayer {
    private final Collection<UUID> playerUuids;
    private final String teamName;

    public BingoPlayerTeam(String teamName, Collection<OfflinePlayer> teammates) {
        this.playerUuids = teammates.stream().map(OfflinePlayer::getUniqueId)
            .collect(Collectors.toCollection(ArrayList::new));
        this.teamName = teamName;
    }

    @Override
    public String getName() {
        return this.teamName;
    }

    @Override
    public Collection<Player> getBukkitPlayers() {
        return this.playerUuids.stream().map(Bukkit::getPlayer)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
