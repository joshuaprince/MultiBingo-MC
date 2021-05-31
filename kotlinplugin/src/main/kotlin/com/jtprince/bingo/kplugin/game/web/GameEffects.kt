package com.jtprince.bingo.kplugin.game.web

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.player.LocalBingoPlayer
import com.jtprince.bukkit.worldset.WorldSet
import io.github.skepter.utils.FireworkUtils
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.Statistic
import org.bukkit.World
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Container for applying all the fancy effects when a game starts and ends - countdown sequence,
 * potion effects, teleportation to worlds, inventory wipe, victory fireworks, etc.
 */
class GameEffects(
    private val playerManager: PlayerManager,
    private val startSequenceDone: () -> Unit,
) {
    private val bukkitPlayers = playerManager.bukkitPlayers
    private var countdown = 0

    private var startSeqDoneTaskId: Int? = null
    private var countdownTaskId: Int? = null

    fun doStartEffects() {
        BingoPlugin.server.scheduler.scheduleSyncDelayedTask(BingoPlugin) {
            val length = 7 * 20

            wipe()
            applyPotionEffects(length)
            playerManager.playerWorldSetMap.forEach(::teleportToWorld)
            countdown = 7
            doCountdown()

            startSeqDoneTaskId = BingoPlugin.server.scheduler.scheduleSyncDelayedTask(
                BingoPlugin, { startSeqDoneTaskId = null ; startSequenceDone() }, length.toLong()
            )
        }
    }

    fun doEndEffects(winner: BingoPlayer?) {
        BingoPlugin.server.scheduler.scheduleSyncDelayedTask(BingoPlugin) {
            (winner as? LocalBingoPlayer)?.bukkitPlayers?.forEach {
                FireworkUtils.spawnSeveralFireworks(BingoPlugin, it)
            }
        }
    }

    fun destroy() {
        startSeqDoneTaskId?.also { BingoPlugin.server.scheduler.cancelTask(it) }
        countdownTaskId?.also { BingoPlugin.server.scheduler.cancelTask(it) }
    }

    private fun wipe() {
        for (p in bukkitPlayers) {
            p.health = 20.0
            p.foodLevel = 20
            p.saturation = 5.0f
            p.exhaustion = 0f
            p.exp = 0f
            p.level = 0
            p.setStatistic(Statistic.TIME_SINCE_REST, 0) // reset Phantom spawns
            p.gameMode = GameMode.SURVIVAL
            for (e in p.activePotionEffects) {
                p.removePotionEffect(e.type)
            }
        }

        val consoleSender = BingoPlugin.server.consoleSender
        BingoPlugin.server.dispatchCommand(consoleSender, "advancement revoke @a everything")
        BingoPlugin.server.dispatchCommand(consoleSender, "clear @a")
    }

    private fun teleportToWorld(player: LocalBingoPlayer, worldSet: WorldSet) {
        val world = worldSet.world(World.Environment.NORMAL)
        world.time = 0
        player.bukkitPlayers.forEach { p -> p.teleport(world.spawnLocation) }
    }

    private fun doCountdown() {
        if (countdown in 1..5) {
            for (p in bukkitPlayers) {
                p.sendTitle(this.countdown.toString() + "", null, 2, 16, 2)
                p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_BELL, 4.0f, 4.0f)
            }
        }
        this.countdown--
        if (this.countdown > 0) {
            countdownTaskId = BingoPlugin.server.scheduler.scheduleSyncDelayedTask(
                BingoPlugin, ::doCountdown, 20
            )
        }
    }

    private fun applyPotionEffects(@Suppress("SameParameterValue") ticks: Int) {
        for (p in bukkitPlayers) {
            p.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, ticks, 1))
            p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, ticks, 6))
            p.addPotionEffect(PotionEffect(PotionEffectType.JUMP, ticks, 128))
            p.addPotionEffect(PotionEffect(PotionEffectType.SLOW_DIGGING, ticks, 5))
        }
    }
}
