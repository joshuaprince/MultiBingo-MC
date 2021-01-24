package com.jtprince.bingo.plugin;

import dev.jorel.commandapi.CommandAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MCBingoPlugin extends JavaPlugin {
    private static MCBingoPlugin plugin;  // singleton instance

    public WorldManager worldManager;
    private BingoGame currentGame;

    @Override
    public void onLoad() {
        plugin = this;

        if (MCBConfig.getDebug()) {
            logger().info("Debug mode is enabled.");
            logger().setLevel(Level.FINER);
        }

        CommandAPI.onLoad(MCBConfig.getDebug());
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable(this);

        this.worldManager = new WorldManager(this);
        new MCBCommands(this).registerCommands();

        this.saveDefaultConfig();
    }

    public static MCBingoPlugin instance() {
        return plugin;
    }

    public static Logger logger() {
        return plugin.getLogger();
    }

    public BingoGame getCurrentGame() {
        return currentGame;
    }

    void prepareNewGame(String gameCode) {
        this.destroyCurrentGame();
        this.currentGame = new BingoGame(this, gameCode, createBingoPlayers());
    }

    void destroyCurrentGame() {
        if (this.currentGame != null) {
            this.currentGame.destroy();
            this.currentGame = null;
        }
    }

    private Collection<BingoPlayer> createBingoPlayers() {
        ArrayList<BingoPlayer> ret = new ArrayList<>();

        // Create a mapping from Player -> Team (or null)
        Map<Player, Team> playerTeamMap = new HashMap<>();
        for (Player p : this.getServer().getOnlinePlayers()) {
            playerTeamMap.put(p, null);
            for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
                if (team.hasEntry(p.getName())) {
                    playerTeamMap.put(p, team);
                }
            }
        }

        // Reverse the map direction
        Map<Team, Set<OfflinePlayer>> teamPlayerMap = new HashMap<>();
        for (Player p : playerTeamMap.keySet()) {
            Team team = playerTeamMap.get(p);
            if (team == null) {
                // No team, add the player to a BingoPlayerSingle
                ret.add(new BingoPlayerSingle(p));
            } else {
                // Player is on a team. Add to teamPlayerMap
                if (!teamPlayerMap.containsKey(team)) {
                    teamPlayerMap.put(team, new HashSet<>());
                }
                teamPlayerMap.get(team).add(p);
            }
        }

        // Create all BingoPlayerTeams.
        for (Team t : teamPlayerMap.keySet()) {
            BingoPlayerTeam bpt = new BingoPlayerTeam(t.getDisplayName(), teamPlayerMap.get(t),
                                                      t.getColor().asBungee());
            ret.add(bpt);
        }

        return ret;
    }
}
