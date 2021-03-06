package com.jtprince.bingo.plugin.automarking;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.StructureType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Miscellaneous static helper/utility functions to deduplicate logic in determining if a
 * Space should be marked.
 */
public class ActivationHelpers {
    public static boolean inVillage(Location location) {
        Location nearestVillage = location.getWorld().locateNearestStructure(
            location, StructureType.VILLAGE, 8, false);
        if (nearestVillage == null) {
            return false;
        }

        // locateNearestStructure returns Y=0. Only calculate horizontal distance
        nearestVillage.setY(location.getY());

        return location.distance(nearestVillage) < 100;
    }

    private static class MapCompletionRenderer extends MapRenderer {
        double completedPercent = 0;
        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            int totalPixels = 0;
            int mappedPixels = 0;
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    totalPixels++;
                    if (canvas.getBasePixel(x, y) != 0) {
                        mappedPixels++;
                    }
                }
            }

            completedPercent = ((double) mappedPixels) / totalPixels;
        }
    }

    /**
     * Returns whether the map item held in an inventory is fully explored, which is defined as
     * 99% or more of the pixels on the map being filled. NOTE: This function may incorrectly return
     * false the first time it is called on a new MapView. The map must be rendered for the player
     * so the pixels can be counted before it is accurate.
     * @param item Any item held by a player.
     * @param requiredScale If specified, only a map item that is at this zoom level will count as
     *                      completed.
     * @return True if the map is certified "completed", false if not or if the map has not been
     *         rendered yet.
     */
    public static boolean isCompletedMap(ItemStack item, @Nullable MapView.Scale requiredScale) {
        if (item == null || item.getType() != Material.FILLED_MAP) {
            return false;
        }

        MapMeta meta = (MapMeta) item.getItemMeta();
        if (meta == null) {
            return false;
        }

        MapView view = meta.getMapView();
        if (view == null) {
            return false;
        }

        if (requiredScale != null && view.getScale() != requiredScale) {
            return false;
        }

        if (view.getRenderers().stream().noneMatch(r -> r instanceof MapCompletionRenderer)) {
            view.addRenderer(new MapCompletionRenderer());
        }

        MapCompletionRenderer renderer =
            (MapCompletionRenderer) view.getRenderers().stream()
                .filter(r -> r instanceof MapCompletionRenderer).findFirst().orElseThrow();

        return renderer.completedPercent > 0.99;
    }
}
