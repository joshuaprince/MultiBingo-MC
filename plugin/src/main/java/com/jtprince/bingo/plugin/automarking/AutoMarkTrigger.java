package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.Space;
import com.jtprince.bingo.plugin.automarking.itemtrigger.ItemTrigger;
import com.jtprince.bingo.plugin.automarking.itemtrigger.SpecialItemTrigger;

import java.util.Collection;
import java.util.HashSet;

public abstract class AutoMarkTrigger {
    protected final Space space;

    protected AutoMarkTrigger(Space space) {
        this.space = space;
    }

    public static Collection<AutoMarkTrigger> createAllTriggers(Space space) {
        HashSet<AutoMarkTrigger> set = new HashSet<>();

        set.addAll(ItemTrigger.createTriggers(space));
        set.addAll(EventTrigger.createTriggers(space));
        set.addAll(OccasionalTrigger.createTriggers(space));
        set.addAll(SpecialItemTrigger.createTriggers(space));

        return set;
    }

    public Space getSpace() {
        return this.space;
    }

    public abstract void destroy();
}
