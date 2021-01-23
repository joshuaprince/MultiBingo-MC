package com.jtprince.bingo.plugin.automarking;

import com.jtprince.bingo.plugin.BingoGame;
import com.jtprince.bingo.plugin.BingoPlayer;
import com.jtprince.bingo.plugin.Square;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.*;

/**
 * Root object for all automated board marking functionality in a game.
 *
 * Each BingoGame may have an AutoMarking instance that handles all listening, processing, and
 * marking of squares on the board for all players in that game.
 */
public class AutoMarking {
    final BingoGame game;
    final AutoMarkListener listener;

    private Set<Square> autoMarkedSquares;

    /**
     * A map from Event *class objects* to a list of Method Triggers that are active in this game.
     *
     * For example, if a game has a "Never Use an Axe" square, this map would contain key
     * BlockBreakEvent, and value a list containing MethodTrigger object with jm_never_n_axe38071
     * and the Square that is described "Never Use an Axe".
     */
    private Map<Class<? extends Event>, List<EventTrigger>> activeEventListenerMap = new HashMap<>();

    public AutoMarking(BingoGame game) {
        this.game = game;
        this.listener = new AutoMarkListener(this);
    }

    /**
     * Given a list of Squares on the board, create listeners that will respond when the player does
     * something in game that would activate each of those squares.
     * @param squares List of Squares on the board.
     * @return An Unmodifiable Set of which Squares will be auto-activated.
     */
    public Set<Square> registerGoals(Collection<Square> squares) {
        Map<Class<? extends Event>, List<EventTrigger>> newEventListenerMap = new HashMap<>();
        Set<Square> listenedGoals = new HashSet<>();

        for (Square sq : squares) {
            // Register all squares with ItemTriggers
            if (sq.itemTriggers.size() > 0) {
                listenedGoals.add(sq);
            }

            // Register all squares with EventTriggers
            if (sq.eventTriggers.size() > 0) {
                for (EventTrigger eventTrigger : sq.eventTriggers) {
                    if (!newEventListenerMap.containsKey(eventTrigger.eventType)) {
                        newEventListenerMap.put(eventTrigger.eventType, new ArrayList<>());
                    }
                    newEventListenerMap.get(eventTrigger.eventType).add(eventTrigger);
                }
                listenedGoals.add(sq);
            }
        }

        this.activeEventListenerMap = newEventListenerMap;
        this.autoMarkedSquares = Collections.unmodifiableSet(listenedGoals);
        return this.autoMarkedSquares;
    }

    /**
     * Removes listeners and unregisters all goals, for when a game ends.
     */
    public void destroy() {
        HandlerList.unregisterAll(this.listener);
        this.autoMarkedSquares = null;
    }

    /**
     * Scan the inventory of a given player, to check if any ItemTriggers on the board should
     * be triggered and cause a square to be marked.
     * @param player The player whose inventory should be scanned.
     */
    void impulseInventory(Player player) {
        for (Square sq : this.autoMarkedSquares) {
            for (ItemTrigger trigger : sq.itemTriggers) {
                if (trigger.isSatisfied(player.getInventory())) {
                    this.game.getBingoPlayer(player).getPlayerBoard().autoMark(sq);
                }
            }
        }
    }

    /**
     * Scan an Event that has been fired by Bukkit, to check if any MethodTriggers on the board
     * should be triggered and cause a square to be marked.
     * @param event Event that was raised by Bukkit.
     * @param player The BingoPlayer whose board should be marked if this Event triggers any
     *               markings.
     */
    void impulseEvent(Event event, BingoPlayer player) {
        List<EventTrigger> methods = activeEventListenerMap.get(event.getClass());
        if (methods == null) {
            return;
        }

        for (EventTrigger gal : methods) {
            if (gal.satisfiedBy(event)) {
                player.getPlayerBoard().autoMark(gal.square);
            }
        }
    }

    /**
     * Scan an Event that has been fired by Bukkit, to check if any MethodTriggers on the board
     * should be triggered and cause a square to be marked.
     * @param event Event that was raised by Bukkit.
     * @param player The Bukkit Player that triggered this event, used to match to a BingoPlayer
     *               whose board should be marked.
     */
    void impulseEvent(Event event, Player player) {
        this.impulseEvent(event, this.game.getBingoPlayer(player));
    }

    /**
     * Scan an Event that has been fired by Bukkit, to check if any MethodTriggers on the board
     * should be triggered and cause a square to be marked.
     * @param event Event that was raised by Bukkit.
     * @param world The world this Event occurred in, used to match to a BingoPlayer whose board
     *              should be marked.
     */
    void impulseEvent(Event event, World world) {
        this.impulseEvent(event, this.game.getBingoPlayer(world));
    }
}
