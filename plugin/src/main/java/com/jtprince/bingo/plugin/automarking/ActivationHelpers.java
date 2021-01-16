package com.jtprince.bingo.plugin.automarking;

import org.bukkit.Location;
import org.bukkit.StructureType;

/**
 * Miscellaneous static helper/utility functions to deduplicate logic in determining if a
 * Square should be marked.
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
}
