package com.jtprince.bingo.plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

public class Messages {
    final BingoGame game;

    protected final ChatColor COLOR_HEADER = ChatColor.GOLD;
    protected final ChatColor COLOR_TEXT = ChatColor.AQUA;
    protected final BaseComponent[] HEADER;

    public Messages(BingoGame game) {
        this.game = game;
        this.HEADER = new ComponentBuilder("[BINGO]")
            .color(COLOR_HEADER).bold(true)
            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Game Code: " + game.gameCode)))
            .append(" ", ComponentBuilder.FormatRetention.NONE).bold(false).color(COLOR_TEXT)
            .create();
    }

    public void announcePreparingGame() {
        BaseComponent[] components = new ComponentBuilder()
            .append(HEADER, ComponentBuilder.FormatRetention.NONE)
            .append("Generating worlds for new game ")
            .append(game.gameCode).color(ChatColor.BLUE)
            .append(".").color(COLOR_TEXT)
            .create();
        this.game.plugin.getServer().broadcast(components);

        components = new ComponentBuilder()
            .append(HEADER, ComponentBuilder.FormatRetention.NONE)
            .append("This will cause the server to lag!")
            .create();
        this.game.plugin.getServer().broadcast(components);
    }

    public void tellGameNotReady(CommandSender sender) {
        BaseComponent[] components = new ComponentBuilder()
            .append(HEADER, ComponentBuilder.FormatRetention.NONE)
            .append("Game is not yet ready to be started!")
            .create();
        sender.sendMessage(components);
    }

    public void announceGameFailed() {
        BaseComponent[] components = new ComponentBuilder()
            .append(HEADER, ComponentBuilder.FormatRetention.NONE)
            .append("Failed to connect to the Bingo board.").color(ChatColor.RED)
            .create();
        this.game.plugin.getServer().broadcast(components);
    }

    public void announceGameReady(Collection<Player> players) {
        BaseComponent[] components = new ComponentBuilder()
            .append(HEADER, ComponentBuilder.FormatRetention.NONE)
            .append("Bingo worlds have been generated for " + players.size() + " players.")
            .create();
        this.game.plugin.getServer().broadcast(components);

        for (Player p : players) {
            // Game link
            URI url = this.game.plugin.getGameUrl(this.game.gameCode, p);
            components = new ComponentBuilder()
                .append(HEADER, ComponentBuilder.FormatRetention.NONE)
                .append("[Open Board]").underlined(true).color(ChatColor.YELLOW)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, url.toString()))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(url.toString())))
                .append(" ", ComponentBuilder.FormatRetention.NONE)
                .append("[START]").underlined(true).color(ChatColor.GREEN)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo start"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Start Game")))
                .create();
            p.sendMessage(components);
        }
    }

    public void announcePlayerMarking(UUID playerUuid, Square square, boolean invalidated) {
        OfflinePlayer player = this.game.plugin.getServer().getOfflinePlayer(playerUuid);

        BaseComponent[] components;
        if (!invalidated) {
            components = new ComponentBuilder()
                .append(HEADER, ComponentBuilder.FormatRetention.NONE)
                .append(player.getName())
                .append(" has marked ")
                .append(square.text).color(ChatColor.GREEN)
                .append("!").color(COLOR_TEXT)
                .create();
        } else {
            components = new ComponentBuilder()
                .append(HEADER, ComponentBuilder.FormatRetention.NONE)
                .append(player.getName())
                .append(" has invalidated ")
                .append(square.text).color(ChatColor.RED)
                .append("!").color(COLOR_TEXT)
                .create();
        }

        this.game.plugin.getServer().broadcast(components);
    }
}
