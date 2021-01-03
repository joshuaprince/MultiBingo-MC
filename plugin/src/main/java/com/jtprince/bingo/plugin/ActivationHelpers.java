package com.jtprince.bingo.plugin;

import org.bukkit.Location;
import org.bukkit.StructureType;

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
