package com.jtprince.bingo.bukkit.game.web

import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.BukkitMessages
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTell
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTellError
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTellNotReady
import com.jtprince.bingo.bukkit.PluginParity
import com.jtprince.bingo.bukkit.WebMessageRelay
import com.jtprince.bingo.bukkit.automark.trigger.BukkitAutoMarkTriggerFactory
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bingo.core.automark.AutoMarkConsumer
import com.jtprince.bingo.core.game.BingoGame
import com.jtprince.bingo.core.player.BingoPlayer
import com.jtprince.bingo.core.webclient.WebBackedWebsocketClient
import com.jtprince.bingo.core.webclient.WebsocketRxMessage
import com.jtprince.bingo.core.webclient.model.WebModelBoard
import com.jtprince.bingo.core.webclient.model.WebModelGameState
import com.jtprince.bingo.core.webclient.model.WebModelPlayerBoard
import net.kyori.adventure.audience.Audience
import org.bukkit.World
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.logging.Logger

class WebBackedGame(
    creator: Audience,
    gameCode: String,
    localPlayers: Collection<BukkitBingoPlayer>
) : BingoGame(creator, gameCode), KoinComponent {
    private val plugin: BingoPlugin by inject()
    private val logger: Logger by inject()

    override var state: State = State.WAITING_FOR_WEBSOCKET
    private val clientId = "KotlinPlugin${hashCode() % 10000}:" +
            localPlayers.map(BingoPlayer::name).joinToString(",")
    private val websocketClient = WebBackedWebsocketClient(
        clientId, gameCode, plugin.core.urlFormatter, plugin.platform.scheduler,
        this::receiveWebsocketOpened, this::receiveWebsocketMessage, this::receiveFailedConnection
    )
    private val messageRelay = WebMessageRelay(websocketClient)
    private var pluginParity: PluginParity? = null

    private val spaces = mutableMapOf<Int, WebBackedSpace>()
    val playerManager = PlayerManager(localPlayers)
    private val playerBoardCache = localPlayers.associateWith(::PlayerBoardCache)
    private var triggerFactory = BukkitAutoMarkTriggerFactory(playerManager)

    /* Both of the following must be ready for the game to be put in the "READY" state */
    private var websocketReady = false
    private var worldsReady = false

    private val startEffects = GameEffects(playerManager) {
        state = State.RUNNING
        websocketClient.sendRevealBoard()
    }

    private var winner: BingoPlayer? = null

    init {
        websocketClient.connect()
        generateWorlds()
    }

    private fun generateWorlds() {
        plugin.server.scheduler.runTask(plugin) { ->
            BukkitMessages.bingoAnnouncePreparingGame(gameCode)
            val players = playerManager.localPlayers
            BukkitMessages.bingoTellTeams(players)
            for (p in players) {
                playerManager.prepareWorldSet(gameCode, p)
                /* Allow for early destruction. */
                if (state == State.DESTROYING) return@runTask
            }

            logger.info("Finished generating " + players.size + " worlds")
            BukkitMessages.bingoAnnounceWorldsGenerated(players)
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
                BukkitMessages.bingoAnnounceGameReady(gameCode, playerManager.localPlayers, creator)
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

        if (plugin.platform.config.debug) {
            for (setting in newPluginParity.getMySettings()) {
                logger.info("[Parity] ${setting.key} = ${setting.value}")
            }
        }

        websocketClient.sendPluginParity(false, newPluginParity.getMySettings())

        pluginParity = newPluginParity
    }

    override fun signalStart(sender: Audience?) {
        if (state != State.READY) {
            sender?.bingoTellNotReady()
            return
        }

        websocketClient.sendStartGame()
    }

    override fun signalEnd(sender: Audience?) {
        if (state <= State.READY) {
            sender?.bingoTellError("The game is not running!")
            return
        }

        websocketClient.sendEndGame()
    }

    override fun signalDestroy(sender: Audience?) {
        for (space in spaces.values) {
            space.destroy()
        }
        playerManager.destroy()
        messageRelay.destroy()
        websocketClient.destroy()
        startEffects.destroy()
        sender?.bingoTell("Game destroyed.")
    }

    fun signalRetry(sender: Audience?) {
        if (state != State.FAILED) {
            sender?.bingoTellError("You can only retry a connection after a connection error.")
            return
        }

        sender?.bingoTell("Retrying connection...")
        websocketClient.retry()
    }

    override fun receiveAutoMark(activation: AutoMarkConsumer.Activation) {
        if (state != State.RUNNING) return

        val space = activation.space
        if (space !is WebBackedSpace) {
            logger.severe(
                "Got auto-mark for space of type ${space::class}, " +
                        "which is not of expected type WebBackedSpace."
            )
            return
        }

        /* This callback is not filtered, and may be called after every time the player completes
         * the goal. We should filter to ensure no excessive backend requests. */
        val cache = playerBoardCache[activation.player] ?: return
        val newMarking = cache.canSendMarking(space.spaceId, space.goalType, activation.fulfilled) ?: return

        websocketClient.sendMarkSpace(activation.player.name, space.spaceId, newMarking.value)
    }

    private fun receiveWebsocketOpened() {
        websocketReady = true
        tryToMoveToReady()
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
        BukkitMessages.bingoAnnounceGameFailed()
        // TODO: `/bingo retry` command?
    }

    private fun receiveBoard(board: WebModelBoard) {
        if (spaces.isNotEmpty()) {
            logger.fine("Received another board, ignoring it.")  // TODO
            return
        }

        for (webModelSpace in board.spaces) {
            val newSpace = WebBackedSpace(triggerFactory, webModelSpace, this)
            spaces[newSpace.spaceId] = newSpace
        }

        val autoSpaces = spaces.values.filter(WebBackedSpace::hasAutoMarkTrigger)
        logger.info("Automated goals: " +
                autoSpaces.map(WebBackedSpace::goalId).sorted().joinToString(", "))

        /* Tell webserver that we are marking these spaces for all local players */
        val autoSpaceIds = autoSpaces.map { s -> s.spaceId }
        websocketClient.sendAutoMarks(
            playerManager.localPlayers.map(BukkitBingoPlayer::name).associateWith { autoSpaceIds }
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
                    logger.warning(
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
                        logger.severe("Unknown game_state marking_type ${msg.markingType}")
                        return
                    }
                }
                BukkitMessages.bingoAnnouncePlayerMarking(player, msg.goalText, invalidate)
            }
            is WebModelGameState.End -> {
                if (state != State.RUNNING && state != State.COUNTING_DOWN) {
                    logger.warning(
                        "Web backend sent an End Game message when game is not running. Ignoring."
                    )
                    return
                }

                state = State.DONE

                val winner = msg.winner?.let { playerManager.bingoPlayer(it) }
                BukkitMessages.bingoAnnounceEnd(winner)
                startEffects.doEndEffects(winner)
            }
        }
    }
}
