package com.jtprince.bingo.core.webclient

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jtprince.bingo.core.scheduler.Scheduler
import org.java_websocket.client.WebSocketClient
import org.java_websocket.framing.CloseFrame
import org.java_websocket.handshake.ServerHandshake
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Serializable
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class WebBackedWebsocketClient(
    val clientId: String,
    private val gameCode: String,
    private val urlFormatter: BingoUrlFormatter,
    private val scheduler: Scheduler,
    private val onFirstOpen: () -> Unit,
    private val onReceive: (WebsocketRxMessage) -> Unit,
    private val onFailure: () -> Unit,
) : WebSocketClient(urlFormatter.websocketUrl(gameCode, clientId)), KoinComponent {
    private val logger: Logger by inject()

    companion object {
        const val RECONNECT_ATTEMPTS = 10
        const val RECONNECT_SECONDS_BETWEEN_ATTEMPTS = 5
    }

    private val mapper = jacksonObjectMapper()

    private val txQueue: Queue<WebsocketTxMessage> = LinkedList()

    private var reconnectTask: Scheduler.Task? = null
    private var connectedBefore = false
    private var connectAttemptsRemaining = RECONNECT_ATTEMPTS

    override fun onOpen(handshakedata: ServerHandshake) {
        logger.info("Successfully connected to game $gameCode.")

        if (!connectedBefore) {
            onFirstOpen()
            connectedBefore = true
        }

        connectAttemptsRemaining = RECONNECT_ATTEMPTS
        flushTxQueue()
    }

    override fun onMessage(message: String) {
        try {
            val msg = mapper.readValue<WebsocketRxMessage>(message)
            onReceive(msg)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Could not receive websocket message", e)
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        if (code == CloseFrame.NORMAL && !remote) {
            /* Normal close initiated by us, do nothing. */
            return
        }

        if (connectAttemptsRemaining > 0) {
            /* Attempt to reconnect if we didn't want this socket to close yet. */
            logger.warning("Lost connection to the backend. Retrying in " +
                    "$RECONNECT_SECONDS_BETWEEN_ATTEMPTS seconds.")

            reconnectTask?.cancel()
            reconnectTask = scheduler.scheduleAsync(RECONNECT_SECONDS_BETWEEN_ATTEMPTS.toLong() * 20) {
                logger.info("Attempting reconnection...")
                connectAttemptsRemaining--
                reconnect()
            }
        } else {
            logger.warning("Could not connect to the backend after " +
                    "$RECONNECT_ATTEMPTS attempts.")
            onFailure()
        }
    }

    override fun onError(ex: Exception) {
        logger.log(Level.SEVERE, "Websocket error: ${ex.localizedMessage}")
    }

    fun destroy() {
        close()
        reconnectTask?.cancel()
        logger.info("Websocket closed for game $gameCode")
    }

    fun retry() {
        reconnect()
    }

    private fun flushTxQueue() {
        while (!txQueue.isEmpty() && isOpen) {
            val msg = txQueue.peek()
            send(mapper.writeValueAsString(msg))
            txQueue.remove()  // after send so the message is kept on error
        }
    }

    private fun send(msg: WebsocketTxMessage) {
        txQueue.add(msg)
        flushTxQueue()
    }

    fun sendStartGame() {
        /* Does not actually block since channel is set to drop on full */
        send(TxMessageGameState(true))
    }

    fun sendEndGame() {
        send(TxMessageGameState(false))
    }

    fun sendRevealBoard() {
        send(TxMessageRevealBoard())
    }

    fun sendMarkSpace(player: String, spaceId: Int, marking: Int) {
        send(TxMessageMarkSpace(player, spaceId, marking))
    }

    fun sendAutoMarks(playerSpaceIdsMap: Map<String, Collection<Int>>) {
        send(TxMessageSetAutoMarks(playerSpaceIdsMap))
    }

    fun sendMessage(msgJson: String) {
        send(TxMessageMessageRelay(msgJson))
    }

    fun sendPluginParity(isEcho: Boolean, mySettings: Map<String, Serializable>) {
        send(TxMessagePluginParity(isEcho, mySettings))
    }


}
