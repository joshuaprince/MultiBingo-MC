package com.jtprince.bingo.bukkit

import com.jtprince.bingo.bukkit.BukkitMessages.bingoTellError
import com.jtprince.bingo.bukkit.game.GameManager
import com.jtprince.bingo.bukkit.game.web.WebBackedGame
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bingo.core.automark.MissingVariableException
import com.jtprince.bingo.core.game.StatefulGame
import com.jtprince.bingo.core.webclient.model.WebGameSettings
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.MultiLiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

object Commands : KoinComponent {
    private val plugin: BingoPlugin by inject()

    fun registerCommands() {
        val shapeArg = MultiLiteralArgument("square", "hexagon")
        val forcedArg = GreedyStringArgument("forcedGoals")

        // All-default form
        val prepareCmd = CommandAPICommand("prepare")
            .withAliases("p")
            .executes(CommandExecutor { sender: CommandSender, _: Array<Any> ->
                commandPrepare(sender, WebGameSettings())
            })
        // Game code form
        val prepareCmdGameCode = CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(StringArgument("gameCode"))
            .executes(CommandExecutor { sender: CommandSender, args: Array<Any> ->
                val settings = WebGameSettings(gameCode = args[0] as String)
                commandPrepare(sender, settings)
            })
        // Shape-only form
        val prepareCmdShaped = CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(shapeArg)
            .executes(CommandExecutor { sender: CommandSender, args: Array<Any> ->
                val settings = WebGameSettings(shape = args[0] as String)
                commandPrepare(sender, settings)
            })
        // +Forced goals
        val prepareCmdShapedDiffForced = CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(shapeArg, forcedArg)
            .executes(CommandExecutor { sender: CommandSender, args: Array<Any> ->
                val settings = WebGameSettings(
                    shape = args[0] as String,
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

        val retryCmd = CommandAPICommand("retry")
            .executes(CommandExecutor { sender: CommandSender, _: Array<Any> ->
                commandRetry(sender)
            })

        val spectateCmd = CommandAPICommand("spectate")
            .executesPlayer(PlayerCommandExecutor { sender: Player, _: Array<Any> ->
                commandSpectate(sender)
            })

        val goSpawnCmd = CommandAPICommand("go")
            .withArguments(LiteralArgument("spawn"))
            .executesPlayer(PlayerCommandExecutor { sender: Player, _: Array<Any> ->
                commandGo(sender,null)
            })
        /*val goCmd = CommandAPICommand("go")
            .withArguments(CustomArgument("player") { input: String ->
                val currentGame = BingoGame.currentGame as? WebBackedGame
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
                val currentGame = BingoGame.currentGame as? WebBackedGame
                    ?: return@overrideSuggestions emptyArray()
                return@overrideSuggestions currentGame.playerManager.localPlayers.
                    map(BukkitBingoPlayer::slugName).toTypedArray()
            })
            .executesPlayer(PlayerCommandExecutor { sender: Player, args: Array<Any> ->
                commandGo(sender, args[0] as BukkitBingoPlayer)
            })*/

        val goalIdsArg = StringArgument("goalId")
        goalIdsArg.replaceSuggestions {
            plugin.platform.triggerDefinitionRegistry.registeredGoalIds.toTypedArray()
        }
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
        root.withSubcommand(retryCmd)
        root.withSubcommand(spectateCmd)
        //root.withSubcommand(goCmd)  TODO
        root.withSubcommand(goSpawnCmd)
        if (plugin.platform.config.debug) {
            root.withSubcommand(debugCmd)
            root.withSubcommand(debugCmdVars)
        }
        root.register()
    }

    private fun commandPrepare(sender: CommandSender, settings: WebGameSettings) {
        GameManager.prepareNewWebGame(sender, settings)
    }

    private fun commandStart(sender: CommandSender) {
        val game = GameManager.currentGame ?: run {
            sender.bingoTellError("No game is prepared! Use /bingo prepare")
            return
        }

        if (game !is StatefulGame) {
            sender.bingoTellError("${game.name} is not a type of game that can be started.")
            return
        }

        game.signalStart(sender)
    }

    private fun commandEnd(sender: CommandSender) {
        val game = GameManager.currentGame ?: run {
            sender.bingoTellError("No game is running!")
            return
        }

        if (game !is StatefulGame) {
            sender.bingoTellError("${game.name} is not a type of game that can be ended.")
            return
        }

        game.signalEnd(sender)
    }

    private fun commandDestroy(sender: CommandSender) {
        /* Must call the Manager function directly so currentGame can be set to null */
        GameManager.destroyCurrentGame(sender, true)
    }

    private fun commandRetry(sender: CommandSender) {
        if (GameManager.currentGame !is WebBackedGame) {
            sender.bingoTellError("No game to retry connecting to!")
            return
        }
        (GameManager.currentGame as WebBackedGame).signalRetry(sender)
    }

    private fun commandSpectate(sender: Player) {
        val game = GameManager.currentGame
        if (game !is StatefulGame || !game.canSpectate()) {
            sender.bingoTellError("You can only spectate your world after a game is over.")
            return
        }

        if (sender.gameMode == GameMode.SPECTATOR) {
            sender.gameMode = GameMode.SURVIVAL
        } else {
            sender.gameMode = GameMode.SPECTATOR
        }
    }

    private fun commandGo(sender: Player, destination: BukkitBingoPlayer?) {
        if (destination == null) {
            sender.teleport(Bukkit.getWorlds()[0].spawnLocation)
        } else {
            val game = GameManager.currentGame as? WebBackedGame?
            val loc = game?.playerManager?.worldSet(destination)?.world(World.Environment.NORMAL)?.spawnLocation

            if (loc == null) {
                sender.bingoTellError("Couldn't send you to that player's world.")
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

        val goalId = args[0]

        try {
            GameManager.prepareDebugGame(sender, goalId, vars.toMap())
        } catch (ex: MissingVariableException) {
            val randomNum = Random.nextInt(10) + 1
            sender.bingoTellError(
                "Goal $goalId requires variable \"${ex.varname}\" to be set. " +
                        "Try /bingo debug $goalId ${ex.varname} $randomNum"
            )
            return
        }
    }
}
