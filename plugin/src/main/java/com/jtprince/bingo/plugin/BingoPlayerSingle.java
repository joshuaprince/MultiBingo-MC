package com.jtprince.bingo.plugin;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class BingoPlayerSingle implements BingoPlayer {
    private final UUID playerUuid;

    public BingoPlayerSingle(OfflinePlayer player) {
        this.playerUuid = player.getUniqueId();
    }

    @Override
    public @NotNull String getName() {
        String playerName = Bukkit.getOfflinePlayer(playerUuid).getName();
        if (playerName == null) {
            // TODO proper logger
            Bukkit.getLogger().warning("getName for player " + playerUuid + " is null");
            return playerUuid.toString();
        }

        return playerName;
    }

    @Override
    public @NotNull BaseComponent getFormattedName() {
        return new TextComponent(this.getName());
    }

    @Override
    public @NotNull Collection<Player> getBukkitPlayers() {
        return Collections.singleton(Bukkit.getPlayer(playerUuid));
    }
}
