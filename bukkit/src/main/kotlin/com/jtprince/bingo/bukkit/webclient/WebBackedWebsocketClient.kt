package com.jtprince.bingo.bukkit.webclient

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jtprince.bingo.bukkit.BingoConfig
import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.PluginParity
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import org.java_websocket.client.WebSocketClient
import org.java_websocket.framing.CloseFrame
import org.java_websocket.handshake.ServerHandshake
import java.util.*
import java.util.logging.Level

class WebBackedWebsocketClient(
    private val gameCode: String,
    internal val clientId: String,
    private val onFirstOpen: () -> Unit,
    private val onReceive: (WebsocketRxMessage) -> Unit,
    private val onFailure: () -> Unit,
) : WebSocketClient(BingoConfig.websocketUrl(gameCode, clientId)) {
    companion object {
        const val RECONNECT_ATTEMPTS = 10
        const val RECONNECT_SECONDS_BETWEEN_ATTEMPTS = 5
    }

    private val mapper = jacksonObjectMapper()

    private val txQueue: Queue<WebsocketTxMessage> = LinkedList()

    private var reconnectTask: BukkitTask? = null
    private var connectedBefore = false
    private var connectAttemptsRemaining = RECONNECT_ATTEMPTS

    override fun onOpen(handshakedata: ServerHandshake) {
        BingoPlugin.logger.info("Successfully connected to game $gameCode.")

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
            BingoPlugin.logger.log(Level.SEVERE, "Could not receive websocket message", e)
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        if (code == CloseFrame.NORMAL && !remote) {
            /* Normal close initiated by us, do nothing. */
            return
        }

        if (connectAttemptsRemaining > 0) {
            /* Attempt to reconnect if we didn't want this socket to close yet. */
            BingoPlugin.logger.warning("Lost connection to the backend. Retrying in " +
                    "$RECONNECT_SECONDS_BETWEEN_ATTEMPTS seconds.")

            reconnectTask?.cancel()
            reconnectTask = Bukkit.getScheduler().runTaskLaterAsynchronously(BingoPlugin, { ->
                BingoPlugin.logger.info("Attempting reconnection...")
                connectAttemptsRemaining--
                reconnect()
            }, RECONNECT_SECONDS_BETWEEN_ATTEMPTS.toLong() * 20)
        } else {
            BingoPlugin.logger.warning("Could not connect to the backend after " +
                    "$RECONNECT_ATTEMPTS attempts.")
            onFailure()
        }
    }

    override fun onError(ex: Exception) {
        BingoPlugin.logger.log(Level.SEVERE, "Websocket error: ${ex.localizedMessage}")
    }

    fun destroy() {
        close()
        reconnectTask?.cancel()
        BingoPlugin.logger.info("Websocket closed for game $gameCode")
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

    fun sendPluginParity(isEcho: Boolean, mySettings: PluginParity.Settings) {
        send(TxMessagePluginParity(isEcho, mySettings))
    }
}
