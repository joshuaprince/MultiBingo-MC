package com.jtprince.bingo.plugin;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class ConcreteGoal {
    public final String id;
    public final int position;
    public final ArrayList<ItemTrigger> itemTriggers = new ArrayList<>();

    public ConcreteGoal(JSONObject obj) {
        this.id = (String) obj.get("id");
        this.position = ((Long) obj.get("position")).intValue();

        JSONArray triggers = (JSONArray) obj.get("triggers");
        if (triggers != null) {
            for (Object trigger : triggers) {
                JSONObject trig = (JSONObject) trigger;
                itemTriggers.add(new ItemTrigger(trig));
            }
        }
    }
}
