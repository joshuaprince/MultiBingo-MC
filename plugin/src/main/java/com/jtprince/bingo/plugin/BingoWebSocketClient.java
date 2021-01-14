package com.jtprince.bingo.plugin;

import org.bukkit.entity.Player;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BingoWebSocketClient extends WebSocketClient {
    final BingoGame game;
    protected JSONParser jsonParser = new JSONParser();

    protected int reconnectAttemptsRemaining = 10;

    public BingoWebSocketClient(BingoGame game, URI uri) {
        super(uri);
        this.game = game;
    }

    public void sendRevealBoard() {
        if (!this.isOpen()) {
            this.game.plugin.getLogger().warning(
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
            this.game.plugin.getLogger().warning(
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

    protected void receiveGoals(JSONArray goalsJson) {
        ConcreteGoal[] squares = new ConcreteGoal[25];
        for (int i = 0; i < 25; i++) {
            JSONObject goal = (JSONObject) goalsJson.get(i);
            ConcreteGoal cg = new ConcreteGoal(goal);
            squares[i] = cg;
        }

        this.game.plugin.getLogger().info("Received board for game " + this.game.gameCode);
        this.game.gameBoard.setSquares(squares);
    }

    protected void receivePlayerBoards(JSONArray playerBoards) {
        for (Object obj : playerBoards) {
            JSONObject json = (JSONObject) obj;
            String playerName = (String) json.get("player_name");
            PlayerBoard pb = this.game.getPlayerBoard(playerName);
            if (pb != null) {
                String board = (String) json.get("board");
                pb.update(board);
            }
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        this.game.plugin.getLogger().info(
            "Successfully opened websocket for game " + this.game.gameCode);
        this.reconnectAttemptsRemaining = 10;
    }

    @Override
    public void onMessage(String message) {
        JSONObject obj;
        try {
            obj = (JSONObject) jsonParser.parse(message);
        } catch (ParseException e) {
            game.plugin.getLogger().log(Level.SEVERE, "JSON parsing exception" + e.getMessage());
            return;
        }

        JSONArray goals = (JSONArray) obj.get("goals");
        if (goals != null) {
            this.receiveGoals(goals);
        }

        JSONArray pboards = (JSONArray) obj.get("pboards");
        if (pboards != null) {
            this.receivePlayerBoards(pboards);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (this.reconnectAttemptsRemaining-- <= 0) {
            this.game.plugin.getLogger().severe("Websocket failed to reconnect.");
        } else {
            this.game.plugin.getLogger().warning(
                "Websocket for game " + this.game.gameCode +
                    " closed. Retrying in 5 seconds. " + reason);
            this.game.plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                this.game.plugin, this::reconnect, 5 * 20);
        }
    }

    @Override
    public void onError(Exception ex) {
        this.game.plugin.getLogger().log(Level.SEVERE,
            "Error in web socket connection for game " + game.gameCode, ex);
    }
}
