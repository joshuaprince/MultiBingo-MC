/*
 * Written by JorelAli, converted to Kotlin by Intellij's converter.
 * Gist: https://gist.github.com/JorelAli/8e60a30ca133769c4ca1
 */
package io.github.skepter.utils

import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.util.*

object FireworkUtils {
    fun spawnRandomFirework(loc: Location) {
        val firework = loc.world.spawnEntity(loc, EntityType.FIREWORK) as Firework
        val fireworkMeta = firework.fireworkMeta
        val random = Random()
        val effect = FireworkEffect.builder()
            .flicker(random.nextBoolean())
            .withColor(getColor(random.nextInt(17) + 1))
            .withFade(getColor(random.nextInt(17) + 1))
            .with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().size)])
            .trail(random.nextBoolean()).build()
        fireworkMeta.addEffect(effect)
        fireworkMeta.power = random.nextInt(4) + 1
        firework.fireworkMeta = fireworkMeta
    }

    private fun getColor(i: Int): Color {
        return when (i) {
            1 -> Color.AQUA
            2 -> Color.BLACK
            3 -> Color.BLUE
            4 -> Color.FUCHSIA
            5 -> Color.GRAY
            6 -> Color.GREEN
            7 -> Color.LIME
            8 -> Color.MAROON
            9 -> Color.NAVY
            10 -> Color.OLIVE
            11 -> Color.ORANGE
            12 -> Color.PURPLE
            13 -> Color.RED
            14 -> Color.SILVER
            15 -> Color.TEAL
            16 -> Color.YELLOW
            else -> Color.WHITE
        }
    }

    // joshuaprince's additions
    fun spawnSeveralFireworks(plugin: JavaPlugin, location: Player) {
        val taskId = plugin.server.scheduler.scheduleSyncRepeatingTask(plugin, {
            val loc = location.location
            loc.add(Vector.getRandom().multiply(16).subtract(Vector(8, 8, 8)))
            while (loc.block.type != Material.AIR && loc.y < 256) {
                loc.add(0.0, 1.0, 0.0)
            }
            spawnRandomFirework(loc)
        }, 0, 4)
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin,
            { plugin.server.scheduler.cancelTask(taskId) }, 120
        )
    }
}
