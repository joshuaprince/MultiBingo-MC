package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.BingoGame;
import com.jtprince.bingo.plugin.player.BingoPlayer;
import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.Space;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Defines a Trigger that is called at a regular interval to check if a player has completed a
 * goal.
 */
class OccasionalTrigger extends AutoMarkTrigger {
    private final BukkitScheduler scheduler;
    private final Method method;
    private final int taskId;

    private OccasionalTrigger(Space space, Method method, OccasionalTriggerListener anno) {
        super(space);
        this.scheduler = MCBingoPlugin.instance().getServer().getScheduler();
        this.method = method;
        this.taskId = scheduler.scheduleSyncRepeatingTask(
                space.board.game.plugin, this::invoke, anno.value(), anno.value());
    }

    static ArrayList<OccasionalTrigger> createTriggers(Space space) {
        ArrayList<OccasionalTrigger> ret = new ArrayList<>();

        // Register goals with Method triggers
        for (Method method : OccasionalTrigger.class.getDeclaredMethods()) {
            OccasionalTriggerListener anno = method.getAnnotation(OccasionalTriggerListener.class);
            if (anno == null) {
                continue;
            }

            // TODO Sanity check each method - return type, params, static, etc
            // TODO Move that sanity check to an onEnable callback, rather than log spam 25x on
            //   every board receive

            if (!space.goalId.equals(method.getName())) {
                continue;
            }

            ret.add(new OccasionalTrigger(space, method, anno));
        }

        return ret;
    }

    public void destroy() {
        scheduler.cancelTask(taskId);
    }

    private void invoke() {
        if (this.getSpace().board.game.state != BingoGame.State.RUNNING) {
            return;
        }

        try {
            for (BingoPlayer p : this.getSpace().board.game.playerManager.getLocalPlayers()) {
                boolean res = (boolean) this.method.invoke(this, p);
                if (res) {
                    getSpace().board.game.playerManager.getPlayerBoard(p).autoMark(this.getSpace());
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            MCBingoPlugin.logger().log(Level.SEVERE,
                "Failed to OccasionalTrigger " + this.method.getName(), e);
        }
    }

    public static Set<String> allAutomatedGoals() {
        Set<String> ret = new HashSet<>();
        for (Method method : OccasionalTrigger.class.getDeclaredMethods()) {
            OccasionalTriggerListener anno = method.getAnnotation(OccasionalTriggerListener.class);
            if (anno == null) {
                continue;
            }
            ret.add(method.getName());
        }
        return ret;
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface OccasionalTriggerListener {
        /** Ticks to wait between checking if this has been triggered */
        int value();
    }

    @OccasionalTriggerListener(20)
    private boolean jm_complete_map(BingoPlayer player) {
        // Complete a map (Any zoom)
        for (Player p : player.getBukkitPlayers()) {
            for (ItemStack itemStack : p.getInventory()) {
                if (ActivationHelpers.isCompletedMap(itemStack, null)) {
                    return true;
                }
            }
        }

        return false;
    }
}
