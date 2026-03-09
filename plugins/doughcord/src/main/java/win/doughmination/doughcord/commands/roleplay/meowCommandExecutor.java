/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.roleplay;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import win.doughmination.doughcord.CordMain;

public class meowCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;
    private final Random random;

    public meowCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        List<String> meowMessages = plugin.getConfig().getStringList("meow-command.messages");
        if (meowMessages.isEmpty()) {
            player.sendMessage(Component.text("No kitty messages are set in the config!", NamedTextColor.RED));
            return true;
        }

        String randomMessage = meowMessages.get(random.nextInt(meowMessages.size()));

        plugin.getServer().broadcast(
                Component.text(player.getName() + " says " + randomMessage, NamedTextColor.LIGHT_PURPLE)
        );

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
