@file:JvmName("AutomatedGoalList")

package com.jtprince.bingo.bukkit.automark.definitions

import com.jtprince.bingo.core.automark.TriggerDefinition
import java.io.File
import java.io.PrintWriter

/**
 * Standalone executable that lists all goals that are automated by the Bukkit plugin.
 *
 * If executed with an argument, goals will be written to the file specified by that argument.
 * Otherwise, goals will be printed to stdout.
 */
fun main(args: Array<String>) {
    val triggerDefinitionRegistry = TriggerDefinition.Registry()
    triggerDefinitionRegistry.registerAll(BukkitDslTriggers)
    triggerDefinitionRegistry.registerItemTriggers()

    val file = if (args.isNotEmpty()) File(args[0]).printWriter() else PrintWriter(System.out)

    file.use {
        for (goal in triggerDefinitionRegistry.registeredGoalIds.sorted()) {
            it.println(goal)
        }
    }
}
