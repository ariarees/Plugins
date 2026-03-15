/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.chests;

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

import win.doughmination.doughutils.Main;
import win.doughmination.doughutils.data.VChestDataManager;

public class VChestCommandExecutor implements CommandExecutor, TabCompleter, Listener {

    /**
     * Tracks inventories that are currently open so we can match them in
     * InventoryCloseEvent. Entries are added when a player opens their chest
     * and removed as soon as it closes — we never hold onto them between sessions.
     */
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final Main plugin;
    private final VChestDataManager dataManager;

    public VChestCommandExecutor(Main plugin) {
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

        // If the player already has their chest open (e.g. double-clicked the command),
        // just re-open the same inventory instance rather than creating a second one.
        if (openInventories.containsKey(uuid)) {
            player.openInventory(openInventories.get(uuid));
            return true;
        }

        // Always build a fresh inventory and populate it from disk.
        Inventory inv = Bukkit.createInventory(player, 54,
                Component.text("✦ " + player.getName() + "'s VIP Chest", NamedTextColor.LIGHT_PURPLE));
        dataManager.loadInto(uuid, inv);

        openInventories.put(uuid, inv);
        player.openInventory(inv);
        return true;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Inventory inv = openInventories.remove(uuid); // always remove — chest is closed
        if (inv != null && event.getInventory().equals(inv)) {
            dataManager.save(uuid, inv);
        }
    }

    /**
     * Called from CordMain#onDisable to flush any inventories still open at shutdown.
     * This is a last-resort safety net; normal saves happen in onInventoryClose.
     */
    public void saveAll() {
        openInventories.forEach(dataManager::save);
        openInventories.clear();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

