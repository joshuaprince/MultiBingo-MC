package com.jtprince.bingo.plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.URL;
import java.util.Collection;

public class Messages {
    final BingoGame game;

    private static final ChatColor COLOR_HEADER = ChatColor.GOLD;
    private static final ChatColor COLOR_TEXT = ChatColor.AQUA;

    private static final BaseComponent[] HEADER_NO_GAME = new ComponentBuilder("[BINGO]")
        .color(COLOR_HEADER).bold(true)
        .append(" ", ComponentBuilder.FormatRetention.NONE).color(COLOR_TEXT)
        .create();

    public Messages(BingoGame game) {
        this.game = game;
    }

    public BaseComponent[] getHeader() {
        ComponentBuilder builder = new ComponentBuilder(HEADER_NO_GAME[0]);

        if (this.game.gameCode != null) {
            builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text("Game Code: " + game.gameCode)));

        }

        builder.append(HEADER_NO_GAME[1], ComponentBuilder.FormatRetention.NONE);

        return builder.create();
    }

    public void basicTell(CommandSender sender, String msg) {
        BaseComponent[] components = new ComponentBuilder()
            .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
            .append(msg)
            .create();
        sender.sendMessage(components);
    }

    public void basicAnnounce(String msg) {
        BaseComponent[] components = new ComponentBuilder()
            .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
            .append(msg)
            .create();
        this.game.plugin.getServer().broadcast(components);
    }

    public static void basicTellNoGame(CommandSender sender, String msg) {
        BaseComponent[] components = new ComponentBuilder()
            .append(HEADER_NO_GAME, ComponentBuilder.FormatRetention.NONE)
            .append(msg)
            .create();
        sender.sendMessage(components);
    }

    public void announcePreparingGame() {
        BaseComponent[] components = new ComponentBuilder()
            .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
            .append("Generating worlds for new game ")
            .append(game.gameCode).color(ChatColor.BLUE)
            .append(".").color(COLOR_TEXT)
            .create();
        this.game.plugin.getServer().broadcast(components);

        components = new ComponentBuilder()
            .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
            .append("This will cause the server to lag!")
            .create();
        this.game.plugin.getServer().broadcast(components);
    }

    public void announceGameFailed() {
        BaseComponent[] components = new ComponentBuilder()
            .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
            .append("Failed to connect to the Bingo board.").color(ChatColor.RED)
            .create();
        this.game.plugin.getServer().broadcast(components);
    }

    public void announceGameReady(Collection<BingoPlayer> players) {
        BaseComponent[] components = new ComponentBuilder()
            .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
            .append("Bingo worlds have been generated for " + players.size() + " players.")
            .create();
        this.game.plugin.getServer().broadcast(components);

        for (BingoPlayer p : players) {
            // Game link
            URL url = MCBConfig.getGameUrl(this.game.gameCode, p);
            if (url == null) {
                components = new ComponentBuilder()
                    .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
                    .append("Could not get the board to link you to! Contact the server admin to "
                        + "update the plugin's config.yml.").color(ChatColor.RED)
                    .create();
            } else {
                components = new ComponentBuilder()
                    .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
                    .append("[Open Board]").underlined(true).color(ChatColor.YELLOW)
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, url.toString()))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(url.toString())))
                    .append(" ", ComponentBuilder.FormatRetention.NONE)
                    .append("[START]").underlined(true).color(ChatColor.GREEN)
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo start"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Start Game")))
                    .create();
            }
            for (Player bukkitPlayer : p.getBukkitPlayers()) {
                bukkitPlayer.sendMessage(components);
            }
        }
    }

    public void announcePlayerMarking(BingoPlayer player, Space space, boolean invalidated) {
        BaseComponent[] components;
        if (!invalidated) {
            components = new ComponentBuilder()
                .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
                .append(player.getFormattedName())
                .append(" has marked ", ComponentBuilder.FormatRetention.NONE).color(COLOR_TEXT)
                .append(space.text).color(ChatColor.GREEN)
                .append("!").color(COLOR_TEXT)
                .create();
        } else {
            components = new ComponentBuilder()
                .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
                .append(player.getFormattedName())
                .append(" has invalidated ", ComponentBuilder.FormatRetention.NONE).color(COLOR_TEXT)
                .append(space.text).color(ChatColor.RED)
                .append("!").color(COLOR_TEXT)
                .create();
        }

        this.game.plugin.getServer().broadcast(components);
    }

    public void tellPlayerTeams(Collection<BingoPlayer> players) {
        for (BingoPlayer bp : players) {
            if (bp instanceof BingoPlayerTeam) {
                BingoPlayerTeam bpt = (BingoPlayerTeam) bp;
                BaseComponent[] components = new ComponentBuilder()
                    .append(getHeader(), ComponentBuilder.FormatRetention.NONE)
                    .append("You are playing on team ")
                    .append(bpt.getFormattedName())
                    .append("!").color(COLOR_TEXT)
                    .create();
                for (Player p : bpt.getBukkitPlayers()) {
                    p.sendMessage(components);
                }
            }
        }
    }
}
