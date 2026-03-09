/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import win.doughmination.doughcord.CordMain;
import win.doughmination.doughcord.data.PlayerDataManager;

public class veinminerCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;
    private final PlayerDataManager dataManager;

    // In-memory toggles — loaded lazily on first use per player, persisted on change
    private final Map<UUID, Boolean> treeVeinMinerEnabled = new HashMap<>();
    private final Map<UUID, Boolean> oreVeinMinerEnabled  = new HashMap<>();

    public veinminerCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /veinminer <ores|trees>", NamedTextColor.AQUA));
            return true;
        }

        UUID uuid = player.getUniqueId();
        ensureLoaded(uuid);

        switch (args[0].toLowerCase()) {
            case "ores" -> {
                boolean now = !oreVeinMinerEnabled.get(uuid);
                oreVeinMinerEnabled.put(uuid, now);
                dataManager.saveVeinminer(uuid, now, treeVeinMinerEnabled.get(uuid));
                player.sendMessage(
                        Component.text("VeinMining for ores is now ", NamedTextColor.GREEN)
                                .append(now
                                        ? Component.text("enabled", NamedTextColor.GREEN)
                                        : Component.text("disabled", NamedTextColor.RED))
                                .append(Component.text(".", NamedTextColor.GREEN))
                );
            }
            case "trees" -> {
                boolean now = !treeVeinMinerEnabled.get(uuid);
                treeVeinMinerEnabled.put(uuid, now);
                dataManager.saveVeinminer(uuid, oreVeinMinerEnabled.get(uuid), now);
                player.sendMessage(
                        Component.text("VeinMining for trees is now ", NamedTextColor.GREEN)
                                .append(now
                                        ? Component.text("enabled", NamedTextColor.GREEN)
                                        : Component.text("disabled", NamedTextColor.RED))
                                .append(Component.text(".", NamedTextColor.GREEN))
                );
            }
            default -> player.sendMessage(Component.text("Invalid option. Use /veinminer <ores|trees>", NamedTextColor.RED));
        }
        return true;
    }

    public boolean isOreVeinMinerEnabled(UUID uuid) {
        ensureLoaded(uuid);
        return oreVeinMinerEnabled.get(uuid);
    }

    public boolean isTreeVeinMinerEnabled(UUID uuid) {
        ensureLoaded(uuid);
        return treeVeinMinerEnabled.get(uuid);
    }

    /** Loads the player's saved toggles on first access — defaults to true if no file exists yet. */
    private void ensureLoaded(UUID uuid) {
        if (!oreVeinMinerEnabled.containsKey(uuid)) {
            oreVeinMinerEnabled.put(uuid,  dataManager.loadVeinminerOres(uuid));
            treeVeinMinerEnabled.put(uuid, dataManager.loadVeinminerTrees(uuid));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (String opt : Arrays.asList("ores", "trees")) {
                if (opt.startsWith(partial)) completions.add(opt);
            }
            return completions;
        }
        return List.of();
    }
}
