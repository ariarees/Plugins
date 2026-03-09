/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class JailHelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "JailedDough Commands:");
        sender.sendMessage(ChatColor.GREEN + "/setjail [x y z]"    + ChatColor.WHITE + " - Set the jail location.");
        sender.sendMessage(ChatColor.GREEN + "/setunjail [x y z]"  + ChatColor.WHITE + " - Set the unjail location.");
        sender.sendMessage(ChatColor.GREEN + "/jail <player> [time]" + ChatColor.WHITE + " - Jail a player (time in seconds).");
        sender.sendMessage(ChatColor.GREEN + "/unjail <player>"    + ChatColor.WHITE + " - Unjail a player.");
        sender.sendMessage(ChatColor.GREEN + "/jailreload"         + ChatColor.WHITE + " - Reload the plugin configuration.");
        sender.sendMessage(ChatColor.GREEN + "/jailhelp"           + ChatColor.WHITE + " - Show this help message.");
        return true;
    }
}