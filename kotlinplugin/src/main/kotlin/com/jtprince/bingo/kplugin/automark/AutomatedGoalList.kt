@file:JvmName("AutomatedGoalList")

package com.jtprince.bingo.kplugin.automark

import java.io.File
import java.io.PrintWriter

/**
 * Standalone executable that lists all goals that are automated by the plugin.
 *
 * If executed with an argument, goals will be written to the file specified by that argument.
 * Otherwise, goals will be printed to stdout.
 */
fun main(args: Array<String>) {
    val allGoals: Set<String> = AutoMarkTrigger.allAutomatedGoals
    val file = if (args.isNotEmpty()) File(args[0]).printWriter() else PrintWriter(System.out)

    file.use {
        for (goal in allGoals) {
            it.println(goal)
        }
    }
}
