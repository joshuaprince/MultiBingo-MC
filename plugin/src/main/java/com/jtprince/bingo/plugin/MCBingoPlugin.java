package com.jtprince.bingo.plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        this.registerCommands();

        this.saveDefaultConfig();
    }

    private void registerCommands() {
        CommandAPICommand prepareCmd = new CommandAPICommand("prepare")
            .withAliases("p")
            .executes((CommandExecutor) (sender, args) -> commandPrepare(sender, null));
        CommandAPICommand prepareCmdArg = new CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(new StringArgument("gameCode"))
            .executes((CommandExecutor) (sender, args) -> commandPrepare(sender, (String) args[0]));

        CommandAPICommand startCmd = new CommandAPICommand("start")
            .withAliases("s")
            .executes((CommandExecutor) (sender, args) -> commandStart(sender, false));
        CommandAPICommand startDebugCmd = new CommandAPICommand("start")
            .withAliases("s")
            .withArguments(new LiteralArgument("debug"))
            .executes((CommandExecutor) (sender, args) -> commandStart(sender, true));

        CommandAPICommand endCmd = new CommandAPICommand("end")
            .executes((CommandExecutor) (sender, args) -> commandEnd(sender));

        CommandAPICommand goSpawnCmd = new CommandAPICommand("go")
            .withArguments(new LiteralArgument("spawn"))
            .executesPlayer((PlayerCommandExecutor) (sender, args) -> commandGo(sender, null));
        CommandAPICommand goCmd = new CommandAPICommand("go")
            .withArguments(new CustomArgument<>("player", (input) -> {
                if (this.currentGame == null) {
                    throw new CustomArgument.CustomArgumentException(
                        new CustomArgument.MessageBuilder("No games running."));
                }
                BingoPlayer player = this.currentGame.getBingoPlayer(input);
                if (player == null) {
                    throw new CustomArgument.CustomArgumentException(
                        new CustomArgument.MessageBuilder("Unknown BingoPlayer: ").appendArgInput());
                } else {
                    return player;
                }
            }).overrideSuggestions(sender -> {
                if (this.currentGame == null) {
                    return new String[]{};
                }
                return this.currentGame.getPlayers().stream().map(BingoPlayer::getSlugName).toArray(String[]::new);
            }))
            .executesPlayer((PlayerCommandExecutor) (sender, args) -> commandGo(sender, (BingoPlayer) args[0]));

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
        root.withSubcommand(goCmd);
        root.withSubcommand(goSpawnCmd);
        if (MCBConfig.getDebug()) {
            root.withSubcommand(debugCmd);
            root.withSubcommand(startDebugCmd);
        }
        root.register();
    }

    private void commandPrepare(CommandSender sender, String gameCode) {
        if (gameCode == null) {
            gameCode = randomGameCode();
        }
        this.prepareNewGame(gameCode);
    }

    private void commandStart(CommandSender sender, boolean debug) {
        if (this.getCurrentGame() == null) {
            Messages.basicTellNoGame(sender, "No game is prepared! Use /bingo prepare <gameCode>");
            return;
        }

        this.getCurrentGame().start(sender, debug);
    }

    private void commandEnd(CommandSender sender) {
        if (this.getCurrentGame() == null) {
            Messages.basicTellNoGame(sender, "No game is running!");
            return;
        }

        this.destroyCurrentGame();
    }

    private void commandGo(Player sender, @Nullable BingoPlayer destination) {
        if (destination == null) {
            sender.teleport(WorldManager.getSpawnWorld().getSpawnLocation());
        } else {
            sender.teleport(
                destination.getWorldSet().getWorld(World.Environment.NORMAL).getSpawnLocation());
        }
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

    private void prepareNewGame(String gameCode) {
        this.destroyCurrentGame();
        this.currentGame = new BingoGame(this, gameCode, createBingoPlayers());
    }

    private void destroyCurrentGame() {
        if (this.currentGame != null) {
            this.currentGame.destroy();
            this.currentGame = null;
        }
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
