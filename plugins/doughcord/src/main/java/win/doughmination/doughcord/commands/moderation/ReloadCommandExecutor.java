/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.moderation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import win.doughmination.doughcord.CordMain;

public class ReloadCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final CordMain plugin;

    public ReloadCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dough.reload")) {
            sender.sendMessage(Component.text("You do not have permission to reload the plugin!", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Reloading configuration...", NamedTextColor.YELLOW));
        try {
            plugin.reloadConfig();
            sender.sendMessage(Component.text("Configuration reloaded successfully!", NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("An error occurred while reloading the configuration.", NamedTextColor.RED));
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
