package com.jtprince.bingo.bukkit

import com.jtprince.bingo.bukkit.game.BingoGame
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayerTeam
import com.jtprince.bingo.core.player.BingoPlayer
import com.jtprince.util.ChatUtils
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URL

object BukkitMessages : KoinComponent {
    private val plugin: BingoPlugin by inject()

    private val COLOR_HEADER: TextColor = NamedTextColor.GOLD
    private val COLOR_TEXT: TextColor = NamedTextColor.AQUA

    private val HEADER = Component.text("[BINGO]")
        .color(COLOR_HEADER).decoration(TextDecoration.BOLD, true)

    private fun Component.withBingoHeader(): Component {
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

    private fun sendWithHeader(a: Audience, component: Component) {
        // TODO: Moved to Core Messages, but still in use here for game code access
        a.sendMessage(component.withBingoHeader())
    }

    private fun announceWithHeader(component: Component) {
        Bukkit.getServer().sendMessage(component.withBingoHeader())
    }

    /* Begin external facing calls */

    fun Audience.bingoTell(msg: String) {
        bingoTell(Component.text(msg))
    }

    fun Audience.bingoTellError(msg: String) {
        bingoTell(Component.text(msg).color(NamedTextColor.RED))
    }

    fun Audience.bingoTell(msg: Component) {
        sendWithHeader(this, msg)
    }

    fun bingoAnnounce(msg: String) {
        bingoAnnounce(Component.text(msg))
    }

    fun bingoAnnounce(msg: Component) {
        announceWithHeader(msg)
    }

    fun bingoAnnouncePreparingGame(gameCode: String) {
        val component = Component
            .text("Generating worlds for new game ")
            .append(Component.text(gameCode).color(NamedTextColor.BLUE))
            .append(Component.text("."))
        announceWithHeader(component)
        Bukkit.getServer().sendActionBar(
            Component.text("Generating Worlds - Server will lag!")
                .color(NamedTextColor.GOLD)
        )
    }

    fun bingoAnnounceGameFailed() {
        val component = Component
            .text("Failed to connect to the Bingo board.")
            .color(NamedTextColor.RED)
        announceWithHeader(component)
    }

    fun bingoAnnounceWorldsGenerated(players: Collection<BukkitBingoPlayer>) {
        val playersCpnt = ChatUtils.commaSeparated(
            players.map(BukkitBingoPlayer::formattedName))
        val component = Component
            .text("Bingo worlds have been generated for ")
            .append(
                Component.text("${players.size} player${if (players.size != 1) 's' else ""}.")
                    .hoverEvent(playersCpnt)
            )
        announceWithHeader(component)
    }

    fun bingoAnnounceGameReady(gameCode: String, players: Collection<BukkitBingoPlayer>, starters: Audience) {
        for (p in players) {
            // Game link for this specific player
            val url = plugin.bingoCore.urlFormatter.gameUrl(gameCode, p)
            val component = Component.empty()
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
        val component = Component.text("[START]")
            .decoration(TextDecoration.UNDERLINED, true)
            .color(NamedTextColor.GREEN)
            .clickEvent(ClickEvent.runCommand("/bingo start"))
            .hoverEvent(Component.text("Start Game (/bingo start)"))
        sendWithHeader(starters, component)
    }

    fun bingoAnnouncePlayerMarking(player: BingoPlayer, spaceText: String, invalidated: Boolean) {
        val component = if (!invalidated) {
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

    fun bingoAnnounceEnd(winner: BingoPlayer?) {
        bingoAnnounce(Component.text("The game has ended!"))
        winner?.also {
            val component = Component.empty()
                .color(NamedTextColor.GOLD)
                .append(it.formattedName)
                .append(Component.text(" is the winner!"))
            announceWithHeader(component)
        }

        bingoAnnounce(Component.empty()
            .append(
                Component.text("[Spectate World]")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand("/bingo spectate"))
                    .hoverEvent(HoverEvent.showText(Component.text(
                        "Click to enter Spectator mode. Click again to go back to Survival."
                    )))
            )
            .append(Component.text(" "))
            .append(
                Component.text("[Go to Spawn]")
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand("/bingo go spawn"))
                    .hoverEvent(HoverEvent.showText(Component.text(
                        "Click to return to the server's spawn world."
                    )))
            )
        )
    }

    fun Audience.bingoTellNotReady() {
        bingoTellError("The game is not ready to be started!")
    }

    fun bingoTellTeams(players: Collection<BukkitBingoPlayer>) {
        for (bp in players) {
            if (bp is BukkitBingoPlayerTeam) {
                val component = Component.text("You are playing on team ")
                    .append(bp.formattedName.decorate(TextDecoration.BOLD))
                    .append(Component.text("!"))
                bp.bingoTell(component)
            }
        }
    }
}
