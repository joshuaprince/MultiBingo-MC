package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.automarking.itemtrigger.ItemTriggerYaml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Standalone executable that lists all goals that are automated by the plugin.
 */
public class AutomatedGoalList {
    public static void main(String[] args) throws IOException {
        Set<String> goals = getAutomatedGoals();
        if (args.length > 0) {
            BufferedWriter out = new BufferedWriter(new FileWriter(args[0], false));
            for (String goal : goals) {
                out.write(goal + '\n');
            }
            out.close();
        } else {
            goals.forEach(System.out::println);
        }
    }

    static Set<String> getAutomatedGoals() {
        HashSet<String> ret = new HashSet<>();
        ret.addAll(EventTrigger.allAutomatedGoals());
        ret.addAll(OccasionalTrigger.allAutomatedGoals());
        ret.addAll(ItemTriggerYaml.defaultYaml().allAutomatedGoals());
        return ret;
    }
}
