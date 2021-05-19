package com.jtprince.bingo.kplugin.automark

import com.jtprince.bingo.kplugin.Messages.bingoTellGoalProgress
import com.jtprince.bingo.kplugin.board.SetVariables
import com.jtprince.bingo.kplugin.player.BingoPlayer
import kotlin.reflect.KClass

/**
 * Container for one BingoPlayer's progress towards a single Auto Mark Trigger. This should only
 * be used by goals that require a persistent state to be tracked between actions that the player
 * can do, such as performing an Event more than once.
 *
 * @param notifyPlayer The BingoPlayer that owns this Progress, and who will be notified as they
 *                     make progress.
 * @param vars Set Variables associated with the space being triggered.
 */
class PlayerTriggerProgress(
    private val space: AutomatedSpace,
    private val notifyPlayer: BingoPlayer,
    private val vars: SetVariables
) {
    private var highestProgress = 0
    private var completed = false
    var progress: Int = 0
        private set

    val triggerExtras = mutableMapOf<KClass<*>, Any>()

    /**
     * Extra data that a trigger definition may store about the player's progress towards this goal.
     */
    inline fun <reified T: Any> extra(default: () -> T): T {
        return triggerExtras.getOrPut(T::class, default) as T
    }

    /**
     * Advance or reverse the player's progress towards this goal.
     * @param towards The total progress that must be made to consider this goal complete.
     * @param amount The amount to move the player's progress by. If negative, decreases the
     *               player's progress.
     * @return True if this advancement is enough to accomplish the goal.
     */
    fun advance(towards: Int, amount: Int = 1): Boolean {
        progress += amount

        /* Update highestProgress and tell player if they have a new highest */
        if (progress > highestProgress) {
            highestProgress = progress
            if (!completed && progress < towards) {
                notifyPlayer.bingoTellGoalProgress(space, progress, towards)
            }
        }

        return progress >= towards
    }

    /**
     * Advance or reverse the player's progress towards this goal.
     * @param towards A Variable representing the total progress that must be made to consider this
     *                goal complete.
     * @param amount The amount to move the player's progress by. If negative, decreases the
     *               player's progress.
     * @return True if this advancement is enough to accomplish the goal.
     */
    fun advance(towards: String, amount: Int = 1): Boolean {
        val towardsVar = vars[towards] ?: throw MissingVariableException(towards)
        return advance(towardsVar, amount)
    }
}
