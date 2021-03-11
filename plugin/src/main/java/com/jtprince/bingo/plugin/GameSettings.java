package com.jtprince.bingo.plugin;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Settings given to the webserver when creating a new game.
 */
public class GameSettings {
    public String gameCode = null;
    public String shape = null;
    public Integer boardDifficulty = null;
    public String seed = null;
    public String[] forcedGoals = null;

    /**
     * Generate a board with the backend
     * @return The new game's Game Code
     */
    public String generateBoardBlocking() throws IOException, ParseException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(MCBConfig.getBoardCreateUrl());

            StringEntity requestEntity = new StringEntity(this.getRequestJsonMap().toJSONString(), ContentType.APPLICATION_JSON);

            httpPost.setEntity(requestEntity);
            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                JSONObject responseJson = (JSONObject) new JSONParser().parse(EntityUtils.toString(responseEntity));
                EntityUtils.consume(responseEntity);
                return (String) responseJson.get("game_code");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject getRequestJsonMap() {
        JSONObject obj = new JSONObject();
        if (gameCode != null) {
            obj.put("game_code", gameCode);
        }
        if (shape != null) {
            obj.put("shape", shape);
        }
        if (boardDifficulty != null) {
            obj.put("board_difficulty", boardDifficulty);
        }
        if (seed != null) {
            obj.put("seed", seed);
        }
        if (forcedGoals != null) {
            JSONArray fgs = new JSONArray();
            fgs.addAll(Arrays.asList(forcedGoals));
            obj.put("forced_goals", fgs);
        }
        return obj;
    }
}
