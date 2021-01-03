package com.jtprince.bingo.plugin;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class BingoWebSocketClient extends WebSocketClient {
    final BingoGame game;
    protected JSONParser jsonParser = new JSONParser();

    public BingoWebSocketClient(BingoGame game, URI uri) {
        super(uri);
        this.game = game;
    }

    public void sendRevealBoard() {
        Map<String, String> m = new HashMap<>();
        m.put("action", "reveal_board");
        JSONObject js = new JSONObject(m);
        this.send(js.toJSONString());
    }

    public void sendMarkSquare(String player, int pos, int toState) {
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

        this.game.plugin.getLogger().info("Successfully received board.");
        this.game.squares = squares;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {
        JSONObject obj;
        try {
            obj = (JSONObject) jsonParser.parse(message);
        } catch (ParseException e) {
            game.plugin.getLogger().severe("JSON parsing exception: " + e.getMessage());
            return;
        }

        JSONArray goals = (JSONArray) obj.get("goals");
        if (goals != null) {
            this.receiveGoals(goals);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }
}
