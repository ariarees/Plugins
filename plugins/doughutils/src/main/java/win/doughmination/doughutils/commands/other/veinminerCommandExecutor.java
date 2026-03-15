/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.other;
import win.doughmination.doughutils.Main;
import win.doughmination.doughutils.data.PlayerDataManager;

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




public class veinminerCommandExecutor implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final PlayerDataManager dataManager;

    // In-memory toggles — loaded lazily on first use per player, persisted on change
    private final Map<UUID, Boolean> treeVeinMinerEnabled = new HashMap<>();
    private final Map<UUID, Boolean> oreVeinMinerEnabled  = new HashMap<>();

    public veinminerCommandExecutor(Main plugin) {
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
        return oreVeinMinerEnabled.getOrDefault(uuid, true);
    }

    public boolean isTreeVeinMinerEnabled(UUID uuid) {
        return treeVeinMinerEnabled.getOrDefault(uuid, true);
    }

    /** Called on player join — loads their saved toggles from disk into memory. */
    public void loadForPlayer(UUID uuid) {
        oreVeinMinerEnabled.put(uuid,  dataManager.loadVeinminerOres(uuid));
        treeVeinMinerEnabled.put(uuid, dataManager.loadVeinminerTrees(uuid));
    }

    /** Called on player quit — flushes current in-memory toggles to disk and clears them. */
    public void saveAndUnloadForPlayer(UUID uuid) {
        boolean ores  = oreVeinMinerEnabled.getOrDefault(uuid, true);
        boolean trees = treeVeinMinerEnabled.getOrDefault(uuid, true);
        dataManager.saveVeinminer(uuid, ores, trees);
        oreVeinMinerEnabled.remove(uuid);
        treeVeinMinerEnabled.remove(uuid);
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
