package com.jtprince.bingo.plugin;

import org.bukkit.entity.Player;

public class AutoActivation {
    final BingoGame game;
    final GoalActivationListener listener;

    public AutoActivation(BingoGame game) {
        this.game = game;
        this.listener = new GoalActivationListener(this);
    }

    public void impulseInventory(Player player) {
        for (ConcreteGoal cg : this.game.gameBoard.getSquares()) {
            for (ItemTrigger trigger : cg.itemTriggers) {
                if (trigger.isSatisfied(player.getInventory())) {
                    cg.impulse(player);
                }
            }
        }
    }
}
