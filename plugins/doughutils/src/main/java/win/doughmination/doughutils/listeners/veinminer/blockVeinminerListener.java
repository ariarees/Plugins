/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.listeners.veinminer;
import win.doughmination.doughutils.Main;

import win.doughmination.doughutils.commands.other.veinminerCommandExecutor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.UUID;

public class blockVeinminerListener implements Listener {
    private final Main plugin;
    private final handleOreVeinminer oreVeinminer;
    private final handleTreeVeinminer treeVeinminer;
    private final veinminerCommandExecutor veinMinerExecutor;

    public blockVeinminerListener(Main plugin, veinminerCommandExecutor veinMinerExecutor) {
        this.plugin = plugin;
        this.oreVeinminer = new handleOreVeinminer(plugin);
        this.treeVeinminer = new handleTreeVeinminer(plugin);
        this.veinMinerExecutor = veinMinerExecutor; // Assign the passed executor
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        Block block = event.getBlock();

        // Get the veinminer toggles
        CommandExecutor executor = plugin.getCommand("veinminer").getExecutor();
        if (executor instanceof veinminerCommandExecutor veinMinerExecutor) {
            // Check if the player has vein-mining enabled for ores
            if (veinMinerExecutor.isOreVeinMinerEnabled(playerUUID) && isOre(block.getType())) {
                oreVeinminer.handleOreBreak(player, block);
                event.setCancelled(true); // Cancel the default block break
                return;
            }

            // Check if the player has vein-mining enabled for trees
            if (veinMinerExecutor.isTreeVeinMinerEnabled(playerUUID) && isLog(block.getType())) {
                treeVeinminer.handleTreeBreak(player, block);
                event.setCancelled(true); // Cancel the default block break
            }
        } else {
            plugin.getLogger().warning("The veinminer command executor is not properly set!");
        }
    }

    private boolean isOre(Material material) {
        return material.name().endsWith("_ORE") || material.name().endsWith("_DEBRIS");
    }

    private boolean isLog(Material material) {
        // Mushroom stems are explicitly excluded — they must not veinmine
        if (material == Material.MUSHROOM_STEM) return false;
        return material.name().endsWith("_LOG") || material.name().endsWith("_STEM");
    }
}
