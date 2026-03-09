/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands;

import win.doughmination.doughcord.CordMain;

import org.bukkit.ChatColor;
import org.bukkit.command.TabCompleter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GrowthShrinkPotionCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;
    private final NamespacedKey potionTypeKey;

    public GrowthShrinkPotionCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        // Create a new namespaced key for custom potion types
        potionTypeKey = new NamespacedKey(plugin, "potionType");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can get potions!");
            return true;
        }
        Player player = (Player) sender;
        String cmdName = command.getName().toLowerCase();
        ItemStack potion = new ItemStack(Material.POTION, 1);
        ItemMeta meta = potion.getItemMeta();
        if (meta == null) return true;

        if (cmdName.equals("growthpotion")) {
            meta.setDisplayName(ChatColor.GOLD + "Growth Potion");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Drink to grow larger!");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(potionTypeKey, PersistentDataType.STRING, "growth");
        } else if (cmdName.equals("shrinkpotion")) {
            meta.setDisplayName(ChatColor.AQUA + "Shrink Potion");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Drink to grow smaller!");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(potionTypeKey, PersistentDataType.STRING, "shrink");
        } else {
            player.sendMessage(ChatColor.RED + "Unknown potion command.");
            return true;
        }
        potion.setItemMeta(meta);
        player.getInventory().addItem(potion);
        player.sendMessage(ChatColor.GREEN + "You have received a " + meta.getDisplayName() + ChatColor.GREEN + "!");
        return true;
    }


    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
