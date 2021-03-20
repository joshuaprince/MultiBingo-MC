package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.player.BingoPlayer;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.stream.Collectors;

class MCBCommands {
    private final MCBingoPlugin plugin;

    MCBCommands(@NotNull MCBingoPlugin plugin) {
        this.plugin = plugin;
    }

    void registerCommands() {
        MultiLiteralArgument shapeArg = new MultiLiteralArgument("square", "hexagon");
        IntegerArgument diffArg = new IntegerArgument("difficulty", 0, 9);
        GreedyStringArgument forcedArg = new GreedyStringArgument("forcedGoals");

        // All-default form
        CommandAPICommand prepareCmd = new CommandAPICommand("prepare")
            .withAliases("p")
            .executes((CommandExecutor) (sender, args) -> commandPrepare(sender, new GameSettings()));
        // Game code form
        CommandAPICommand prepareCmdGameCode = new CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(new StringArgument("gameCode"))
            .executes((sender, args) -> {
                GameSettings settings = new GameSettings();
                settings.gameCode = (String) args[0];
                commandPrepare(sender, settings);
            });
        // Shape-only form
        CommandAPICommand prepareCmdShaped = new CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(shapeArg)
            .executes((sender, args) -> {
                GameSettings settings = new GameSettings();
                settings.shape = (String) args[0];
                commandPrepare(sender, settings);
            });
        // +Difficulty
        CommandAPICommand prepareCmdShapedDiff = new CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(shapeArg, diffArg)
            .executes((sender, args) -> {
                GameSettings settings = new GameSettings();
                settings.shape = (String) args[0];
                settings.boardDifficulty = (int) args[1];
                commandPrepare(sender, settings);
            });
        // +Forced goals
        CommandAPICommand prepareCmdShapedDiffForced = new CommandAPICommand("prepare")
            .withAliases("p")
            .withArguments(shapeArg, diffArg, forcedArg)
            .executes((sender, args) -> {
                GameSettings settings = new GameSettings();
                settings.shape = (String) args[0];
                settings.boardDifficulty = (int) args[1];
                settings.forcedGoals = ((String) args[2]).split(" ");
                commandPrepare(sender, settings);
            });

        CommandAPICommand startCmd = new CommandAPICommand("start")
            .withAliases("s")
            .executes((CommandExecutor) (sender, args) -> commandStart(sender));

        CommandAPICommand endCmd = new CommandAPICommand("end")
            .executes((CommandExecutor) (sender, args) -> commandEnd(sender));

        CommandAPICommand goSpawnCmd = new CommandAPICommand("go")
            .withArguments(new LiteralArgument("spawn"))
            .executesPlayer((PlayerCommandExecutor) (sender, args) -> commandGo(sender, null));
        CommandAPICommand goCmd = new CommandAPICommand("go")
            .withArguments(new CustomArgument<>("player", (input) -> {
                if (this.plugin.getCurrentGame() == null) {
                    throw new CustomArgument.CustomArgumentException(
                        new CustomArgument.MessageBuilder("No games running."));
                }
                BingoPlayer player = this.plugin.getCurrentGame().playerManager.getLocalPlayer(input);
                if (player == null) {
                    throw new CustomArgument.CustomArgumentException(
                        new CustomArgument.MessageBuilder("Unknown BingoPlayer: ").appendArgInput());
                } else {
                    return player;
                }
            }).overrideSuggestions(sender -> {
                if (this.plugin.getCurrentGame() == null) {
                    return new String[]{};
                }
                return this.plugin.getCurrentGame().playerManager.getLocalPlayers().stream()
                    .map(BingoPlayer::getSlugName).toArray(String[]::new);
            }))
            .executesPlayer((PlayerCommandExecutor) (sender, args) -> commandGo(sender, (BingoPlayer) args[0]));

        CommandAPICommand debugCmd = new CommandAPICommand("debug")
            .executes((sender, args) -> {
                sender.sendMessage(String.join(", ",
                    this.plugin.getServer().getWorlds().stream().map(World::getName)
                        .collect(Collectors.toUnmodifiableList())));
            });

        CommandAPICommand root = new CommandAPICommand("bingo");
        root.withSubcommand(prepareCmd);
        root.withSubcommand(prepareCmdGameCode);
        root.withSubcommand(prepareCmdShaped);
        root.withSubcommand(prepareCmdShapedDiff);
        root.withSubcommand(prepareCmdShapedDiffForced);
        root.withSubcommand(startCmd);
        root.withSubcommand(endCmd);
        root.withSubcommand(goCmd);
        root.withSubcommand(goSpawnCmd);
        if (MCBConfig.getDebug()) {
            root.withSubcommand(debugCmd);
        }
        root.register();
    }

    private void commandPrepare(@NotNull CommandSender sender, @NotNull GameSettings settings) {
        this.plugin.prepareNewGame(sender, settings);
    }

    private void commandStart(CommandSender sender) {
        BingoGame game = this.plugin.getCurrentGame();
        if (game == null) {
            Messages.basicTellNoGame(sender, "No game is prepared! Use /bingo prepare <gameCode>");
            return;
        }

        if (game.state != BingoGame.State.READY) {
            game.messages.basicTell(sender, "Game is not yet ready to be started!");
            return;
        }

        game.wsClient.sendStartGame();
    }

    private void commandEnd(CommandSender sender) {
        if (this.plugin.getCurrentGame() == null) {
            Messages.basicTellNoGame(sender, "No game is running!");
            return;
        }

        this.plugin.getCurrentGame().wsClient.sendEndGame();

        this.plugin.destroyCurrentGame();  // Fail-safe in case the above remote call fails
    }

    private void commandGo(Player sender, @Nullable BingoPlayer destination) {
        if (destination == null) {
            sender.teleport(WorldManager.getSpawnWorld().getSpawnLocation());
        } else {
            sender.teleport(
                MCBingoPlugin.instance().getCurrentGame().playerManager.getWorldSet(destination)
                    .getWorld(World.Environment.NORMAL).getSpawnLocation());
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
}
