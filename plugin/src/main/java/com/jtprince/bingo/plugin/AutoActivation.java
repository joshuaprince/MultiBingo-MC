package com.jtprince.bingo.plugin;

import org.bukkit.World;
import org.bukkit.entity.Player;

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
        for (ConcreteGoal cg : this.game.gameBoard.getSquares()) {
            if (goal.equals(cg.id)) {
                this.game.wsClient.sendMarkSquare(player.getName(), cg.position, 1);
            }
        }
    }

    /**
     * Activate a square for the player that is associated to this world if it exists on the board.
     * @param world World that contains this player.
     * @param goal The goal ID to activate.
     */
    public void impulseGoal(World world, String goal) {
        impulseGoal(this.game.getPlayerInWorld(world), goal);
    }

    /**
     * Activate a goal phrased like "Never" - marking it as "undone". For example, a goal
     * "Never Sleep" would be negatively impulsed if the player sleeps.
     * @param player Player to "de"activate for.
     * @param goal The goal ID to "de"activate.
     */
    public void impulseGoalNegative(Player player, String goal) {
        for (ConcreteGoal cg : this.game.gameBoard.getSquares()) {
            if (goal.equals(cg.id)) {
                this.game.wsClient.sendMarkSquare(player.getName(), cg.position, 3);
            }
        }
    }

    /**
     * Consider marking a goal as complete if it exists on the board, indicating that the player has
     * made some amount of progress towards a variable goal. If the variable passed is equal to or
     * greater than the goal's set variable, this goal will be activated.
     *
     * Only works for single-variable goals, where the goal is named $var!
     * @param player Player to activate for.
     * @param goal The goal ID to activate.
     * @param var The player's current progress towards this goal. The goal will only activate if
     *            this is greater than or equal to the goal's set variable.
     */
    public void impulseGoal(Player player, String goal, int var) {
        String varName = "var";

        for (ConcreteGoal cg : this.game.gameBoard.getSquares()) {
            if (goal.equals(cg.id)) {
                Integer varValue = cg.variables.get(varName);
                if (varValue == null) {
                    this.game.plugin.getLogger().warning(
                        "Cannot impulse unknown variable " + varName + " on " + cg.id);
                    continue;
                }

                if (var >= varValue) {
                    this.game.wsClient.sendMarkSquare(player.getName(), cg.position, 1);
                }
            }
        }
    }

    public void impulseInventory(Player player) {
        for (ConcreteGoal cg : this.game.gameBoard.getSquares()) {
            for (ItemTrigger trigger : cg.itemTriggers) {
                if (trigger.isSatisfied(player.getInventory())) {
                    this.game.wsClient.sendMarkSquare(player.getName(), cg.position, 1);
                }
            }
        }
    }
}
