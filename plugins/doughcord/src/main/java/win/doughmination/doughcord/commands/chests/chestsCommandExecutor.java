/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.chests;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import win.doughmination.doughcord.CordMain;
import win.doughmination.doughcord.data.VChestDataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /chest <echest|vchest|inv> <player> [take <slot>]
 *
 * Opens a read-only snapshot of a target player's ender chest, VIP chest,
 * or live inventory for moderation purposes.
 * The optional "take" action removes an item from the specified slot and logs
 * the action, for use when illegal items are found.
 *
 * Requires permission: dough.chest.inspect
 * Take action requires: dough.chest.take
 */
public class chestsCommandExecutor implements CommandExecutor, TabCompleter {

    private static final String PERM_INSPECT = "dough.chest.inspect";
    private static final String PERM_TAKE    = "dough.chest.take";

    private final CordMain plugin;
    private final VChestDataManager vChestDataManager;

    public chestsCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        this.vChestDataManager = new VChestDataManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERM_INSPECT)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to inspect player inventories!");
            return true;
        }

        // /chest <echest|vchest|inv> <player> [take <slot>]
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /chest <echest|vchest|inv> <player> [take <slot>]");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];

        if (!subCommand.equals("echest") && !subCommand.equals("vchest") && !subCommand.equals("inv")) {
            sender.sendMessage(ChatColor.RED + "Unknown type '" + args[0] + "'. Use: echest, vchest, or inv");
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

        // Check for optional take action: /chest <type> <player> take <slot>
        boolean isTake = args.length >= 4 && args[2].equalsIgnoreCase("take");
        int takeSlot = -1;

        if (isTake) {
            if (!inspector.hasPermission(PERM_TAKE)) {
                inspector.sendMessage(ChatColor.RED + "You do not have permission to take items from player inventories!");
                return true;
            }
            try {
                takeSlot = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                inspector.sendMessage(ChatColor.RED + "Invalid slot number '" + args[3] + "'. Must be an integer.");
                return true;
            }
        }

        switch (subCommand) {
            case "echest" -> {
                if (isTake) takeFromEChest(inspector, target, targetUUID, resolvedName, takeSlot);
                else         openEChest(inspector, target, targetUUID, resolvedName);
            }
            case "vchest" -> {
                if (isTake) takeFromVChest(inspector, targetUUID, resolvedName, takeSlot);
                else         openVChest(inspector, targetUUID, resolvedName);
            }
            case "inv" -> {
                if (isTake) takeFromInv(inspector, target, targetUUID, resolvedName, takeSlot);
                else         openInv(inspector, target, targetUUID, resolvedName);
            }
        }

        return true;
    }

    // -----------------------------------------------------------------------
    // Ender Chest — player must be online
    // -----------------------------------------------------------------------

    private void openEChest(Player inspector, Player target, UUID targetUUID, String targetName) {
        if (target == null || !target.isOnline()) {
            inspector.sendMessage(ChatColor.RED + targetName + " must be online to inspect their ender chest.");
            return;
        }

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

    private void takeFromEChest(Player inspector, Player target, UUID targetUUID, String targetName, int slot) {
        if (target == null || !target.isOnline()) {
            inspector.sendMessage(ChatColor.RED + targetName + " must be online to take from their ender chest.");
            return;
        }

        Inventory echest = target.getEnderChest();
        if (slot < 0 || slot >= echest.getSize()) {
            inspector.sendMessage(ChatColor.RED + "Slot " + slot + " is out of range (0-" + (echest.getSize() - 1) + ").");
            return;
        }

        ItemStack item = echest.getItem(slot);
        if (item == null || item.getType().isAir()) {
            inspector.sendMessage(ChatColor.YELLOW + "Slot " + slot + " in " + targetName + "'s ender chest is empty.");
            return;
        }

        String itemDesc = item.getAmount() + "x " + item.getType().name();
        echest.setItem(slot, null);

        inspector.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + itemDesc
                + ChatColor.GREEN + " from " + ChatColor.YELLOW + targetName
                + ChatColor.GREEN + "'s ender chest (slot " + slot + ").");
        plugin.getLogger().warning("[chest:take] " + inspector.getName()
                + " removed " + itemDesc + " from " + targetName
                + "'s echest slot " + slot + " (UUID: " + targetUUID + ")");

        target.sendMessage(ChatColor.RED + "A staff member has removed an item from your ender chest.");
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

    private void takeFromVChest(Player inspector, UUID targetUUID, String targetName, int slot) {
        if (slot < 0 || slot >= 54) {
            inspector.sendMessage(ChatColor.RED + "Slot " + slot + " is out of range (0-53).");
            return;
        }

        // Load a live copy, remove the item, then save it back
        Inventory live = Bukkit.createInventory(null, 54, "vchest_edit");
        vChestDataManager.loadInto(targetUUID, live);

        ItemStack item = live.getItem(slot);
        if (item == null || item.getType().isAir()) {
            inspector.sendMessage(ChatColor.YELLOW + "Slot " + slot + " in " + targetName + "'s VIP chest is empty.");
            return;
        }

        String itemDesc = item.getAmount() + "x " + item.getType().name();
        live.setItem(slot, null);
        vChestDataManager.save(targetUUID, live);

        inspector.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + itemDesc
                + ChatColor.GREEN + " from " + ChatColor.YELLOW + targetName
                + ChatColor.GREEN + "'s VIP chest (slot " + slot + ").");
        plugin.getLogger().warning("[chest:take] " + inspector.getName()
                + " removed " + itemDesc + " from " + targetName
                + "'s vchest slot " + slot + " (UUID: " + targetUUID + ")");

        Player online = Bukkit.getPlayer(targetUUID);
        if (online != null && online.isOnline()) {
            online.sendMessage(ChatColor.RED + "A staff member has removed an item from your VIP chest.");
        }
    }

    // -----------------------------------------------------------------------
    // Player Inventory — player must be online
    // -----------------------------------------------------------------------

    private void openInv(Player inspector, Player target, UUID targetUUID, String targetName) {
        if (target == null || !target.isOnline()) {
            inspector.sendMessage(ChatColor.RED + targetName + " must be online to inspect their inventory.");
            return;
        }

        // 36-slot snapshot of the main inventory (hotbar + storage, excludes armour/offhand)
        Inventory snapshot = Bukkit.createInventory(
                null,
                36,
                ChatColor.DARK_RED + "⚑ [INSPECT] " + targetName + "'s Inv"
        );
        ItemStack[] mainContents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            mainContents[i] = target.getInventory().getItem(i);
        }
        snapshot.setContents(mainContents);

        inspector.openInventory(snapshot);
        inspector.sendMessage(ChatColor.GOLD + "Inspecting " + ChatColor.YELLOW + targetName
                + ChatColor.GOLD + "'s inventory. " + ChatColor.GRAY + "(Read-only snapshot — slots 0-35 main inv, 36-39 armour, 40 offhand)");
        plugin.getLogger().info("[chest] " + inspector.getName()
                + " inspected " + targetName + "'s inventory (UUID: " + targetUUID + ")");
    }

    private void takeFromInv(Player inspector, Player target, UUID targetUUID, String targetName, int slot) {
        if (target == null || !target.isOnline()) {
            inspector.sendMessage(ChatColor.RED + targetName + " must be online to take from their inventory.");
            return;
        }

        // 0-35: main inventory, 36-39: armour, 40: offhand
        if (slot < 0 || slot > 40) {
            inspector.sendMessage(ChatColor.RED + "Slot " + slot + " is out of range (0-40). Main: 0-35, Armour: 36-39, Offhand: 40");
            return;
        }

        ItemStack item = target.getInventory().getItem(slot);
        if (item == null || item.getType().isAir()) {
            inspector.sendMessage(ChatColor.YELLOW + "Slot " + slot + " in " + targetName + "'s inventory is empty.");
            return;
        }

        String itemDesc = item.getAmount() + "x " + item.getType().name();
        target.getInventory().setItem(slot, null);

        inspector.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + itemDesc
                + ChatColor.GREEN + " from " + ChatColor.YELLOW + targetName
                + ChatColor.GREEN + "'s inventory (slot " + slot + ").");
        plugin.getLogger().warning("[chest:take] " + inspector.getName()
                + " removed " + itemDesc + " from " + targetName
                + "'s inventory slot " + slot + " (UUID: " + targetUUID + ")");

        target.sendMessage(ChatColor.RED + "A staff member has removed an item from your inventory.");
    }

    // -----------------------------------------------------------------------
    // Tab completion
    // -----------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM_INSPECT)) return List.of();

        if (args.length == 1) {
            return Arrays.asList("echest", "vchest", "inv").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && sender.hasPermission(PERM_TAKE)) {
            return List.of("take").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[2].equalsIgnoreCase("take") && sender.hasPermission(PERM_TAKE)) {
            String type = args[0].toLowerCase();
            int maxSlot = switch (type) {
                case "echest" -> 26;
                case "vchest" -> 53;
                case "inv"    -> 40;
                default       -> 35;
            };
            List<String> slots = new ArrayList<>();
            for (int i = 0; i <= maxSlot; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(args[3])) slots.add(s);
            }
            return slots;
        }

        return new ArrayList<>();
    }
}
