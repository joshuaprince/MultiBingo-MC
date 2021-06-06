package com.jtprince.bingo.bukkit.automark.trigger

abstract class AutoMarkTrigger {
    /**
     * Callback that is executed whenever a BingoPlayer performs an action that activates this
     * trigger.
     *
     * For some triggers, the player can undo their progress, which will result in this being called
     * with `fulfilled = false`.
     */
    abstract fun destroy()
}
