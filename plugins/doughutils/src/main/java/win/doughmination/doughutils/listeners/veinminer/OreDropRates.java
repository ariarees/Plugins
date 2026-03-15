/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.listeners.veinminer;

/**
 * Central place to tweak veinminer drop rates.
 * All ranges are inclusive. Fortune multipliers follow vanilla behaviour
 * unless overridden here.
 */
public final class OreDropRates {

    private OreDropRates() {}

    // -------------------------------------------------------------------------
    // Base drop ranges (no Fortune, no Silk Touch)
    // -------------------------------------------------------------------------

    public static final int COAL_MIN            = 1;
    public static final int COAL_MAX            = 1;

    public static final int IRON_MIN            = 1;
    public static final int IRON_MAX            = 1;

    public static final int GOLD_MIN            = 1;
    public static final int GOLD_MAX            = 1;

    public static final int DIAMOND_MIN         = 1;
    public static final int DIAMOND_MAX         = 1;

    public static final int EMERALD_MIN         = 1;
    public static final int EMERALD_MAX         = 1;

    public static final int COPPER_MIN          = 2;
    public static final int COPPER_MAX          = 5;

    public static final int REDSTONE_MIN        = 4;
    public static final int REDSTONE_MAX        = 5;

    public static final int LAPIS_MIN           = 4;
    public static final int LAPIS_MAX           = 9;

    public static final int QUARTZ_MIN          = 1;
    public static final int QUARTZ_MAX          = 1;

    public static final int NETHER_GOLD_MIN     = 2;
    public static final int NETHER_GOLD_MAX     = 6;

    // -------------------------------------------------------------------------
    // Fortune caps — maximum drops achievable with Fortune III
    // -------------------------------------------------------------------------

    public static final int COAL_FORTUNE_CAP        = 4;
    public static final int IRON_FORTUNE_CAP        = 4;
    public static final int GOLD_FORTUNE_CAP        = 4;
    public static final int DIAMOND_FORTUNE_CAP     = 4;
    public static final int EMERALD_FORTUNE_CAP     = 4;
    public static final int COPPER_FORTUNE_CAP      = 20;
    public static final int REDSTONE_FORTUNE_BONUS  = 1;  // flat +1 per Fortune level
    public static final int LAPIS_FORTUNE_CAP       = 36;
    public static final int QUARTZ_FORTUNE_CAP      = 4;
    public static final int NETHER_GOLD_FORTUNE_CAP = 24;

    // -------------------------------------------------------------------------
    // XP drop ranges (per ore block broken)
    // Iron, Gold, Copper, Ancient Debris drop no XP (smelted instead)
    // -------------------------------------------------------------------------

    public static final int COAL_XP_MIN         = 0;
    public static final int COAL_XP_MAX         = 2;

    public static final int LAPIS_XP_MIN        = 2;
    public static final int LAPIS_XP_MAX        = 5;

    public static final int QUARTZ_XP_MIN       = 2;
    public static final int QUARTZ_XP_MAX       = 5;

    public static final int EMERALD_XP_MIN      = 3;
    public static final int EMERALD_XP_MAX      = 7;

    public static final int DIAMOND_XP_MIN      = 3;
    public static final int DIAMOND_XP_MAX      = 7;

    public static final int REDSTONE_XP_MIN     = 1;
    public static final int REDSTONE_XP_MAX     = 5;

    public static final int NETHER_GOLD_XP_MIN  = 0;
    public static final int NETHER_GOLD_XP_MAX  = 1;
    //   Fortune I:   33.3% chance to ×2
    //   Fortune II:  25%   chance to ×2 or ×3
    //   Fortune III: 20%   chance each to ×2, ×3, or ×4
    // -------------------------------------------------------------------------

    public static final double NETHER_GOLD_F1_CHANCE  = 0.333;
    public static final double NETHER_GOLD_F2_CHANCE  = 0.25;
    public static final double NETHER_GOLD_F3_CHANCE  = 0.20;
}