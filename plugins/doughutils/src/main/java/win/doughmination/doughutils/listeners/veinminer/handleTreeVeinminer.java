/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.listeners.veinminer;
import win.doughmination.doughutils.Main;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.*;

public class handleTreeVeinminer {

    private final Main plugin;
    private final handleLeafDecay leafDecay;

    public handleTreeVeinminer(Main plugin) {
        this.plugin = plugin;
        this.leafDecay = new handleLeafDecay(plugin);
    }

    public void handleTreeBreak(Player player, Block block) {
        Material blockType = block.getType();
        if (!isLog(blockType)) return;

        // Mushroom stems are explicitly excluded from veinmining
        if (isMushroomStem(blockType)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean usingAxe = isAxe(tool.getType());

        Location start = block.getLocation();

        if (!usingAxe) {
            // Fists (or any non-axe): break just the one block, drop appropriately, no veinmine
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(start, new ItemStack(getLogDrop(blockType), 1));
            return;
        }

        // Axe: full veinmine logs first
        int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        boolean silkTouch = tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;

        Set<Location> logBlocks = new HashSet<>();
        findConnectedBlocks(start, blockType, logBlocks, plugin.getConfig().getInt("tree-remover.max-blocks", 100));

        for (Location loc : logBlocks) {
            loc.getBlock().setType(Material.AIR);
            loc.getWorld().dropItemNaturally(loc, new ItemStack(getLogDrop(blockType)));
        }

        if (isNetherStem(blockType)) {
            // Nether trees: fast-decay the nether wart blocks connected to the tree
            Material wartBlock = getNetherWartBlock(blockType);
            Set<Location> wartBlocks = new HashSet<>();
            for (Location logLoc : logBlocks) {
                findConnectedWartBlocks(logLoc, wartBlock, wartBlocks,
                        plugin.getConfig().getInt("tree-remover.max-leaf-blocks", 200));
            }
            for (Location loc : wartBlocks) {
                Block wb = loc.getBlock();
                if (wb.getType() != wartBlock) continue; // re-check
                wb.setType(Material.AIR);
                loc.getWorld().dropItemNaturally(loc, new ItemStack(wartBlock, 1));
            }
        } else {
            // Normal trees: fast-decay leaves instantly on veinmine, use configured drop rates
            Set<Location> leafBlocks = new HashSet<>();
            for (Location logLoc : logBlocks) {
                findConnectedLeaves(logLoc, leafBlocks, plugin.getConfig().getInt("tree-remover.max-leaf-blocks", 200));
            }
            for (Location loc : leafBlocks) {
                Block leafBlock = loc.getBlock();
                if (!isLeaf(leafBlock.getType())) continue; // re-check, world may have changed
                leafDecay.dropLeafLoot(leafBlock, fortune, silkTouch);
                leafBlock.setType(Material.AIR);
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
        player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, start, 20, 1.0, 1.0, 1.0, 0.1);
    }

    /**
     * Returns what a log/stem should drop when broken.
     * Crimson Stem → Nether Wart Block, Warped Stem → Warped Wart Block,
     * everything else → itself.
     */
    private Material getLogDrop(Material blockType) {
        return switch (blockType) {
            case CRIMSON_STEM, STRIPPED_CRIMSON_STEM -> Material.NETHER_WART_BLOCK;
            case WARPED_STEM,  STRIPPED_WARPED_STEM  -> Material.WARPED_WART_BLOCK;
            default -> blockType;
        };
    }

    /**
     * Returns the wart block type associated with a nether stem.
     * Used to flood-fill and drop the canopy equivalent for nether trees.
     */
    private Material getNetherWartBlock(Material stem) {
        return switch (stem) {
            case WARPED_STEM, STRIPPED_WARPED_STEM -> Material.WARPED_WART_BLOCK;
            default -> Material.NETHER_WART_BLOCK; // Crimson and any future nether stem
        };
    }

    private boolean isAxe(Material m) {
        return m == Material.WOODEN_AXE || m == Material.STONE_AXE
                || m == Material.COPPER_AXE || m == Material.IRON_AXE
                || m == Material.GOLDEN_AXE || m == Material.DIAMOND_AXE
                || m == Material.NETHERITE_AXE;
    }

    /**
     * Logs and nether stems are veinminable; mushroom stems are excluded separately.
     */
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

    private boolean isLeaf(Material material) {
        return material.name().endsWith("_LEAVES");
    }

    /** Flood-fill matching blocks of a single type (used for logs). */
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

    /**
     * Flood-fill any leaf blocks reachable from a given position.
     * Walks through both leaves and air so it can reach leaves on the
     * outer canopy, but only adds actual leaf blocks to the result set.
     */
    private void findConnectedLeaves(Location start, Set<Location> foundLeaves, int maxLeaves) {
        Set<Location> visited = new HashSet<>();
        Queue<Location> toCheck = new LinkedList<>();
        toCheck.add(start);

        while (!toCheck.isEmpty() && foundLeaves.size() < maxLeaves) {
            Location current = toCheck.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            Material type = current.getBlock().getType();

            if (isLeaf(type)) {
                foundLeaves.add(current);
            } else if (type != Material.AIR && !isLog(type)) {
                // Hit something solid that isn't a log or leaf — don't pass through
                continue;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Location neighbour = current.clone().add(dx, dy, dz);
                        if (!visited.contains(neighbour)) {
                            toCheck.add(neighbour);
                        }
                    }
                }
            }
        }
    }

    /**
     * Flood-fill nether wart blocks (or warped wart blocks) adjacent to the felled nether tree.
     * Passes through the wart block type and air only, mirroring how leaf flood-fill works.
     */
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
                // Hit something solid that isn't a stem or wart block — stop here
                continue;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Location neighbour = current.clone().add(dx, dy, dz);
                        if (!visited.contains(neighbour)) {
                            toCheck.add(neighbour);
                        }
                    }
                }
            }
        }
    }
}