/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import win.doughmination.jaileddough.JailMain;

public class JailReloadCommand implements CommandExecutor {

    private final JailMain plugin;

    public JailReloadCommand(JailMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        plugin.reloadConfig();
        plugin.loadJailLocation();
        plugin.getLocationStorage().load();
        plugin.getTimerStorage().load();
        sender.sendMessage(ChatColor.GREEN + "JailedDough configuration reloaded!");
        return true;
    }
}