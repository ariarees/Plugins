/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.jail.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import win.doughmination.doughutils.Main;

public class JailReloadCommand implements CommandExecutor {

    private final Main plugin;

    public JailReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("jailplugin.reload")) {
            sender.sendRichMessage("<red>You don't have permission to use this command!</red>");
            return true;
        }

        plugin.reloadConfig();
        plugin.loadJailLocation();
        plugin.getLocationStorage().load();
        plugin.getTimerStorage().load();
        sender.sendRichMessage("<green>Jail configuration reloaded!</green>");
        return true;
    }
}
