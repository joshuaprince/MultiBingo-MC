package com.jtprince.bingo.plugin;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BingoWebSocketClient extends WebSocketClient {
    final BingoGame game;

    private static final JSONParser jsonParser = new JSONParser();
    private int reconnectAttemptsRemaining = 10;

    public BingoWebSocketClient(BingoGame game, URI uri) {
        super(uri);
        this.game = game;
    }

    public void sendRevealBoard() {
        if (!this.isOpen()) {
            MCBingoPlugin.logger().warning(
                "Dropping reveal_board packet since Websocket is closed");
            return;
        }
        Map<String, String> m = new HashMap<>();
        m.put("action", "reveal_board");
        JSONObject js = new JSONObject(m);
        this.send(js.toJSONString());
    }

    public void sendMarkSquare(String player, int pos, int toState) {
        if (!this.isOpen()) {
            MCBingoPlugin.logger().warning(
                "Dropping board_mark_admin packet since Websocket is closed");
            return;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("action", "board_mark_admin");
        m.put("player", player);
        m.put("position", pos);
        m.put("to_state", toState);
        JSONObject js = new JSONObject(m);
        this.send(js.toJSONString());
    }

    private void receiveBoard(JSONObject boardJson) {
        JSONArray squaresJson = (JSONArray) boardJson.get("squares");

        ArrayList<Square> squares = new ArrayList<>();
        for (Object o : squaresJson) {
            JSONObject sqJson = (JSONObject) o;
            Square sq = new Square(this.game.gameBoard, sqJson);
            squares.add(sq);
        }

        this.game.gameBoard.setSquares(squares);
        MCBingoPlugin.logger().info("Received board for game " + this.game.gameCode);
    }

    private void receivePlayerBoards(JSONArray playerBoards) {
        for (Object obj : playerBoards) {
            JSONObject json = (JSONObject) obj;
            String playerName = (String) json.get("player_name");
            BingoPlayer player = this.game.getBingoPlayer(playerName);
            if (player == null) {
                MCBingoPlugin.logger().finest("No Bingo Player named " + playerName
                    + " is on this server, but a board exists for them.");
                continue;
            }
            PlayerBoard pb = player.getPlayerBoard();
            if (pb != null) {
                String board = (String) json.get("board");
                pb.update(board);
            }
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        MCBingoPlugin.logger().info(
            "Successfully opened websocket for game " + this.game.gameCode);
        this.reconnectAttemptsRemaining = 10;
        this.game.transitionToReady();
    }

    @Override
    public void onMessage(String message) {
        JSONObject obj;
        try {
            obj = (JSONObject) jsonParser.parse(message);
        } catch (ParseException e) {
            MCBingoPlugin.logger().log(Level.SEVERE, "JSON parsing exception", e);
            return;
        }

        JSONObject board = (JSONObject) obj.get("board");
        if (board != null) {
            this.receiveBoard(board);
        }

        JSONArray pboards = (JSONArray) obj.get("pboards");
        if (pboards != null) {
            this.receivePlayerBoards(pboards);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (code == CloseFrame.NORMAL) {
            MCBingoPlugin.logger().info("Closed websocket for game " + this.game.gameCode);
        }
        else if (this.reconnectAttemptsRemaining-- <= 0) {
            MCBingoPlugin.logger().severe("Websocket failed to reconnect.");
            this.game.messages.announceGameFailed();
            this.game.state = BingoGame.State.FAILED;
        }
        else {
            MCBingoPlugin.logger().warning(
                "Websocket for game " + this.game.gameCode +
                    " closed. Retrying in 5 seconds. " + reason);
            this.game.plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                this.game.plugin, this::reconnect, 5 * 20);
        }
    }

    @Override
    public void onError(Exception ex) {
        MCBingoPlugin.logger().log(Level.SEVERE,
            "Error in web socket connection for game " + game.gameCode, ex);
    }
}
