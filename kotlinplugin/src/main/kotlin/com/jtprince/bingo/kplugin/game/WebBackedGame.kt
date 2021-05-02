package com.jtprince.bingo.kplugin.game

import com.jtprince.bingo.kplugin.BingoPlugin
import com.jtprince.bingo.kplugin.Messages
import com.jtprince.bingo.kplugin.Messages.bingoTell
import com.jtprince.bingo.kplugin.Messages.bingoTellError
import com.jtprince.bingo.kplugin.Messages.bingoTellNotReady
import com.jtprince.bingo.kplugin.automark.AutomatedSpace
import com.jtprince.bingo.kplugin.board.Space
import com.jtprince.bingo.kplugin.player.BingoPlayer
import com.jtprince.bingo.kplugin.webclient.WebBackedWebsocketClient
import com.jtprince.bingo.kplugin.webclient.WebMessageRelay
import com.jtprince.bingo.kplugin.webclient.WebsocketRxMessage
import com.jtprince.bingo.kplugin.webclient.model.WebModelBoard
import com.jtprince.bingo.kplugin.webclient.model.WebModelGameState
import com.jtprince.bingo.kplugin.webclient.model.WebModelPlayerBoard
import org.bukkit.command.CommandSender

class WebBackedGame(
    creator: CommandSender,
    gameCode: String,
    players: Collection<BingoPlayer>
) : BingoGame(creator, gameCode, players) {

    override var state: State = State.WAITING_FOR_WEBSOCKET
    private val clientId = "KotlinPlugin${hashCode() % 10000}:" +
            players.map(BingoPlayer::slugName).joinToString(",")
    private val websocketClient = WebBackedWebsocketClient(
        gameCode, clientId, this::receiveMessage,
        this::receiveFailedConnection
    )
    private val messageRelay = WebMessageRelay(websocketClient)
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
            sender?.bingoTellError("The game is not running!")
            return
        }

        websocketClient.sendEndGame()
    }

    override fun signalDestroy(sender: CommandSender?) {
        // Spaces are destroyed in the superclass.
        sender?.bingoTell("Game destroyed.")
        messageRelay.destroy()
        websocketClient.destroy()
        startEffects.destroy()
    }

    override fun receiveAutomark(bingoPlayer: BingoPlayer, space: AutomatedSpace, satisfied: Boolean) {
        if (state != State.RUNNING) return

        if (space !is Space) {
            BingoPlugin.logger.severe(
                "Got automark for space of type ${space::class}, which is not of expected Space type.")
            return
        }

        /* Not filtered - first must filter to ensure no excessive backend requests. */
        val cache = playerBoardCache[bingoPlayer] ?: return
        val newMarking = cache.canSendMarking(space.spaceId, space.goalType, satisfied) ?: return

        websocketClient.sendMarkSpace(bingoPlayer.name, space.spaceId, newMarking.value)
    }

    private fun receiveMessage(msg: WebsocketRxMessage) {
        msg.board?.run(this::receiveBoard)
        msg.pboards?.run(this::receivePlayerBoards)
        msg.gameState?.run(this::receiveGameState)
        msg.messageRelay?.run(messageRelay::receive)
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
            val player = playerManager.bingoPlayer(pb.playerName, false) ?: continue
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
                val player = playerManager.bingoPlayer(msg.player, true)!!
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

                val winner = msg.winner?.let { playerManager.bingoPlayer(it, false) }
                Messages.bingoAnnounceEnd(winner)
                startEffects.doEndEffects(winner)
            }
        }
    }
}
