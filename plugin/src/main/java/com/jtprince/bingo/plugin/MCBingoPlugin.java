package com.jtprince.bingo.plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;
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
        ArrayList<Player> players = new ArrayList<>(this.getServer().getOnlinePlayers());
        if (worldCode == null) {
            worldCode = randomGameCode();
        }
        this.setCurrentGame(new BingoGame(this, worldCode, players));
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

    URI getGameUrl(String gameCode, Player p) {
        String template = this.getConfig().getString("urls.game_player");
        if (template == null) {
            this.getLogger().severe("No game_player URL is configured!");
            return null;
        }
        try {
            return new URI(template.replace("$code", gameCode).replace("$player", p.getName()));
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
}
