/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;

import win.doughmination.doughcord.CordMain;
import win.doughmination.api.LibMain;

import java.util.UUID;

public class baseCommandExecutor implements CommandExecutor, TabCompleter {
    private final CordMain plugin;

    public baseCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(player, "base")) {
            player.sendMessage(ChatColor.RED + "You cannot teleport while jailed!");
            return true;
        }

        UUID playerUUID = player.getUniqueId();
        if (!plugin.getBases().containsKey(playerUUID)) {
            player.sendMessage(ChatColor.RED + "You have not set a base!");
            return true;
        }

        Location baseLocation = plugin.getBases().get(playerUUID);
        if (player.teleport(baseLocation)) {
            String soundName = plugin.getConfig().getString("sounds.base", "ENTITY_ENDERMAN_TELEPORT");
            Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase()));
            if (sound != null) {
                player.playSound(baseLocation, sound, 1.0f, 1.0f);
            }
            player.sendMessage(ChatColor.GREEN + "Teleporting to your base!");
        } else {
            player.sendMessage(ChatColor.GOLD + "Teleport failed!");
        }

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
