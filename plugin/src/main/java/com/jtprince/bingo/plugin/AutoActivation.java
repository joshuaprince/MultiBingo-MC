package com.jtprince.bingo.plugin;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class AutoActivation {
    final BingoGame game;
    final GoalActivationListener listener;

    public AutoActivation(BingoGame game) {
        this.game = game;
        this.listener = new GoalActivationListener(this);
    }

    /**
     * Simply activate a square for a player if it exists on the board.
     * @param player Player to activate for.
     * @param goal The goal ID to activate.
     */
    public void impulseGoal(Player player, String goal) {

    }

    /**
     * Activate a goal phrased like "Never" - marking it as "undone". For example, a goal
     * "Never Sleep" would be negatively impulsed if the player sleeps.
     * @param player Player to "de"activate for.
     * @param goal The goal ID to "de"activate.
     */
    public void impulseGoalNegative(Player player, String goal) {

    }

    /**
     * Consider marking a goal as complete if it exists on the board, indicating that the player has
     * made some amount of progress towards a variable goal. If the variable passed is equal to or
     * greater than the goal's set variable, this goal will be activated.
     *
     * Convenience method for impulseGoalVariables where the goal only has one variable named "var".
     * @param player Player to activate for.
     * @param goal The goal ID to activate.
     * @param var The player's current progress towards this goal. The goal will only activate if
     *            this is greater than or equal to the goal's set variable.
     */
    public boolean impulseGoal(Player player, String goal, int var) {
        HashMap<String, Integer> varMap = new HashMap<>();
        varMap.put("var", var);
        return this.impulseGoal(player, goal, varMap);
    }

    /**
     * Consider marking a goal as complete if it exists on the board, indicating that the player has
     * made some amount of progress towards a variable goal. If the variable passed is equal to or
     * greater than the goal's set variable, this goal will be activated.
     * @param player Player to activate for.
     * @param goal The goal ID to activate.
     * @param varMap A mapping of variable names to the values that this player has acquired. If
     *               ALL entries in this map are greater than or equal to the goal's set variables,
     *               the goal will activate.
     */
    public boolean impulseGoal(Player player, String goal, Map<String, Integer> varMap) {
        return true;
    }

    public void impulseInventory(Player player) {
        if (this.game.wsClient == null || this.game.squares == null) {
            return;
        }

        for (ConcreteGoal cg : this.game.squares) {
            for (ItemTrigger trigger : cg.itemTriggers) {
                if (trigger.isSatisfied(player.getInventory())) {
                    this.game.wsClient.sendMarkSquare(player.getName(), cg.position, 1);
                }
            }
        }
    }
}
