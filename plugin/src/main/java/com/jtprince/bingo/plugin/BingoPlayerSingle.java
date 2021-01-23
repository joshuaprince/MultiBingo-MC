package com.jtprince.bingo.plugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class BingoPlayerSingle implements BingoPlayer {
    private final UUID playerUuid;

    public BingoPlayerSingle(OfflinePlayer player) {
        this.playerUuid = player.getUniqueId();
    }

    @Override
    public String getName() {
        return Bukkit.getOfflinePlayer(playerUuid).getName();
    }

    @Override
    public Collection<Player> getBukkitPlayers() {
        return Collections.singleton(Bukkit.getPlayer(playerUuid));
    }
}
