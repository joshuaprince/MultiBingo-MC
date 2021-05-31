package com.jtprince.bingo.kplugin.game.web

import com.jtprince.bingo.kplugin.BingoConfig
import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.Messages
import com.jtprince.bingo.kplugin.Messages.bingoTell
import com.jtprince.bingo.kplugin.Messages.bingoTellError
import com.jtprince.bingo.kplugin.Messages.bingoTellNotReady
import com.jtprince.bingo.kplugin.PluginParity
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.game.BingoGame
import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.player.LocalBingoPlayer
import com.jtprince.bingo.kplugin.webclient.WebBackedWebsocketClient
import com.jtprince.bingo.kplugin.webclient.WebMessageRelay
import com.jtprince.bingo.kplugin.webclient.WebsocketRxMessage
import com.jtprince.bingo.kplugin.webclient.model.WebModelBoard
import com.jtprince.bingo.kplugin.webclient.model.WebModelGameState
import com.jtprince.bingo.kplugin.webclient.model.WebModelPlayerBoard
import org.bukkit.World
import org.bukkit.command.CommandSender

class WebBackedGame(
    creator: CommandSender,
    gameCode: String,
    localPlayers: Collection<LocalBingoPlayer>
) : BingoGame(creator, gameCode) {

    override var state: State = State.WAITING_FOR_WEBSOCKET
    private val clientId = "KotlinPlugin${hashCode() % 10000}:" +
            localPlayers.map(BingoPlayer::name).joinToString(",")
    private val websocketClient = WebBackedWebsocketClient(
        gameCode, clientId, this::receiveWebsocketMessage,
        this::receiveFailedConnection
    )
    private val messageRelay = WebMessageRelay(websocketClient)
    private var pluginParity: PluginParity? = null

    private val spaces = mutableMapOf<Int, WebBackedSpace>()
    val playerManager = PlayerManager(localPlayers)
    private val playerBoardCache = localPlayers.associateWith(::PlayerBoardCache)

    /* Both of the following must be ready for the game to be put in the "READY" state */
    private var websocketReady = false
    private var worldsReady = false

    private val startEffects = GameEffects(playerManager) {
        state = State.RUNNING
        websocketClient.sendRevealBoard()
    }

    private var winner: BingoPlayer? = null

    init {
        websocketClient.connect {
            websocketReady = true
            tryToMoveToReady()
        }
        generateWorlds()
    }

    private fun generateWorlds() {
        BingoPlugin.server.scheduler.runTask(BingoPlugin) { ->
            Messages.bingoAnnouncePreparingGame(gameCode)
            val players = playerManager.localPlayers
            Messages.bingoTellTeams(players)
            for (p in players) {
                playerManager.prepareWorldSet(gameCode, p)
                /* Allow for early destruction. */
                if (state == State.DESTROYING) return@runTask
            }

            BingoPlugin.logger.info("Finished generating " + players.size + " worlds")
            Messages.bingoAnnounceWorldsGenerated(players)
            worldsReady = true
            tryToMoveToReady()
        }
    }

    private fun tryToMoveToReady() {
        when {
            !websocketReady -> state = State.WAITING_FOR_WEBSOCKET
            !worldsReady -> state = State.WORLDS_GENERATING
            else -> {
                state = State.READY
                Messages.bingoAnnounceGameReady(gameCode, playerManager.localPlayers, creator)
                startPluginParity()
            }
        }
    }

    private fun startPluginParity() {
        val anyPlayerWorld = playerManager.localPlayers.firstOrNull()?.let {
            playerManager.worldSet(it).world(World.Environment.NORMAL)
        } ?: return

        val newPluginParity = PluginParity(gameCode, anyPlayerWorld, sendEcho = { settings ->
            websocketClient.sendPluginParity(true, settings)
        })

        if (BingoConfig.debug) {
            for (setting in newPluginParity.getMySettings()) {
                BingoPlugin.logger.info("[Parity] ${setting.key} = ${setting.value}")
            }
        }

        websocketClient.sendPluginParity(false, newPluginParity.getMySettings())

        pluginParity = newPluginParity
    }

    override fun signalStart(sender: CommandSender?) {
        if (state != State.READY) {
            sender?.bingoTellNotReady()
            return
        }

        websocketClient.sendStartGame()
    }

    override fun signalEnd(sender: CommandSender?) {
        if (state <= State.READY) {
            sender?.bingoTellError("The game is not running!")
            return
        }

        websocketClient.sendEndGame()
    }

    override fun signalDestroy(sender: CommandSender?) {
        for (space in spaces.values) {
            space.destroy()
        }
        playerManager.destroy()
        messageRelay.destroy()
        websocketClient.destroy()
        startEffects.destroy()
        sender?.bingoTell("Game destroyed.")
    }

    override fun receiveAutoMark(player: LocalBingoPlayer, space: AutomatedSpace, fulfilled: Boolean) {
        if (state != State.RUNNING) return

        if (space !is WebBackedSpace) {
            BingoPlugin.logger.severe(
                "Got auto-mark for space of type ${space::class}, " +
                        "which is not of expected type WebBackedSpace."
            )
            return
        }

        /* This callback is not filtered, and may be called after every time the player completes
         * the goal. We should filter to ensure no excessive backend requests. */
        val cache = playerBoardCache[player] ?: return
        val newMarking = cache.canSendMarking(space.spaceId, space.goalType, fulfilled) ?: return

        websocketClient.sendMarkSpace(player.name, space.spaceId, newMarking.value)
    }

    private fun receiveWebsocketMessage(msg: WebsocketRxMessage) {
        msg.board?.run(this::receiveBoard)
        msg.pboards?.run(this::receivePlayerBoards)
        msg.gameState?.run(this::receiveGameState)
        msg.messageRelay?.run(messageRelay::receive)
        pluginParity?.let { msg.pluginParity?.run(it::receiveSettings) }
    }

    private fun receiveFailedConnection() {
        state = State.FAILED
        Messages.bingoAnnounceGameFailed()
        // TODO: `/bingo retry` command?
    }

    private fun receiveBoard(board: WebModelBoard) {
        if (spaces.isNotEmpty()) {
            BingoPlugin.logger.fine("Received another board, ignoring it.")  // TODO
            return
        }

        for (webModelSpace in board.spaces) {
            val newSpace = WebBackedSpace(webModelSpace, playerManager, this)
            spaces[newSpace.spaceId] = newSpace
        }

        val autoSpaces = spaces.values.filter(WebBackedSpace::hasAutoMarkTrigger)
        BingoPlugin.logger.info("Automated goals: " +
                autoSpaces.map(WebBackedSpace::goalId).sorted().joinToString(", "))

        /* Tell webserver that we are marking these spaces for all local players */
        val autoSpaceIds = autoSpaces.map { s -> s.spaceId }
        websocketClient.sendAutoMarks(
            playerManager.localPlayers.map(LocalBingoPlayer::name).associateWith { autoSpaceIds }
        )
    }

    private fun receivePlayerBoards(playerBoards: List<WebModelPlayerBoard>) {
        for (pb in playerBoards) {
            val player = playerManager.bingoPlayer(pb.playerName) ?: continue
            playerBoardCache[player]?.updateFromWeb(pb)
        }
    }

    private fun receiveGameState(msg: WebModelGameState) {
        return when (msg) {
            is WebModelGameState.Start -> {
                if (state != State.READY) {
                    BingoPlugin.logger.warning(
                        "Web backend sent a Start Game message when game is not ready. Ignoring."
                    )
                    return
                }

                state = State.COUNTING_DOWN
                startEffects.doStartEffects()
            }
            is WebModelGameState.Marking -> {
                val player = playerManager.bingoPlayerOrCreateRemote(msg.player)
                val invalidate = when (msg.markingType) {
                    "complete" -> false
                    "invalidate" -> true
                    else -> run {
                        BingoPlugin.logger.severe("Unknown game_state marking_type ${msg.markingType}")
                        return
                    }
                }
                Messages.bingoAnnouncePlayerMarking(player, msg.goalText, invalidate)
            }
            is WebModelGameState.End -> {
                if (state != State.RUNNING && state != State.COUNTING_DOWN) {
                    BingoPlugin.logger.warning(
                        "Web backend sent an End Game message when game is not running. Ignoring."
                    )
                    return
                }

                state = State.DONE

                val winner = msg.winner?.let { playerManager.bingoPlayer(it) }
                Messages.bingoAnnounceEnd(winner)
                startEffects.doEndEffects(winner)
            }
        }
    }
}
