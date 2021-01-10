package com.jtprince.bingo.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;

public class MCBingoPlugin extends JavaPlugin {
    boolean debug = false;
    WorldManager worldManager;
    BingoGame currentGame;

    @Override
    public void onEnable() {
        worldManager = new WorldManager(this);
        this.getServer().getPluginManager().registerEvents(worldManager, this);

        if (this.getConfig().getBoolean("debug", false)) {
            this.getLogger().setLevel(Level.FINER);
            this.debug = true;
        }

        this.saveDefaultConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                sender.sendMessage("Your world: " + p.getWorld().getName());
            }
            return false;
        }

        if (args[0].equalsIgnoreCase("generate")) {
            if (args.length < 2) return false;
            return this.commandGenerate(sender, args[1]);
        }

        if (args[0].equalsIgnoreCase("go")) {
            if (args.length < 2) return false;
            return this.commandGo(sender, args[1]);
        }

        if (args[0].equalsIgnoreCase("prepare")) {
            if (args.length < 2) return false;
            return this.commandPrepare(sender, args[1]);
        }

        if (args[0].equalsIgnoreCase("start")) {
            return this.commandStart(sender);
        }

        return false;
    }

    protected boolean commandGenerate(CommandSender sender, String worldCode) {
        // TODO consider deletion or format messages
        sender.sendMessage("Generating worlds...");
        worldManager.createWorlds(worldCode, worldCode);
        sender.sendMessage("Worlds generated! Type /bingo go " + worldCode + " to go there.");
        return true;
    }

    protected boolean commandGo(CommandSender sender, String worldCode) {
        // TODO consider deletion or format messages
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command.");
            return true;
        }

        Player p = (Player) sender;
        return worldManager.putInWorld(p, worldCode);
    }

    protected boolean commandPrepare(CommandSender sender, String worldCode) {
        this.currentGame = new BingoGame(this, worldCode);

        ArrayList<Player> players = new ArrayList<>(this.getServer().getOnlinePlayers());
        this.currentGame.prepare(players);

        return true;
    }

    protected boolean commandStart(CommandSender sender) {
        if (this.currentGame == null) {
            // TODO format message
            sender.sendMessage("No game is prepared! Use /bingo prepare <gameID>");
            return true;
        }

        this.currentGame.start();

        return true;
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
}
