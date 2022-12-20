package com.jtprince.multibingo.fabric

import com.jtprince.multibingo.core.game.BingoGameFactory
import com.jtprince.multibingo.core.game.DebugGame
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object FabricBingoCommands {
    fun register() {
        CommandRegistrationCallback.EVENT.register {
                dispatcher: CommandDispatcher<CommandSourceStack>,
                buildContext: CommandBuildContext,
                selection: Commands.CommandSelection ->
            dispatcher.register(
                Commands.literal("bingo")
                    .then(Commands.literal("debug")
                    .then(Commands.argument("goalId", StringArgumentType.string())
                    .executes {
                        commandSetDebugGoal(it.source.audience(), StringArgumentType.getString(it, "goalId"))
                        return@executes 1
                    }))
            )
        }
    }

    private fun commandSetDebugGoal(sender: Audience, goal: String) {
        val debugGame = BingoGameFactory.currentGame as? DebugGame ?: run {
            sender.sendMessage(Component.text("Not in a debug game."))
            return
        }

        debugGame.setGoal(goal)
        sender.sendMessage(Component.text("Set goal to $goal."))
    }
}
