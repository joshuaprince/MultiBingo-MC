package com.jtprince.bingo.bukkit.player

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

object BukkitBingoPlayerFactory {
    /**
     * Create Bingo Players based on all players currently on the server, organizing them into
     * teams if they are on a Scoreboard team.
     */
    fun createPlayers(): Collection<BukkitBingoPlayer> {
        val ret = HashSet<BukkitBingoPlayer>()

        // Create a mapping from Player -> Team (or null)
        val playerTeamMap = HashMap<Player, Team?>()
        for (p in Bukkit.getOnlinePlayers()) {
            playerTeamMap[p] = null
            for (team in Bukkit.getScoreboardManager().mainScoreboard.teams) {
                if (team.hasEntry(p.name)) {
                    playerTeamMap[p] = team
                }
            }
        }

        // Reverse the map direction
        val teamPlayerMap = HashMap<Team, MutableSet<OfflinePlayer>>()
        for (p in playerTeamMap.keys) {
            val team = playerTeamMap[p]
            if (team == null) {
                // No team, add the player to a BingoPlayerSingle
                ret.add(BukkitBingoPlayerSingle(p))
            } else {
                // Player is on a team. Add to teamPlayerMap
                if (!teamPlayerMap.containsKey(team)) {
                    teamPlayerMap[team] = HashSet()
                }
                teamPlayerMap[team]!!.add(p)
            }
        }

        // Create all BingoPlayerTeams.
        for ((team, players) in teamPlayerMap) {
            val bpt = BukkitBingoPlayerTeam(
                Component.textOfChildren(team.displayName().color(team.color())),
                players
            )
            ret.add(bpt)
        }

        return ret.toSet()
    }
}
