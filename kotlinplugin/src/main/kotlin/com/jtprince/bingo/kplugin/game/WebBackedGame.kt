package com.jtprince.bingo.kplugin.game

import com.jtprince.bingo.kplugin.BingoConfig
import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.Messages
import com.jtprince.bingo.kplugin.Messages.bingoTell
import com.jtprince.bingo.kplugin.Messages.bingoTellNotReady
import com.jtprince.bingo.kplugin.board.Space
import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.webclient.WebBackedWebsocketClient
import com.jtprince.bingo.kplugin.webclient.WebsocketRxMessage
import com.jtprince.bingo.kplugin.webclient.model.WebModelBoard
import com.jtprince.bingo.kplugin.webclient.model.WebModelPlayerBoard
import org.bukkit.command.CommandSender

class WebBackedGame(
    creator: CommandSender,
    gameCode: String,
    players: Collection<BingoPlayer>
) : BingoGame(creator, gameCode, players) {

    override var state: State = State.WAITING_FOR_WEBSOCKET
    private val websocketClient = WebBackedWebsocketClient(
        gameCode, BingoConfig.websocketUrl(gameCode, players), this::receiveMessage,
        this::receiveFailedConnection)
    private val playerBoardCache = players.associateWith(::PlayerBoardCache)

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
            }
        }
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
            sender?.bingoTell("The game is not running!")
            return
        }

        websocketClient.sendEndGame()
    }

    override fun signalDestroy(sender: CommandSender?) {
        // Spaces are destroyed in the superclass.
        sender?.bingoTell("Game destroyed.")
        websocketClient.destroy()
        startEffects.destroy()
    }

    override fun receiveAutomark(bingoPlayer: BingoPlayer, spaceId: Int, satisfied: Boolean) {
        if (state != State.RUNNING) return

        /* Not filtered - first must filter to ensure no excessive backend requests. */
        val cache = playerBoardCache[bingoPlayer] ?: return
        val goalType = spaces[spaceId]?.goalType ?: run {
            BingoPlugin.logger.severe("Want to mark space $spaceId, but can't determine goal type")
            return
        }
        val newMarking = cache.canSendMarking(spaceId, goalType, satisfied) ?: return

        websocketClient.sendMarkSpace(bingoPlayer.name, spaceId, newMarking.value)
    }

    private fun receiveMessage(msg: WebsocketRxMessage) {
        msg.board?.run(this::receiveBoard)
        msg.pboards?.run(this::receivePlayerBoards)
        msg.gameState?.run(this::receiveGameStateTransition)
    }

    private fun receiveFailedConnection() {
        state = State.FAILED
        Messages.bingoAnnounceGameFailed()
        // TODO: `/bingo retry` command?
    }

    private fun receiveBoard(board: WebModelBoard) {
        if (spaces.isNotEmpty()) {
            BingoPlugin.logger.warning("Received another board, ignoring it.")  // TODO
            return
        }

        for (webSpace in board.spaces) {
            val newSpace = Space(webSpace.spaceId, webSpace.goalId,
                Space.GoalType.ofString(webSpace.goalType), webSpace.text, webSpace.variables)
            spaces[newSpace.spaceId] = newSpace
            newSpace.startListening(playerManager, this::receiveAutomark)
        }

        val autoSpaces = spaces.values.filter(Space::automarking)
        BingoPlugin.logger.info("Automated goals: " +
                autoSpaces.map(Space::goalId).joinToString(", "))

        val autoMarkMap = HashMap<String, List<Int>>()
        for (player in playerManager.localPlayers) {
            autoMarkMap[player.name] = autoSpaces.map(Space::spaceId)
        }
        websocketClient.sendAutoMarks(autoMarkMap)
    }

    private fun receivePlayerBoards(playerBoards: List<WebModelPlayerBoard>) {
        for (pb in playerBoards) {
            // TODO Do we even need remote players any more?
            val player = playerManager.bingoPlayer(pb.playerName, false) ?: continue
            playerBoardCache[player]?.updateFromWeb(pb)
            if (pb.win && winner == null) {
                winner = player
                // TODO The backend should send the "game end" state transition.
                receiveGameStateTransition("end")
            }
        }
    }

    private fun receiveGameStateTransition(newGameState: String) {
        when (newGameState) {
            "start" -> {
                if (state != State.READY) {
                    BingoPlugin.logger.warning(
                        "Web backend sent a Start Game message when game is not ready. Ignoring."
                    )
                    return
                }

                state = State.COUNTING_DOWN
                startEffects.doStartEffects()
            }
            "end" -> {
                if (state != State.RUNNING && state != State.COUNTING_DOWN) {
                    BingoPlugin.logger.warning(
                        "Web backend sent an End Game message when game is not running. Ignoring."
                    )
                    return
                }

                state = State.DONE
                Messages.bingoAnnounceEnd()
                startEffects.doEndEffects(winner)
            }
            else -> {
                BingoPlugin.logger.severe(
                    "Web backend sent an unrecognized game state transition: $newGameState")
            }
        }
    }
}
