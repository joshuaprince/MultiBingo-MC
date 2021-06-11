package com.jtprince.bingo.bukkit.game.web

import com.jtprince.bingo.bukkit.BingoConfig
import com.jtprince.bingo.bukkit.BingoPlugin
import com.jtprince.bingo.bukkit.BukkitMessages.bingoTell
import com.jtprince.bingo.bukkit.automark.EventPlayerMapper
import com.jtprince.bingo.bukkit.player.BukkitBingoPlayer
import com.jtprince.bingo.core.player.BingoPlayer
import com.jtprince.bingo.core.player.RemoteBingoPlayer
import com.jtprince.bukkit.worldset.WorldSet
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.PlayerLeashEntityEvent
import org.bukkit.event.hanging.HangingEvent
import org.bukkit.event.inventory.InventoryEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.vehicle.VehicleEvent
import org.bukkit.event.weather.WeatherEvent
import org.bukkit.event.world.WorldEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

/**
 * Single-game container for all players in that game and functionality relating to them.
 */
class PlayerManager(localPlayers: Collection<BukkitBingoPlayer>) : EventPlayerMapper, KoinComponent {
    private val plugin: BingoPlugin by inject()

    /**
     * Bukkit Player UUID -> BingoPlayer that contains that Player
     */
    private val localPlayersMap: HashMap<UUID, BukkitBingoPlayer> = run {
        val ret = HashMap<UUID, BukkitBingoPlayer>()
        for (p in localPlayers) {
            for (op in p.offlinePlayers) {
                ret[op.uniqueId] = p
            }
        }
        ret
    }
    private val remotePlayers = HashSet<RemoteBingoPlayer>()

    internal val playerWorldSetMap = HashMap<BukkitBingoPlayer, WorldSet>()
    internal val worldPlayerMap = HashMap<World, BukkitBingoPlayer>()

    /**
     * A list of BingoPlayers that are participating in this game. This includes all
     * remote players (i.e. that are not logged in to this server).
     */
    val players: Collection<BingoPlayer>
        get() = localPlayersMap.values + remotePlayers

    /**
     * Return a list of BingoPlayers that are participating in this game and playing on this
     * server (i.e. does not include Remote players)
     */
    override val localPlayers: Collection<BukkitBingoPlayer>
        get() = localPlayersMap.values

    /**
     * A list of Bukkit Player objects that are participating in this game and connected
     * to the server.
     */
    val bukkitPlayers: Collection<Player>
        get() = localPlayers.map { p -> p.bukkitPlayers }.flatten()

    /**
     * Find which BingoPlayer is associated to a Bukkit Player.
     * @param player The Bukkit Player to check.
     * @return The BingoPlayer object, or null if this Player is not part of this game.
     */
    fun bingoPlayer(player: Player): BukkitBingoPlayer? {
        return localPlayersMap[player.uniqueId]
    }

    /**
     * Find a BingoPlayer with a given name.
     * @param name String to look for.
     */
    fun bingoPlayer(name: String): BingoPlayer? {
        return players.find { bp -> bp.name == name }
    }

    /**
     * Find a BingoPlayer with a given name. If there is no player with the given name on this
     * server, a RemoteBingoPlayer will always be returned.
     * @param name String to look for.
     * @return The BingoPlayer object, or a new RemoteBingoPlayer if this player has never been
     *         seen before.
     */
    fun bingoPlayerOrCreateRemote(name: String): BingoPlayer {
        return bingoPlayer(name) ?: run {
            /* Player does not exist. Create a new one. */
            plugin.logger.info("Creating new Remote player $name.")
            val newBingoPlayer = RemoteBingoPlayer(name)
            remotePlayers.add(newBingoPlayer)
            newBingoPlayer
        }
    }

    /**
     * Find which BingoPlayer is associated to a World on the server.
     * @param world The World to check.
     * @return The BingoPlayer object, or null if this World is not part of this game.
     */
    fun bingoPlayer(world: World): BukkitBingoPlayer? {
        return worldPlayerMap[world]
    }

    /**
     * Find the set of worlds this Bingo Player is given to play in for this game.
     * @param player A Local BingoPlayer.
     * @return The player's WorldSet.
     * @throws RuntimeException If there is no WorldSet for this Player.
     */
    override fun worldSet(player: BukkitBingoPlayer) : WorldSet {
        return playerWorldSetMap[player] ?: throw RuntimeException(
            "Failed to get WorldSet for ${player.name}")
    }

    /**
     * Create worlds for a given player.
     * @param player Player to create worlds for.
     */
    fun prepareWorldSet(gameCode: String, player: BukkitBingoPlayer): WorldSet {
        val worldSet = plugin.worldSetManager.createWorldSet(
            worldSetCode = "${gameCode}_${player.slugName()}",
            seed = gameCode
        )

        playerWorldSetMap[player] = worldSet
        for (w in worldSet.worlds) {
            worldPlayerMap[w] = player
        }

        return worldSet
    }

    /**
     * Unload all worlds the game is being played in.
     */
    fun destroy() {
        for (ws in playerWorldSetMap.values) {
            ws.manager?.unload(ws, BingoConfig.saveWorlds) { playerInWorldSet ->
                playerInWorldSet.bingoTell("The game is over, returning you to the spawn world.")
            }
        }
    }

    /**
     * Determine which BingoPlayer an Event is associated with, for determining who to
     * potentially automark for.
     */
    override fun mapEvent(event: Event): BukkitBingoPlayer? {
        return when (event) {
            is PlayerEvent -> bingoPlayer(event.player)
            is WorldEvent -> bingoPlayer(event.world)
            is InventoryEvent -> bingoPlayer(event.view.player as Player)
            is BlockEvent -> bingoPlayer(event.block.world)
            is EntityEvent -> bingoPlayer(event.entity.world)
            is HangingEvent -> bingoPlayer(event.entity.world)
            is PlayerLeashEntityEvent -> bingoPlayer(event.player)
            is VehicleEvent -> bingoPlayer(event.vehicle.world)
            is WeatherEvent -> bingoPlayer(event.world)
            else -> run {
                plugin.logger.warning("Received a ${event::class}, but don't know " +
                        "how to assign it to a Bingo Player")
                null
            }
        }
    }

    private fun BingoPlayer.slugName(): String {
        return name.filter { ch -> ch.isLetterOrDigit() }
    }
}
