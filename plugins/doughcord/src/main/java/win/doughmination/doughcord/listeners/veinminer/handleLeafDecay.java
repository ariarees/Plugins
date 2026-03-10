/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.veinminer;

import win.doughmination.doughcord.CordMain;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Handles drops from leaf blocks, both when they decay naturally
 * and when a player breaks them without Silk Touch or shears.
 *
 * Drop rates (vanilla-accurate):
 *   Saplings  — 5% base (2.5% for jungle leaves)
 *   Sticks    — 2% base
 *   Apple     — 0.5% base, oak/dark oak only (additive, independent roll)
 *
 * Fortune increases sapling and apple drop chances:
 *   Fortune I  → rates × 2
 *   Fortune II → rates × 3
 *   Fortune III→ rates × 4
 *
 * Burned leaves drop nothing (LeavesDecayEvent is not fired for fire).
 */
public class handleLeafDecay implements Listener {

    private final CordMain plugin;
    private final Random random = new Random();

    // Base drop chances
    private static final double BASE_SAPLING_CHANCE        = 0.05;   // 5%
    private static final double JUNGLE_SAPLING_CHANCE      = 0.025;  // 2.5%
    private static final double BASE_STICK_CHANCE          = 0.02;   // 2%
    private static final double BASE_APPLE_CHANCE          = 0.005;  // 0.5%

    public handleLeafDecay(CordMain plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------
    // Natural decay
    // -----------------------------------------------------------------

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        Block block = event.getBlock();
        if (!isLeaf(block.getType())) return;

        // Natural decay: no fortune, no silk touch
        dropLeafLoot(block, 0, false);
    }

    // -----------------------------------------------------------------
    // Core drop logic
    // -----------------------------------------------------------------

    /**
     * Rolls and drops loot for a leaf block at its location.
     *
     * @param block   the leaf block being destroyed
     * @param fortune fortune enchantment level (0 = none)
     * @param burned  if true, no drops (reserved for future fire integration)
     */
    /**
     * Rolls and drops loot for a leaf block.
     * Called by handleTreeVeinminer when felling a tree, and by LeavesDecayEvent.
     *
     * @param block     the leaf block being destroyed
     * @param fortune   fortune enchantment level (0 = none)
     * @param silkTouch if true, skip all loot rolls (vanilla handles silk touch drop)
     */
    void dropLeafLoot(Block block, int fortune, boolean silkTouch) {
        if (silkTouch) return;

        Material leaf = block.getType();
        double fortuneMultiplier = 1.0 + fortune; // F0→×1, F1→×2, F2→×3, F3→×4

        // --- Sapling roll ---
        double saplingChance = isJungleLeaf(leaf)
                ? JUNGLE_SAPLING_CHANCE
                : BASE_SAPLING_CHANCE;
        saplingChance *= fortuneMultiplier;

        if (random.nextDouble() < saplingChance) {
            Material sapling = getSaplingFor(leaf);
            if (sapling != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(sapling, 1));
            }
        }

        // --- Stick roll ---
        double stickChance = BASE_STICK_CHANCE * fortuneMultiplier;
        if (random.nextDouble() < stickChance) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.STICK, 1));
        }

        // --- Apple roll (oak and dark oak only, independent) ---
        if (isAppleLeaf(leaf)) {
            double appleChance = BASE_APPLE_CHANCE * fortuneMultiplier;
            if (random.nextDouble() < appleChance) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE, 1));
            }
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private boolean isLeaf(Material material) {
        return material.name().endsWith("_LEAVES");
    }

    private boolean isJungleLeaf(Material material) {
        return material == Material.JUNGLE_LEAVES;
    }

    /** Oak and dark oak leaves can drop apples. */
    private boolean isAppleLeaf(Material material) {
        return material == Material.OAK_LEAVES || material == Material.DARK_OAK_LEAVES;
    }

    /**
     * Maps a leaf type to its corresponding sapling.
     * Returns null for leaf types without a sapling (e.g. azalea, mangrove bundles).
     */
    private Material getSaplingFor(Material leaf) {
        return switch (leaf) {
            case OAK_LEAVES           -> Material.OAK_SAPLING;
            case SPRUCE_LEAVES        -> Material.SPRUCE_SAPLING;
            case BIRCH_LEAVES         -> Material.BIRCH_SAPLING;
            case JUNGLE_LEAVES        -> Material.JUNGLE_SAPLING;
            case ACACIA_LEAVES        -> Material.ACACIA_SAPLING;
            case DARK_OAK_LEAVES      -> Material.DARK_OAK_SAPLING;
            case CHERRY_LEAVES        -> Material.CHERRY_SAPLING;
            case PALE_OAK_LEAVES      -> Material.PALE_OAK_SAPLING;
            case MANGROVE_LEAVES      -> Material.MANGROVE_PROPAGULE;
            // Azalea leaves, flowering azalea leaves — no sapling drop
            default                   -> null;
        };
    }
}