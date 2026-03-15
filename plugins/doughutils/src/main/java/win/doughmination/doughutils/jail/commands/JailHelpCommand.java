/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.jail.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class JailHelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendRichMessage("<yellow>Jail Commands:</yellow>");
        sender.sendRichMessage("<green>/setjail [x y z]</green> - Set the jail location.");
        sender.sendRichMessage("<green>/jail <player> [time]</green> - Jail a player (time in seconds).");
        sender.sendRichMessage("<green>/unjail <player></green> - Unjail a player.");
        sender.sendRichMessage("<green>/jailreload</green> - Reload the jail configuration.");
        sender.sendRichMessage("<green>/jailhelp</green> - Show this help message.");
        return true;
    }
}
