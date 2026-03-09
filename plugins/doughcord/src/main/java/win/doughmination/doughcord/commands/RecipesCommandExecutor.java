/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;

import win.doughmination.doughcord.CordMain;

public class RecipesCommandExecutor implements CommandExecutor, TabCompleter {

    private static final String RECIPES_URL = "https://modding.doughmination.co.uk/dougminationcord-recipes";
    private final CordMain plugin;

    public RecipesCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Create clickable message
            TextComponent message = new TextComponent(ChatColor.LIGHT_PURPLE + "Click here to view all spawn egg recipes: ");
            TextComponent link = new TextComponent(ChatColor.AQUA + "" + ChatColor.UNDERLINE + RECIPES_URL);
            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, RECIPES_URL));
            message.addExtra(link);

            player.spigot().sendMessage(message);
        } else {
            // Console output
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "View all spawn egg recipes at: " + ChatColor.AQUA + RECIPES_URL);
        }

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
