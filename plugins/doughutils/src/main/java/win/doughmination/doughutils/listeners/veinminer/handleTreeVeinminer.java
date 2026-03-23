/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.listeners.veinminer;
import win.doughmination.doughutils.Main;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.*;

public class handleTreeVeinminer {

    private final Main plugin;

    public handleTreeVeinminer(Main plugin) {
        this.plugin = plugin;
    }

    public void handleTreeBreak(Player player, Block block) {
        Material blockType = block.getType();
        if (!isLog(blockType)) return;

        if (isMushroomStem(blockType)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean usingAxe = isAxe(tool.getType());

        Location start = block.getLocation();

        if (!usingAxe) {
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(start, new ItemStack(getLogDrop(blockType), 1));
            return;
        }

        Set<Location> logBlocks = new HashSet<>();
        findConnectedBlocks(start, blockType, logBlocks, plugin.getDoughConfig().getTreeRemoverMaxBlocks());

        for (Location loc : logBlocks) {
            loc.getBlock().setType(Material.AIR);
            loc.getWorld().dropItemNaturally(loc, new ItemStack(getLogDrop(blockType)));
        }

        if (isNetherStem(blockType)) {
            Material wartBlock = getNetherWartBlock(blockType);
            Set<Location> wartBlocks = new HashSet<>();
            for (Location logLoc : logBlocks) {
                findConnectedWartBlocks(logLoc, wartBlock, wartBlocks,
                        plugin.getDoughConfig().getTreeRemoverMaxBlocks());
            }
            for (Location loc : wartBlocks) {
                Block wb = loc.getBlock();
                if (wb.getType() != wartBlock) continue;
                wb.setType(Material.AIR);
                loc.getWorld().dropItemNaturally(loc, new ItemStack(wartBlock, 1));
            }
        }
        // Leaf decay is handled by the external RHLeafDecay plugin.

        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
        player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, start, 20, 1.0, 1.0, 1.0, 0.1);
    }

    private Material getLogDrop(Material blockType) {
        return switch (blockType) {
            case CRIMSON_STEM, STRIPPED_CRIMSON_STEM -> Material.NETHER_WART_BLOCK;
            case WARPED_STEM,  STRIPPED_WARPED_STEM  -> Material.WARPED_WART_BLOCK;
            default -> blockType;
        };
    }

    private Material getNetherWartBlock(Material stem) {
        return switch (stem) {
            case WARPED_STEM, STRIPPED_WARPED_STEM -> Material.WARPED_WART_BLOCK;
            default -> Material.NETHER_WART_BLOCK;
        };
    }

    private boolean isAxe(Material m) {
        return m == Material.WOODEN_AXE || m == Material.STONE_AXE
                || m == Material.COPPER_AXE || m == Material.IRON_AXE
                || m == Material.GOLDEN_AXE || m == Material.DIAMOND_AXE
                || m == Material.NETHERITE_AXE;
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG") || material.name().endsWith("_STEM");
    }

    private boolean isMushroomStem(Material material) {
        return material == Material.MUSHROOM_STEM;
    }

    private boolean isNetherStem(Material material) {
        return material == Material.CRIMSON_STEM || material == Material.STRIPPED_CRIMSON_STEM
                || material == Material.WARPED_STEM || material == Material.STRIPPED_WARPED_STEM;
    }

    private void findConnectedBlocks(Location start, Material blockType, Set<Location> foundBlocks, int maxBlocks) {
        Queue<Location> toCheck = new LinkedList<>();
        toCheck.add(start);

        while (!toCheck.isEmpty() && foundBlocks.size() < maxBlocks) {
            Location current = toCheck.poll();
            if (foundBlocks.contains(current)) continue;
            if (current.getBlock().getType() != blockType) continue;

            foundBlocks.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        toCheck.add(current.clone().add(dx, dy, dz));
                    }
                }
            }
        }
    }

    private void findConnectedWartBlocks(Location start, Material wartType, Set<Location> found, int maxBlocks) {
        Set<Location> visited = new HashSet<>();
        Queue<Location> toCheck = new LinkedList<>();
        toCheck.add(start);

        while (!toCheck.isEmpty() && found.size() < maxBlocks) {
            Location current = toCheck.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            Material type = current.getBlock().getType();
            if (type == wartType) {
                found.add(current);
            } else if (type != Material.AIR && !isNetherStem(type)) {
                continue;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Location neighbour = current.clone().add(dx, dy, dz);
                        if (!visited.contains(neighbour)) toCheck.add(neighbour);
                    }
                }
            }
        }
    }
}
