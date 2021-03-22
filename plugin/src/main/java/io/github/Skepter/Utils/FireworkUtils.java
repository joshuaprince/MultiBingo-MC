/*
 * Written by JorelAli. Gist: https://gist.github.com/JorelAli/8e60a30ca133769c4ca1
 */

package io.github.Skepter.Utils;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class FireworkUtils {

	public static void spawnRandomFirework(final Location loc) {
		final Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
		final FireworkMeta fireworkMeta = firework.getFireworkMeta();
		final Random random = new Random();
		final FireworkEffect effect = FireworkEffect.builder()
			.flicker(random.nextBoolean())
			.withColor(getColor(random.nextInt(17) + 1))
			.withFade(getColor(random.nextInt(17) + 1))
			.with(Type.values()[random.nextInt(Type.values().length)])
			.trail(random.nextBoolean()).build();
		fireworkMeta.addEffect(effect);
		fireworkMeta.setPower(random.nextInt(4) + 1);
		firework.setFireworkMeta(fireworkMeta);
	}

	private static @NotNull Color getColor(final int i) {
		switch (i) {
		case 1:
			return Color.AQUA;
		case 2:
			return Color.BLACK;
		case 3:
			return Color.BLUE;
		case 4:
			return Color.FUCHSIA;
		case 5:
			return Color.GRAY;
		case 6:
			return Color.GREEN;
		case 7:
			return Color.LIME;
		case 8:
			return Color.MAROON;
		case 9:
			return Color.NAVY;
		case 10:
			return Color.OLIVE;
		case 11:
			return Color.ORANGE;
		case 12:
			return Color.PURPLE;
		case 13:
			return Color.RED;
		case 14:
			return Color.SILVER;
		case 15:
			return Color.TEAL;
		case 16:
			return Color.WHITE;
		case 17:
			return Color.YELLOW;
		}
		return Color.WHITE;
	}

	// joshuaprince's additions
    public static void spawnSeveralFireworks(JavaPlugin plugin, final Player location) {
        final int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
			Location loc = location.getLocation();
			loc.add(Vector.getRandom().multiply(16).subtract(new Vector(8, 8, 8)));
			while (loc.getBlock().getType() != Material.AIR && loc.getY() < 256) {
				loc.add(0, 1, 0);
			}
			spawnRandomFirework(loc);
		}, 0, 4);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
			plugin.getServer().getScheduler().cancelTask(taskId), 120);
    }
}
