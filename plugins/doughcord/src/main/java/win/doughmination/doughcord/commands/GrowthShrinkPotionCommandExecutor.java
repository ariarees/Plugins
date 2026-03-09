/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import win.doughmination.doughcord.CordMain;
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
        potionTypeKey = new NamespacedKey(plugin, "potionType");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can get potions!");
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        ItemStack potion = new ItemStack(Material.POTION, 1);
        ItemMeta meta = potion.getItemMeta();
        if (meta == null) return true;

        if (cmdName.equals("growthpotion")) {
            meta.displayName(Component.text("Growth Potion", NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Drink to grow larger!", NamedTextColor.GRAY));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(potionTypeKey, PersistentDataType.STRING, "growth");
        } else if (cmdName.equals("shrinkpotion")) {
            meta.displayName(Component.text("Shrink Potion", NamedTextColor.AQUA));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Drink to grow smaller!", NamedTextColor.GRAY));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(potionTypeKey, PersistentDataType.STRING, "shrink");
        } else {
            player.sendMessage(Component.text("Unknown potion command.", NamedTextColor.RED));
            return true;
        }

        potion.setItemMeta(meta);
        player.getInventory().addItem(potion);
        player.sendMessage(
            Component.text("You have received a ", NamedTextColor.GREEN)
                .append(meta.displayName())
                .append(Component.text("!", NamedTextColor.GREEN))
        );
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
