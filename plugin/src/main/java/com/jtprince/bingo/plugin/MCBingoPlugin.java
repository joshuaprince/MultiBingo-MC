package com.jtprince.bingo.plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MCBingoPlugin extends JavaPlugin {
    boolean debug = false;
    public WorldManager worldManager;
    private BingoGame currentGame;

    @Override
    public void onLoad() {
        if (this.getConfig().getBoolean("debug", false)) {
            this.getLogger().info("Debug mode is enabled.");
            this.getLogger().setLevel(Level.FINER);
            this.debug = true;
        }

        CommandAPI.onLoad(this.debug);
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable(this);

        this.worldManager = new WorldManager(this);
        this.registerCommands();

        this.saveDefaultConfig();
    }

    private void registerCommands() {
        CommandAPICommand prepareCmd = new CommandAPICommand("prepare")
            .executes((CommandExecutor) (sender, args) -> commandPrepare(sender, null));
        CommandAPICommand prepareCmdArg = new CommandAPICommand("prepare")
            .withArguments(new StringArgument("gameCode"))
            .executes((CommandExecutor) (sender, args) -> commandPrepare(sender, (String) args[0]));

        CommandAPICommand startCmd = new CommandAPICommand("start")
            .executes((CommandExecutor) (sender, args) -> commandStart(sender));

        CommandAPICommand endCmd = new CommandAPICommand("end")
            .executes((CommandExecutor) (sender, args) -> commandEnd(sender));

        CommandAPICommand debugCmd = new CommandAPICommand("debug")
            .executes((sender, args) -> {
                sender.sendMessage(String.join(", ",
                    this.getServer().getWorlds().stream().map(World::getName)
                        .collect(Collectors.toUnmodifiableList())));
            });

        CommandAPICommand root = new CommandAPICommand("bingo");
        root.withSubcommand(prepareCmd);
        root.withSubcommand(prepareCmdArg);
        root.withSubcommand(startCmd);
        root.withSubcommand(endCmd);
        if (debug) {
            root.withSubcommand(debugCmd);
        }
        root.register();
    }

    private void commandPrepare(CommandSender sender, String worldCode) {
        if (worldCode == null) {
            worldCode = randomGameCode();
        }
        this.setCurrentGame(new BingoGame(this, worldCode, createBingoPlayers()));
    }

    private void commandStart(CommandSender sender) {
        if (this.getCurrentGame() == null) {
            Messages.basicTellNoGame(sender, "No game is prepared! Use /bingo prepare <gameCode>");
            return;
        }

        this.getCurrentGame().start(sender);
    }

    private void commandEnd(CommandSender sender) {
        if (this.getCurrentGame() == null) {
            Messages.basicTellNoGame(sender, "No game is running!");
            return;
        }

        this.setCurrentGame(null);
    }

    URI getWebsocketUrl(String gameCode) {
        String template = this.getConfig().getString("urls.websocket");
        if (template == null) {
            this.getLogger().severe("No websocket URL is configured!");
            return null;
        }
        try {
            return new URI(template.replace("$code", gameCode));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    URI getGameUrl(String gameCode, BingoPlayer p) {
        String template = this.getConfig().getString("urls.game_player");
        if (template == null) {
            this.getLogger().severe("No game_player URL is configured!");
            return null;
        }
        try {
            return new URI(template
                .replace("$code", URLEncoder.encode(gameCode, StandardCharsets.UTF_8))
                .replace("$player", URLEncoder.encode(p.getName(), StandardCharsets.UTF_8))
            );
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BingoGame getCurrentGame() {
        return currentGame;
    }

    private void setCurrentGame(BingoGame newGame) {
        if (this.currentGame != null) {
            this.currentGame.destroy();
        }
        this.currentGame = newGame;
    }

    private static String randomGameCode() {
        final int numChars = 6;

        Random rand = new Random();
        char[] chars = new char[numChars];
        for (int i = 0; i < numChars; i++) {
            chars[i] = (char) ('A' + rand.nextInt(26));
        }

        return new String(chars);
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
