package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.automarking.AutoMarkTrigger;
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

    public Space(GameBoard board, String goalId, String goalType, String text, int spaceId,
                 Map<String, Integer> variables) {
        this.board = board;
        this.goalId = goalId;
        this.goalType = goalType;
        this.text = text;
        this.spaceId = spaceId;
        if (variables != null) {
            this.variables.putAll(variables);
        }

        this.autoMarkTriggers = AutoMarkTrigger.createAllTriggers(this);
    }

    public Space(GameBoard board, JSONObject obj) {
        this(board, (String) obj.get("goal_id"), (String) obj.get("type"),
            (String) obj.get("text"), ((Long) obj.get("space_id")).intValue(),
            parseVariablesMap((JSONObject) obj.get("variables")));
    }

    public void destroy() {
        for (AutoMarkTrigger t : this.autoMarkTriggers) {
            t.destroy();
        }
    }

    public boolean isAutoMarked() {
        return autoMarkTriggers.size() > 0;
    }

    private static Map<String, Integer> parseVariablesMap(JSONObject variables) {
        Map<String, Integer> ret = new HashMap<>();
        if (variables != null) {
            for (Object o : variables.keySet()) {
                String k = (String) o;
                int v = ((Long) variables.get(k)).intValue();
                ret.put(k, v);
            }
        }
        return ret;
    }
}
