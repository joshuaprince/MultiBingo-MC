package com.jtprince.bingo.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class MCBingoPlugin extends JavaPlugin {
    WorldManager wmgr;

    @Override
    public void onEnable() {
        wmgr = new WorldManager(this);
        this.getServer().getPluginManager().registerEvents(wmgr, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                sender.sendMessage("Your world: " + p.getWorld().getName());
            }
            return false;
        }

        if (args[0].equalsIgnoreCase("generate")) {
            sender.sendMessage("Generating worlds...");
            wmgr.createWorlds(args[1]);
            sender.sendMessage("Worlds generated! Type /bingo go " + args[1] + " to go there.");
            return true;
        }

        if (args[0].equalsIgnoreCase("go")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You must be a player to use this command.");
                return true;
            }

            Player p = (Player) sender;
            wmgr.putInWorld(p, args[1]);
            return true;
        }

        return false;
    }
}
