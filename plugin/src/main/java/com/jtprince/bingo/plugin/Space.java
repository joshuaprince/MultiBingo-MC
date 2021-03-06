package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.automarking.AutoMarkTrigger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Space {
    public final GameBoard board;

    public final String goalId;
    public final String goalType;
    public final String text;
    public final int spaceId;
    public final Map<String, Integer> variables = new HashMap<>();

    public final Collection<AutoMarkTrigger> autoMarkTriggers;

    public Space(GameBoard board, JSONObject obj) {
        this.board = board;

        this.goalId = (String) obj.get("goal_id");
        this.goalType = (String) obj.get("type");
        this.text = (String) obj.get("text");
        this.spaceId = ((Long) obj.get("space_id")).intValue();

        JSONObject vars = (JSONObject) obj.get("variables");
        if (vars != null) {
            for (Object o : vars.keySet()) {
                String k = (String) o;
                int v = Integer.parseInt((String) vars.get(k));
                variables.put(k, v);
            }
        }

        /* List of Auto-Triggers defined in goals.xml and passed to the plugin as JSON */
        JSONArray xmlTriggers = (JSONArray) obj.get("triggers");

        this.autoMarkTriggers = AutoMarkTrigger.createAllTriggers(this, xmlTriggers);
    }

    public void destroy() {
        for (AutoMarkTrigger t : this.autoMarkTriggers) {
            t.destroy();
        }
    }

    public boolean isAutoMarked() {
        return autoMarkTriggers.size() > 0;
    }
}
