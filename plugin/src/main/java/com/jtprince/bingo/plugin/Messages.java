package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.player.BingoPlayer;
import com.jtprince.bingo.plugin.player.BingoPlayerRemote;
import com.jtprince.bingo.plugin.player.BingoPlayerTeam;
import com.jtprince.util.ChatUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public class Messages {
    final BingoGame game;

    private static final TextColor COLOR_HEADER = NamedTextColor.GOLD;
    private static final TextColor COLOR_TEXT = NamedTextColor.AQUA;

    private static final TextComponent HEADER_NO_GAME = Component.text("[BINGO]")
            .color(COLOR_HEADER).decoration(TextDecoration.BOLD, true);

    public Messages(BingoGame game) {
        this.game = game;
    }

    private TextComponent withHeader(TextComponent component) {
        if (this.game.gameCode != null) {
            return Component.empty().color(COLOR_TEXT)
                .append(HEADER_NO_GAME.hoverEvent(Component.text("Game Code: " + game.gameCode)))
                .append(Component.space())
                .append(component);
        } else {
            return withNoGameHeader(component);
        }
    }

    private static TextComponent withNoGameHeader(TextComponent component) {
        return Component.empty().color(COLOR_TEXT)
            .append(HEADER_NO_GAME)
            .append(Component.space())
            .append(component);
    }

    private void sendWithHeader(Audience a, TextComponent component) {
        a.sendMessage(withHeader(component));
    }

    private void announceWithHeader(TextComponent component) {
        MCBingoPlugin.instance().getServer().sendMessage(withHeader(component));
    }

    private static void sendGamelessWithHeader(Audience a, TextComponent component) {
        a.sendMessage(withNoGameHeader(component));
    }

    /* Begin external facing calls */

    public void basicTell(Audience a, String msg) {
        sendWithHeader(a, Component.text(msg));
    }

    public void basicAnnounce(String msg) {
        sendWithHeader(MCBingoPlugin.instance().getServer(), Component.text(msg));
    }

    public static void basicTellNoGame(Audience a, String msg) {
        sendGamelessWithHeader(a, Component.text(msg));
    }

    public void announcePreparingGame() {
        TextComponent component = Component
            .text("Generating worlds for new game ")
            .append(Component.text(game.gameCode).color(NamedTextColor.BLUE))
            .append(Component.text("."));
        announceWithHeader(component);

        component = Component.text("This will cause the server to lag!");
        announceWithHeader(component);
    }

    public void announceGameFailed() {
        TextComponent component = Component
            .text("Failed to connect to the Bingo board.")
            .color(NamedTextColor.RED);
        announceWithHeader(component);
    }

    public void announceWorldsGenerated(Collection<BingoPlayer> players) {
        players = new HashSet<>(players);
        players.add(new BingoPlayerRemote("Hello world"));
        TextComponent playersCpnt = ChatUtils.commaSeparated(
            players.stream().map(BingoPlayer::getFormattedName)
                .collect(Collectors.toUnmodifiableSet())
        );
        TextComponent component = Component
            .text("Bingo worlds have been generated for ")
            .append(Component.text(players.size() + " players.").hoverEvent(playersCpnt));

        announceWithHeader(component);
    }

    public void announceGameReady(Collection<BingoPlayer> players) {
        TextComponent component;
        for (BingoPlayer p : players) {
            // Game link for this specific player
            URL url = MCBConfig.getGameUrl(this.game.gameCode, p);
            if (url == null) {
                component = Component
                    .text("Could not get the board to link you to! Contact the server admin "
                        + "to update the plugin's config.yml.")
                    .color(NamedTextColor.RED);
            } else {
                component = Component.empty()
                    .append(Component.text("[Open Board]")
                        .decoration(TextDecoration.UNDERLINED, true)
                        .color(NamedTextColor.YELLOW)
                        .hoverEvent(Component.text(url.toString()))
                        .clickEvent(ClickEvent.openUrl(url))
                    )
                    .append(Component.space())
                    .append(Component.text("[START]")
                        .decoration(TextDecoration.UNDERLINED, true)
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/bingo start"))
                        .hoverEvent(Component.text("Start Game (/bingo start)"))
                    );
            }

            for (Player bukkitPlayer : p.getBukkitPlayers()) {
                sendWithHeader(bukkitPlayer, component);
            }
        }
    }

    public void announcePlayerMarking(BingoPlayer player, Space space, boolean invalidated) {
        TextComponent component;
        if (!invalidated) {
            component = Component.empty()
                .append(player.getFormattedName())
                .append(Component.text(" has marked "))
                .append(Component.text(space.text).color(NamedTextColor.GREEN))
                .append(Component.text("!"));
        } else {
            component = Component.empty()
                .append(player.getFormattedName())
                .append(Component.text(" has invalidated "))
                .append(Component.text(space.text).color(NamedTextColor.RED))
                .append(Component.text("!"));
        }

        announceWithHeader(component);
    }

    public void tellPlayerTeams(Collection<BingoPlayer> players) {
        for (BingoPlayer bp : players) {
            if (bp instanceof BingoPlayerTeam) {
                BingoPlayerTeam bpt = (BingoPlayerTeam) bp;

                Component component = Component.text("You are playing on team ")
                    .append(bpt.getFormattedName())
                    .append(Component.text("!"));
                Audience.audience(bpt.getBukkitPlayers()).sendMessage(component);
            }
        }
    }
}
