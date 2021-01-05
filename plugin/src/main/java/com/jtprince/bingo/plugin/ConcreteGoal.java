package com.jtprince.bingo.plugin;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConcreteGoal {
    public final String id;
    public final String text;
    public final int position;
    public final Map<String, Integer> variables = new HashMap<>();
    public final ArrayList<ItemTrigger> itemTriggers = new ArrayList<>();

    public ConcreteGoal(JSONObject obj) {
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
}
