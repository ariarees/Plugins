/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.chests;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import win.doughmination.doughcord.CordMain;
import win.doughmination.doughcord.data.VChestDataManager;

public class VChestCommandExecutor implements CommandExecutor, TabCompleter, Listener {

    private final Map<UUID, Inventory> vipInventories = new HashMap<>();
    private final CordMain plugin;
    private final VChestDataManager dataManager;

    public VChestCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        this.dataManager = new VChestDataManager(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("dough.vchest")) {
            player.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        UUID uuid = player.getUniqueId();
        Inventory vipChest = vipInventories.computeIfAbsent(uuid, k -> {
            Inventory inv = Bukkit.createInventory(player, 54,
                    Component.text("✦ " + player.getName() + "'s VIP Chest", NamedTextColor.LIGHT_PURPLE));
            dataManager.loadInto(uuid, inv);
            return inv;
        });

        player.openInventory(vipChest);
        return true;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Inventory vipChest = vipInventories.get(uuid);
        if (vipChest != null && event.getInventory().equals(vipChest)) {
            dataManager.save(uuid, vipChest);
        }
    }

    /** Called from CordMain#onDisable to flush all open inventories. */
    public void saveAll() {
        vipInventories.forEach(dataManager::save);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
