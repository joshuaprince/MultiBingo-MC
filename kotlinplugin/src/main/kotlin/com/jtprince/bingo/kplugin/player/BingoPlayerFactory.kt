package com.jtprince.bingo.kplugin.player

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

object BingoPlayerFactory {
    /**
     * Create Bingo Players based on all players currently on the server, organizing them into
     * teams if they are on a Scoreboard team.
     */
    fun createPlayers(): Collection<BingoPlayer> {
        val ret = HashSet<BingoPlayer>()

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
                ret.add(BingoPlayerSingle(p))
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
            val bpt = BingoPlayerTeam(team.displayName(), players)
            ret.add(bpt)
        }

        return ret.toSet()
    }
}
