package com.jtprince.bingo.kplugin

import com.jtprince.bingo.kplugin.Messages.bingoTell
import com.jtprince.bingo.kplugin.game.BingoGame
import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.player.BingoPlayerTeam
import com.jtprince.util.ChatUtils
import io.ktor.http.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import java.net.URL

object Messages {
    private val COLOR_HEADER: TextColor = NamedTextColor.GOLD
    private val COLOR_TEXT: TextColor = NamedTextColor.AQUA

    private val HEADER = Component.text("[BINGO]")
        .color(COLOR_HEADER).decoration(TextDecoration.BOLD, true)

    private fun TextComponent.withBingoHeader(): TextComponent {
        val gameCode = BingoGame.currentGame?.gameCode
        var builder = Component.empty().color(COLOR_TEXT)
        builder = if (gameCode != null) {
            builder.append(HEADER.hoverEvent(HoverEvent.showText(Component.text(
                "Game Code: $gameCode"
            ))))
        } else {
            builder.append(HEADER)
        }
        return builder.append(Component.space()).append(this)
    }

    private fun sendWithHeader(a: Audience, component: TextComponent) {
        a.sendMessage(component.withBingoHeader())
    }

    private fun announceWithHeader(component: TextComponent) {
        Bukkit.getServer().sendMessage(component.withBingoHeader())
    }

    /* Begin external facing calls */

    fun Audience.bingoTell(msg: String) {
        bingoTell(Component.text(msg))
    }

    fun Audience.bingoTell(msg: TextComponent) {
        sendWithHeader(this, msg)
    }

    fun bingoAnnounce(msg: String) {
        bingoAnnounce(Component.text(msg))
    }

    fun bingoAnnounce(msg: TextComponent) {
        announceWithHeader(msg)
    }

    fun bingoAnnouncePreparingGame(gameCode: String) {
        var component: TextComponent = Component
            .text("Generating worlds for new game ")
            .append(Component.text(gameCode).color(NamedTextColor.BLUE))
            .append(Component.text("."))
        announceWithHeader(component)
        component = Component.text("This will cause the server to lag!")
        announceWithHeader(component)
    }

    fun bingoAnnounceGameFailed() {
        val component = Component
            .text("Failed to connect to the Bingo board.")
            .color(NamedTextColor.RED)
        announceWithHeader(component)
    }

    fun bingoAnnounceWorldsGenerated(players: Collection<BingoPlayer>) {
        val playersCpnt: TextComponent = ChatUtils.commaSeparated(
            players.map(BingoPlayer::formattedName))
        val component = Component
            .text("Bingo worlds have been generated for ")
            .append(
                Component.text("${players.size} player${if (players.size != 1) 's' else ""}.")
                    .hoverEvent(playersCpnt)
            )
        announceWithHeader(component)
    }

    fun bingoAnnounceGameReady(gameCode: String, players: Collection<BingoPlayer>, starters: Audience) {
        var component: TextComponent
        for (p in players) {
            // Game link for this specific player
            val url: Url = BingoConfig.gameUrl(gameCode, p)
            component = Component.empty()
                .append(
                    Component.text("[Open Board]")
                        .decoration(TextDecoration.UNDERLINED, true)
                        .color(NamedTextColor.YELLOW)
                        .hoverEvent(Component.text(url.toString()))
                        .clickEvent(ClickEvent.openUrl(URL(url.toString())))
                )
            for (bukkitPlayer in p.bukkitPlayers) {
                sendWithHeader(bukkitPlayer, component)
            }
        }
        component = Component.text("[START]")
            .decoration(TextDecoration.UNDERLINED, true)
            .color(NamedTextColor.GREEN)
            .clickEvent(ClickEvent.runCommand("/bingo start"))
            .hoverEvent(Component.text("Start Game (/bingo start)"))
        sendWithHeader(starters, component)
    }

    fun bingoAnnouncePlayerMarking(player: BingoPlayer, spaceText: String, invalidated: Boolean) {
        val component: TextComponent = if (!invalidated) {
            Component.empty()
                .append(player.formattedName)
                .append(Component.text(" has marked "))
                .append(Component.text(spaceText).color(NamedTextColor.GREEN))
                .append(Component.text("!"))
        } else {
            Component.empty()
                .append(player.formattedName)
                .append(Component.text(" has invalidated "))
                .append(Component.text(spaceText).color(NamedTextColor.RED))
                .append(Component.text("!"))
        }
        announceWithHeader(component)
    }

    fun bingoAnnounceEnd() {
        bingoAnnounce(Component.text("The game has ended!"))
    }

    fun bingoAnnouncePlayerVictory(player: BingoPlayer) {
        val component: TextComponent = Component.empty()
            .color(NamedTextColor.GOLD)
            .append(player.formattedName)
            .append(Component.text(" has won the game!"))
        announceWithHeader(component)
    }

    fun Audience.bingoTellNotReady() {
        bingoTell(Component.text("The game is not ready to be started!")
            .color(NamedTextColor.RED))
    }

    fun bingoTellTeams(players: Collection<BingoPlayer>) {
        for (bp in players) {
            if (bp is BingoPlayerTeam) {
                val component: Component = Component.text("You are playing on team ")
                    .append(bp.formattedName)
                    .append(Component.text("!"))
                Audience.audience(bp.bukkitPlayers).sendMessage(component)
            }
        }
    }
}
