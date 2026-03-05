/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord.commands.chests;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import win.doughmination.doughminationcord.CordMain;
import win.doughmination.doughminationcord.data.VChestDataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /chest <echest|vchest> <player>
 *
 * Opens a read-only snapshot of the target player's ender chest or VIP chest
 * for moderation purposes.
 *
 * Requires permission: dough.chest.inspect
 */
public class chestsCommandExecutor implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "dough.chest.inspect";

    private final CordMain plugin;
    private final VChestDataManager vChestDataManager;

    public chestsCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        this.vChestDataManager = new VChestDataManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to inspect player chests!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /chest <echest|vchest> <player>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];

        if (!subCommand.equals("echest") && !subCommand.equals("vchest")) {
            sender.sendMessage(ChatColor.RED + "Unknown chest type '" + args[0] + "'. Use: echest or vchest");
            return true;
        }

        // Must be a player to open an inventory GUI
        if (!(sender instanceof Player inspector)) {
            sender.sendMessage(ChatColor.RED + "Console cannot open inventory GUIs. Use an in-game account.");
            return true;
        }

        // Resolve target — online first, then offline
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID;
        String resolvedName;

        if (target != null) {
            targetUUID = target.getUniqueId();
            resolvedName = target.getName();
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (!offline.hasPlayedBefore()) {
                inspector.sendMessage(ChatColor.RED + "Player '" + targetName + "' has never joined this server.");
                return true;
            }
            targetUUID = offline.getUniqueId();
            resolvedName = offline.getName() != null ? offline.getName() : targetName;
        }

        switch (subCommand) {
            case "echest" -> openEChest(inspector, target, targetUUID, resolvedName);
            case "vchest" -> openVChest(inspector, targetUUID, resolvedName);
        }

        return true;
    }

    // -----------------------------------------------------------------------
    // Ender Chest — player must be online (ender chest is only in memory)
    // -----------------------------------------------------------------------

    private void openEChest(Player inspector, Player target, UUID targetUUID, String targetName) {
        if (target == null || !target.isOnline()) {
            inspector.sendMessage(ChatColor.RED + targetName + " must be online to inspect their ender chest.");
            return;
        }

        // Snapshot so the inspector cannot accidentally modify live contents
        Inventory snapshot = Bukkit.createInventory(
                null,
                target.getEnderChest().getSize(),
                ChatColor.DARK_RED + "⚑ [INSPECT] " + targetName + "'s E-Chest"
        );
        snapshot.setContents(target.getEnderChest().getContents().clone());

        inspector.openInventory(snapshot);
        inspector.sendMessage(ChatColor.GOLD + "Inspecting " + ChatColor.YELLOW + targetName
                + ChatColor.GOLD + "'s ender chest. " + ChatColor.GRAY + "(Read-only snapshot)");

        plugin.getLogger().info("[chest] " + inspector.getName()
                + " inspected " + targetName + "'s echest (UUID: " + targetUUID + ")");
    }

    // -----------------------------------------------------------------------
    // VIP Chest — loaded from disk, works for offline players too
    // -----------------------------------------------------------------------

    private void openVChest(Player inspector, UUID targetUUID, String targetName) {
        Inventory snapshot = Bukkit.createInventory(
                null,
                54,
                ChatColor.DARK_RED + "⚑ [INSPECT] " + targetName + "'s V-Chest"
        );
        vChestDataManager.loadInto(targetUUID, snapshot);

        inspector.openInventory(snapshot);
        inspector.sendMessage(ChatColor.GOLD + "Inspecting " + ChatColor.YELLOW + targetName
                + ChatColor.GOLD + "'s VIP chest. " + ChatColor.GRAY + "(Read-only snapshot)");

        plugin.getLogger().info("[chest] " + inspector.getName()
                + " inspected " + targetName + "'s vchest (UUID: " + targetUUID + ")");
    }

    // -----------------------------------------------------------------------
    // Tab completion
    // -----------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) return List.of();

        if (args.length == 1) {
            return Arrays.asList("echest", "vchest").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}