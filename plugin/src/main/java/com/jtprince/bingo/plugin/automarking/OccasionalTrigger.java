package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.BingoGame;
import com.jtprince.bingo.plugin.player.BingoPlayer;
import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.Space;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Defines a Trigger that is called at a regular interval to check if a player has completed a
 * goal.
 */
class OccasionalTrigger extends AutoMarkTrigger {
    final Space space;
    private final Method method;
    private final int taskId;

    private OccasionalTrigger(Space space, Method method, OccasionalTriggerListener anno) {
        this.space = space;
        this.method = method;
        this.taskId = space.board.game.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
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
        space.board.game.plugin.getServer().getScheduler().cancelTask(taskId);
    }

    private void invoke() {
        if (this.space.board.game.state != BingoGame.State.RUNNING) {
            return;
        }

        try {
            for (BingoPlayer p : this.space.board.game.getLocalPlayers()) {
                boolean res = (boolean) this.method.invoke(this, p);
                if (res) {
                    space.board.game.getPlayerBoard(p).autoMark(this.space);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            MCBingoPlugin.logger().log(Level.SEVERE,
                "Failed to OccasionalTrigger " + this.method.getName(), e);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface OccasionalTriggerListener {
        /** Ticks to wait between checking if this has been triggered */
        int value();
    }

    @OccasionalTriggerListener(20)
    private boolean jm_compl_a_map49710(BingoPlayer player) {
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

    @OccasionalTriggerListener(20)
    private boolean jm_compl_e_map18056(BingoPlayer player) {
        // Complete a full size map (Highest zoom)
        for (Player p : player.getBukkitPlayers()) {
            for (ItemStack itemStack : p.getInventory()) {
                if (ActivationHelpers.isCompletedMap(itemStack, MapView.Scale.FARTHEST)) {
                    return true;
                }
            }
        }

        return false;
    }
}
