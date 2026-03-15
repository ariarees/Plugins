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
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.*;

public class handleOreVeinminer {

    private final Main plugin;
    private final Random random = new Random();

    public handleOreVeinminer(Main plugin) {
        this.plugin = plugin;
    }

    public void handleOreBreak(Player player, Block block) {
        Material blockType = block.getType();
        if (!isOre(blockType)) return;

        // Tool lock — must be holding a pickaxe
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isPickaxe(tool.getType())) return;

        // Enforce minimum pickaxe tier per ore
        if (!meetsMinTier(tool.getType(), blockType)) return;

        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        boolean silkTouch = tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;

        Location start = block.getLocation();
        Set<Location> oreBlocks = new HashSet<>();
        findOreBlocks(start, blockType, oreBlocks);

        for (Location loc : oreBlocks) {
            loc.getBlock().setType(Material.AIR);

            if (silkTouch) {
                loc.getWorld().dropItemNaturally(loc, new ItemStack(blockType));
            } else {
                List<ItemStack> drops = getDrops(blockType, fortuneLevel);
                for (ItemStack drop : drops) {
                    loc.getWorld().dropItemNaturally(loc, drop);
                }
                int xp = getXpDrop(blockType);
                if (xp > 0) {
                    loc.getWorld().spawn(loc, ExperienceOrb.class, orb -> orb.setExperience(xp));
                }
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_NETHER_ORE_BREAK, 1.0f, 1.0f);
        player.spawnParticle(Particle.HAPPY_VILLAGER, start, 20, 1.0, 1.0, 1.0, 0.1);
    }

    // -----------------------------------------------------------------
    // Drop calculation — matches vanilla wiki rates exactly
    // -----------------------------------------------------------------

    private List<ItemStack> getDrops(Material oreType, int fortune) {
        List<ItemStack> drops = new ArrayList<>();
        int amount;

        switch (oreType) {

            // Coal
            case COAL_ORE, DEEPSLATE_COAL_ORE -> {
                amount = randRange(OreDropRates.COAL_MIN, OreDropRates.COAL_MAX);
                amount = applyAdditiveForune(amount, fortune, OreDropRates.COAL_FORTUNE_CAP);
                drops.add(new ItemStack(Material.COAL, amount));
            }

            // Iron
            case IRON_ORE, DEEPSLATE_IRON_ORE -> {
                amount = randRange(OreDropRates.IRON_MIN, OreDropRates.IRON_MAX);
                amount = applyAdditiveForune(amount, fortune, OreDropRates.IRON_FORTUNE_CAP);
                drops.add(new ItemStack(Material.RAW_IRON, amount));
            }

            // Gold
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> {
                amount = randRange(OreDropRates.GOLD_MIN, OreDropRates.GOLD_MAX);
                amount = applyAdditiveForune(amount, fortune, OreDropRates.GOLD_FORTUNE_CAP);
                drops.add(new ItemStack(Material.RAW_GOLD, amount));
            }

            // Diamond
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> {
                amount = randRange(OreDropRates.DIAMOND_MIN, OreDropRates.DIAMOND_MAX);
                amount = applyAdditiveForune(amount, fortune, OreDropRates.DIAMOND_FORTUNE_CAP);
                drops.add(new ItemStack(Material.DIAMOND, amount));
            }

            // Emerald
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> {
                amount = randRange(OreDropRates.EMERALD_MIN, OreDropRates.EMERALD_MAX);
                amount = applyAdditiveForune(amount, fortune, OreDropRates.EMERALD_FORTUNE_CAP);
                drops.add(new ItemStack(Material.EMERALD, amount));
            }

            // Copper — Fortune multiplies
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> {
                amount = randRange(OreDropRates.COPPER_MIN, OreDropRates.COPPER_MAX);
                if (fortune > 0) amount = applyFortuneMultiplier(amount, fortune, OreDropRates.COPPER_FORTUNE_CAP);
                drops.add(new ItemStack(Material.RAW_COPPER, amount));
            }

            // Redstone — flat +bonus per Fortune level
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> {
                amount = randRange(OreDropRates.REDSTONE_MIN, OreDropRates.REDSTONE_MAX);
                if (fortune > 0) amount += fortune * OreDropRates.REDSTONE_FORTUNE_BONUS;
                drops.add(new ItemStack(Material.REDSTONE, amount));
            }

            // Lapis — Fortune multiplies
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> {
                amount = randRange(OreDropRates.LAPIS_MIN, OreDropRates.LAPIS_MAX);
                if (fortune > 0) amount = applyFortuneMultiplier(amount, fortune, OreDropRates.LAPIS_FORTUNE_CAP);
                drops.add(new ItemStack(Material.LAPIS_LAZULI, amount));
            }

            // Nether Quartz
            case NETHER_QUARTZ_ORE -> {
                amount = randRange(OreDropRates.QUARTZ_MIN, OreDropRates.QUARTZ_MAX);
                amount = applyAdditiveForune(amount, fortune, OreDropRates.QUARTZ_FORTUNE_CAP);
                drops.add(new ItemStack(Material.QUARTZ, amount));
            }

            // Nether Gold — tiered Fortune chance
            case NETHER_GOLD_ORE -> {
                amount = randRange(OreDropRates.NETHER_GOLD_MIN, OreDropRates.NETHER_GOLD_MAX);
                amount = applyNetherGoldFortune(amount, fortune);
                drops.add(new ItemStack(Material.GOLD_NUGGET, amount));
            }

            // Ancient Debris: always drops itself (no fortune effect)
            case ANCIENT_DEBRIS -> drops.add(new ItemStack(Material.ANCIENT_DEBRIS, 1));

            default -> {} // unsupported ore — drop nothing
        }

        return drops;
    }

    /**
     * Returns XP to drop for a given ore block.
     * Iron, gold, copper, and ancient debris drop 0 (player smelts for XP).
     */
    private int getXpDrop(Material oreType) {
        return switch (oreType) {
            case COAL_ORE,         DEEPSLATE_COAL_ORE     -> randRange(OreDropRates.COAL_XP_MIN,        OreDropRates.COAL_XP_MAX);
            case LAPIS_ORE,        DEEPSLATE_LAPIS_ORE    -> randRange(OreDropRates.LAPIS_XP_MIN,       OreDropRates.LAPIS_XP_MAX);
            case NETHER_QUARTZ_ORE                        -> randRange(OreDropRates.QUARTZ_XP_MIN,      OreDropRates.QUARTZ_XP_MAX);
            case EMERALD_ORE,      DEEPSLATE_EMERALD_ORE  -> randRange(OreDropRates.EMERALD_XP_MIN,     OreDropRates.EMERALD_XP_MAX);
            case DIAMOND_ORE,      DEEPSLATE_DIAMOND_ORE  -> randRange(OreDropRates.DIAMOND_XP_MIN,     OreDropRates.DIAMOND_XP_MAX);
            case REDSTONE_ORE,     DEEPSLATE_REDSTONE_ORE -> randRange(OreDropRates.REDSTONE_XP_MIN,    OreDropRates.REDSTONE_XP_MAX);
            case NETHER_GOLD_ORE                          -> randRange(OreDropRates.NETHER_GOLD_XP_MIN, OreDropRates.NETHER_GOLD_XP_MAX);
            default -> 0;
        };
    }
    private int randRange(int min, int max) {
        if (min == max) return min;
        return min + random.nextInt(max - min + 1);
    }

    /**
     * Additive Fortune: adds a random 0–fortune bonus, capped at max.
     */
    private int applyAdditiveForune(int base, int fortune, int max) {
        if (fortune <= 0) return base;
        return Math.min(base + random.nextInt(fortune + 1), max);
    }

    /**
     * Multiplier Fortune: randomly multiplies amount by 1–(fortune+1), capped at max.
     */
    private int applyFortuneMultiplier(int base, int fortune, int max) {
        int multiplier = 1 + random.nextInt(fortune + 1);
        return Math.min(base * multiplier, max);
    }

    /**
     * Nether Gold Fortune using vanilla tiered chances from OreDropRates.
     */
    private int applyNetherGoldFortune(int base, int fortune) {
        if (fortune == 0) return base;
        double roll = random.nextDouble();
        int multiplier = switch (fortune) {
            case 1 -> roll < OreDropRates.NETHER_GOLD_F1_CHANCE ? 2 : 1;
            case 2 -> roll < OreDropRates.NETHER_GOLD_F2_CHANCE ? (random.nextBoolean() ? 2 : 3) : 1;
            default -> roll < OreDropRates.NETHER_GOLD_F3_CHANCE ? (2 + random.nextInt(3)) : 1;
        };
        return Math.min(base * multiplier, OreDropRates.NETHER_GOLD_FORTUNE_CAP);
    }

    // -----------------------------------------------------------------
    // Tool checks
    // -----------------------------------------------------------------

    private boolean isPickaxe(Material m) {
        return m == Material.WOODEN_PICKAXE || m == Material.STONE_PICKAXE
                || m == Material.COPPER_PICKAXE || m == Material.IRON_PICKAXE
                || m == Material.GOLDEN_PICKAXE || m == Material.DIAMOND_PICKAXE
                || m == Material.NETHERITE_PICKAXE;
    }

    /** Returns true if the held pickaxe meets the minimum tier for the given ore. */
    private boolean meetsMinTier(Material pickaxe, Material ore) {
        // Diamond or better required for: ancient debris
        boolean needsDiamond = ore == Material.ANCIENT_DEBRIS;

        // Iron or better required for: gold, diamond, emerald, redstone
        boolean needsIron = switch (ore) {
            case GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> true;
            default -> false;
        };

        // Stone or better required for: iron, copper, lapis, coal, nether ores
        boolean needsStone = switch (ore) {
            case IRON_ORE, DEEPSLATE_IRON_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 COAL_ORE, DEEPSLATE_COAL_ORE,
                 NETHER_QUARTZ_ORE, NETHER_GOLD_ORE -> true;
            default -> false;
        };

        if (needsDiamond) return isDiamondOrBetter(pickaxe);
        if (needsIron)    return isIronOrBetter(pickaxe);
        if (needsStone)   return isStoneOrBetter(pickaxe);
        return true;
    }

    private boolean isDiamondOrBetter(Material m) {
        return m == Material.DIAMOND_PICKAXE || m == Material.NETHERITE_PICKAXE;
    }

    // Copper sits between stone and iron
    private boolean isStoneOrBetter(Material m) {
        return m == Material.STONE_PICKAXE || m == Material.COPPER_PICKAXE
                || isIronOrBetter(m);
    }

    private boolean isIronOrBetter(Material m) {
        return m == Material.IRON_PICKAXE || m == Material.GOLDEN_PICKAXE
                || m == Material.DIAMOND_PICKAXE || m == Material.NETHERITE_PICKAXE;
    }

    // -----------------------------------------------------------------
    // Flood fill
    // -----------------------------------------------------------------

    private boolean isOre(Material material) {
        return material.name().endsWith("_ORE") || material.name().endsWith("_DEBRIS");
    }

    private void findOreBlocks(Location start, Material blockType, Set<Location> foundBlocks) {
        int maxBlocks = plugin.getConfig().getInt("ore-remover.max-blocks", 100);

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
}