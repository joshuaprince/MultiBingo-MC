package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.Space;
import org.json.simple.JSONArray;

import java.util.Collection;
import java.util.HashSet;

public abstract class AutoMarkTrigger {
    public static Collection<AutoMarkTrigger> createAllTriggers(Space space, JSONArray xmlTriggers) {
        HashSet<AutoMarkTrigger> set = new HashSet<>();

        set.addAll(ItemTrigger.createTriggers(space, xmlTriggers));
        set.addAll(EventTrigger.createTriggers(space));
        set.addAll(OccasionalTrigger.createTriggers(space));

        return set;
    }

    public abstract void destroy();
}
