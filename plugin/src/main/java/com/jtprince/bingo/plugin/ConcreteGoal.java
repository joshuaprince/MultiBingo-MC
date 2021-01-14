package com.jtprince.bingo.plugin;

import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConcreteGoal {
    BingoGame game;

    public final String id;
    public final String text;
    public final int position;
    public final Map<String, Integer> variables = new HashMap<>();
    public final ArrayList<ItemTrigger> itemTriggers = new ArrayList<>();

    public ConcreteGoal(BingoGame game, JSONObject obj) {
        this.game = game;

        this.id = (String) obj.get("id");
        this.text = (String) obj.get("text");
        this.position = ((Long) obj.get("position")).intValue();

        JSONObject vars = (JSONObject) obj.get("variables");
        if (vars != null) {
            for (Object o : vars.keySet()) {
                String k = (String) o;
                int v = Integer.parseInt((String) vars.get(k));
                variables.put(k, v);
            }
        }

        JSONArray triggers = (JSONArray) obj.get("triggers");
        if (triggers != null) {
            for (Object trigger : triggers) {
                JSONObject trig = (JSONObject) trigger;
                itemTriggers.add(new ItemTrigger(trig));
            }
        }
    }

    /**
     * Simply activate a square for a player if it exists on the board.
     * @param player Player to activate for.
     */
    public void impulse(Player player) {
        debugLog("Impulsing goal " + this.id);
        this.game.wsClient.sendMarkSquare(player.getName(), position, 1);
    }

    /**
     * Activate a goal phrased like "Never" - marking it as "undone". For example, a goal
     * "Never Sleep" would be negatively impulsed if the player sleeps.
     * @param player Player to "de"activate for.
     */
    public void impulseNegative(Player player) {
        debugLog("Impulsing negative goal " + this.id);
        this.game.wsClient.sendMarkSquare(player.getName(), position, 3);
    }

    protected void debugLog(String msg) {
        if (this.game.plugin.debug) {
            this.game.plugin.getLogger().info(msg);
        } else {
            this.game.plugin.getLogger().finer(msg);
        }
    }
}
