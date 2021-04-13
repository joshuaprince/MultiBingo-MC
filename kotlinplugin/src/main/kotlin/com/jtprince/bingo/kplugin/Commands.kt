package com.jtprince.bingo.kplugin

import com.jtprince.bingo.kplugin.Messages.bingoTell
import com.jtprince.bingo.kplugin.automark.AutoMarkTrigger
import com.jtprince.bingo.kplugin.game.BingoGame
import com.jtprince.bingo.kplugin.game.WebBackedGameProto
import com.jtprince.bingo.kplugin.player.BingoPlayer
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException
import dev.jorel.commandapi.arguments.CustomArgument.MessageBuilder
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object Commands {
    fun registerCommands() {
        val shapeArg = MultiLiteralArgument("square", "hexagon")
        val forcedArg = GreedyStringArgument("forcedGoals")

        // All-default form
        val prepareCmd = CommandAPICommand("prepare")
            .withAliases("p")
            .executes(CommandExecutor { sender: CommandSender, _: Array<Any> ->
                commandPrepare(sender, WebBackedGameProto.WebGameSettings())
            })
        // Game code form
        val prepareCmdGameCode = CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(StringArgument("gameCode"))
            .executes(CommandExecutor { sender: CommandSender, args: Array<Any> ->
                val settings = WebBackedGameProto.WebGameSettings(gameCode = args[0] as String)
                commandPrepare(sender, settings)
            })
        // Shape-only form
        val prepareCmdShaped = CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(shapeArg)
            .executes(CommandExecutor { sender: CommandSender, args: Array<Any> ->
                val settings = WebBackedGameProto.WebGameSettings(shape = args[0] as String)
                commandPrepare(sender, settings)
            })
        // +Forced goals
        val prepareCmdShapedDiffForced = CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(shapeArg, forcedArg)
            .executes(CommandExecutor { sender: CommandSender, args: Array<Any> ->
                val settings = WebBackedGameProto.WebGameSettings(
                    gameCode = args[0] as String,
                    forcedGoals = (args[1] as String).split(" "))
                commandPrepare(sender, settings)
            })

        val startCmd = CommandAPICommand("start")
            .withAliases("s")
            .executes(CommandExecutor { sender: CommandSender, _: Array<Any> ->
                commandStart(sender)
            })

        val endCmd = CommandAPICommand("end")
            .executes(CommandExecutor { sender: CommandSender, _: Array<Any> ->
                commandEnd(sender)
            })

        val destroyCmd = CommandAPICommand("destroy")
            .executes(CommandExecutor { sender: CommandSender, _: Array<Any> ->
                commandDestroy(sender)
            })

        val goSpawnCmd = CommandAPICommand("go")
            .withArguments(LiteralArgument("spawn"))
            .executesPlayer(PlayerCommandExecutor { sender: Player, _: Array<Any> ->
                commandGo(sender,null)
            })
        val goCmd = CommandAPICommand("go")
            .withArguments(CustomArgument("player") { input: String ->
                val currentGame = BingoGame.currentGame
                    ?: throw CustomArgumentException(
                        MessageBuilder("No games running.")
                    )
                val player = currentGame.playerManager.bingoPlayer(input, false)
                if (player == null) {
                    throw CustomArgumentException(
                        MessageBuilder("Unknown BingoPlayer: ").appendArgInput()
                    )
                } else {
                    return@CustomArgument player
                }
            }.overrideSuggestions { _ ->
                val currentGame = BingoGame.currentGame
                return@overrideSuggestions currentGame?.playerManager?.localPlayers?.
                    map(BingoPlayer::slugName)?.toTypedArray() ?: arrayOf()
            })
            .executesPlayer(PlayerCommandExecutor { sender: Player, args: Array<Any> ->
                commandGo(sender, args[0] as BingoPlayer)
            })

        val goalIdsArg = StringArgument("goalId")
            .overrideSuggestions(AutoMarkTrigger.allAutomatedGoals)
        val debugCmd = CommandAPICommand("debug")
            .withArguments(goalIdsArg)
            .executesPlayer(PlayerCommandExecutor { sender: Player, args: Array<Any> ->
                commandDebug(sender, args.map{a -> a as String}.toTypedArray())
            })
        val debugCmdVars = CommandAPICommand("debug")
            .withArguments(goalIdsArg, GreedyStringArgument("variables"))
            .executesPlayer(PlayerCommandExecutor { sender: Player, args: Array<Any> ->
                commandDebug(sender, args.map{a -> a as String}.toTypedArray())
            })

        val root = CommandAPICommand("bingo")
        root.withSubcommand(prepareCmd)
        root.withSubcommand(prepareCmdGameCode)
        root.withSubcommand(prepareCmdShaped)
        root.withSubcommand(prepareCmdShapedDiffForced)
        root.withSubcommand(startCmd)
        root.withSubcommand(endCmd)
        root.withSubcommand(destroyCmd)
        root.withSubcommand(goCmd)
        root.withSubcommand(goSpawnCmd)
        if (BingoConfig.debug) {
            root.withSubcommand(debugCmd)
            root.withSubcommand(debugCmdVars)
        }
        root.register()
    }

    private fun commandPrepare(sender: CommandSender, settings: WebBackedGameProto.WebGameSettings) {
        BingoGame.prepareNewWebGame(sender, settings)
    }

    private fun commandStart(sender: CommandSender) {
        val game = BingoGame.currentGame ?: run {
            sender.bingoTell("No game is prepared! Use /bingo prepare")
            return
        }

        game.signalStart(sender)
    }

    private fun commandEnd(sender: CommandSender) {
        val game = BingoGame.currentGame ?: run {
            sender.bingoTell("No game is running!")
            return
        }

        game.signalEnd(sender)
    }

    private fun commandDestroy(sender: CommandSender) {
        /* Must call the Manager function directly so currentGame can be set to null */
        BingoGame.destroyCurrentGame(sender)
    }

    private fun commandGo(sender: Player, destination: BingoPlayer?) {
        if (destination == null) {
            sender.teleport(WorldManager.spawnWorld.spawnLocation)
        } else {
            val loc = BingoGame.currentGame?.playerManager?.worldSet(destination)?.
                    world(World.Environment.NORMAL)?.spawnLocation

            if (loc == null) {
                sender.bingoTell("Couldn't send you to that player's world.")
            } else {
                sender.teleport(loc)
            }
        }
    }

    private fun commandDebug(sender: Player, args: Array<String>) {
        val vars = HashMap<String, Int>()

        // TODO: Document this pattern
        val varArgs = if (args.size > 1) args[1].split(" ") else emptyList()
        for (i in 0 until varArgs.size-1 step 2) {
            vars[varArgs[i]] = varArgs[i+1].toInt()
        }

        BingoGame.prepareDebugGame(sender, args[0], vars.toMap())
    }
}
