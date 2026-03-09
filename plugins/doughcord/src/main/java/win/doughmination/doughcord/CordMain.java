/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bstats.bukkit.Metrics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import win.doughmination.api.LibMain;
import win.doughmination.doughcord.commands.GrowthShrinkPotionCommandExecutor;
import win.doughmination.doughcord.commands.RecipesCommandExecutor;
import win.doughmination.doughcord.commands.chests.EChestCommandExecutor;
import win.doughmination.doughcord.commands.chests.VChestCommandExecutor;
import win.doughmination.doughcord.commands.chests.chestsCommandExecutor;
import win.doughmination.doughcord.commands.moderation.BanCommandExecutor;
import win.doughmination.doughcord.commands.moderation.BanlistCommandExecutor;
import win.doughmination.doughcord.commands.moderation.DoughCommandExecutor;
import win.doughmination.doughcord.commands.moderation.ReloadCommandExecutor;
import win.doughmination.doughcord.commands.moderation.UnbanCommandExecutor;
import win.doughmination.doughcord.commands.moderation.VersionCommandExecutor;
import win.doughmination.doughcord.commands.playtimeCommandExecutor;
import win.doughmination.doughcord.commands.roleplay.barkCommandExecutor;
import win.doughmination.doughcord.commands.roleplay.kissCommandExecutor;
import win.doughmination.doughcord.commands.roleplay.meowCommandExecutor;
import win.doughmination.doughcord.commands.travel.base.BaseCommandExecutor;
import win.doughmination.doughcord.commands.travel.base.BaseDataManager;
import win.doughmination.doughcord.commands.travel.base.BaseProtectionListener;
import win.doughmination.doughcord.commands.travel.rtpCommandExecutor;
import win.doughmination.doughcord.commands.travel.setspawnCommandExecutor;
import win.doughmination.doughcord.commands.travel.spawnCommandExecutor;
import win.doughmination.doughcord.commands.travel.tpaCommandExecutor;
import win.doughmination.doughcord.commands.travel.tpacceptCommandExecutor;
import win.doughmination.doughcord.commands.travel.tpdenyCommandExecutor;
import win.doughmination.doughcord.commands.veinminerCommandExecutor;
import win.doughmination.doughcord.data.PlayerDataManager;
import win.doughmination.doughcord.listeners.potions.PotionRecipeManager;
import win.doughmination.doughcord.listeners.potions.PotionUseListener;
import win.doughmination.doughcord.listeners.spawneggs.RecipeManager;
import win.doughmination.doughcord.listeners.spawneggs.SpawnEggRecipes;
import win.doughmination.doughcord.listeners.travel.BaseFlightMain;
import win.doughmination.doughcord.listeners.travel.TeleportRequestManager;
import win.doughmination.doughcord.listeners.veinminer.blockVeinminerListener;

public class CordMain extends JavaPlugin {

    private Metrics metrics;
    private final Map<UUID, Long>     playtimeMap     = new HashMap<>();
    private final Map<UUID, Long>     loginTimestamps = new HashMap<>();
    private final Map<UUID, Location> bases           = new HashMap<>();

    private PlayerDataManager        playerDataManager;
    private VChestCommandExecutor    vChestExecutor;
    private veinminerCommandExecutor veinMinerExecutor;
    private BaseFlightMain           baseFlightMain;
    private TeleportRequestManager   teleportRequestManager;
    private BaseCommandExecutor      baseCommandExecutor;
    private BaseDataManager          baseDataManager;

    @Override
    public void onEnable() {
        int pluginId = 29925;
        metrics = new Metrics(this, pluginId);

        getLogger().info("Doughcord is starting up...");

        if (LibMain.getInstance() == null) {
            getLogger().severe("DoughAPI is not initialized! Ensure it is installed and loaded.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        playerDataManager = new PlayerDataManager(this);

        baseDataManager = new BaseDataManager(this);
        baseDataManager.loadAll();

        loadAllBases();
        loadAllPlaytime();

        veinMinerExecutor = new veinminerCommandExecutor(this);
        vChestExecutor    = new VChestCommandExecutor(this);

        teleportRequestManager = new TeleportRequestManager(this);
        teleportRequestManager.onEnable();

        registerCommands();

        baseFlightMain = new BaseFlightMain(this);
        baseFlightMain.onEnable();

        getServer().getPluginManager().registerEvents(new blockVeinminerListener(this, veinMinerExecutor), this);
        getServer().getPluginManager().registerEvents(new PotionUseListener(this), this);
        getServer().getPluginManager().registerEvents(new BaseProtectionListener(this, baseDataManager), this);

        SpawnEggRecipes.resetSymbolMap();
        for (SpawnEggRecipes recipe : SpawnEggRecipes.values()) {
            Bukkit.removeRecipe(new NamespacedKey(this, recipe.getKey()));
        }
        RecipeManager.registerRecipes(this);
        PotionRecipeManager.registerRecipes(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Doughcord is shutting down...");
        if (baseFlightMain != null) baseFlightMain.onDisable();
        if (teleportRequestManager != null) teleportRequestManager.onDisable();
        if (baseCommandExecutor != null) baseCommandExecutor.shutdown();
        saveAllBases();
        saveAllPlaytime();
        vChestExecutor.saveAll();
    }

    // -------------------------------------------------------------------------
    // Command registration
    // -------------------------------------------------------------------------

    private void registerCommands() {
        reg("setspawn",    new setspawnCommandExecutor(this));
        reg("spawn",       new spawnCommandExecutor(this));
        reg("tpa",         new tpaCommandExecutor(this));
        reg("tpaccept",    new tpacceptCommandExecutor(this));
        reg("tpdeny",      new tpdenyCommandExecutor(this));
        reg("rtp",         new rtpCommandExecutor(this));
        baseCommandExecutor = new BaseCommandExecutor(this, baseDataManager);
        reg("base", baseCommandExecutor);
        reg("meow",        new meowCommandExecutor(this));
        reg("bark",        new barkCommandExecutor(this));
        reg("kiss",        new kissCommandExecutor(this));
        reg("playtime",    new playtimeCommandExecutor(this));
        reg("veinminer",   veinMinerExecutor);
        reg("echest",      new EChestCommandExecutor(this));
        reg("vchest",      vChestExecutor);
        reg("chest",       new chestsCommandExecutor(this));
        reg("recipes",     new RecipesCommandExecutor(this));
        GrowthShrinkPotionCommandExecutor potionExec = new GrowthShrinkPotionCommandExecutor(this);
        reg("growthpotion", potionExec);
        reg("shrinkpotion", potionExec);
        reg("dough",        new DoughCommandExecutor(this));
        reg("version",      new VersionCommandExecutor(this));
        reg("doughreload",  new ReloadCommandExecutor(this));
        BanCommandExecutor banExec = new BanCommandExecutor(this);
        reg("doughban",    banExec);
        UnbanCommandExecutor unbanExec = new UnbanCommandExecutor(this);
        reg("unban",       unbanExec);
        reg("banlist",     new BanlistCommandExecutor(this));
    }

    private <T extends CommandExecutor & TabCompleter> void reg(String cmd, T exec) {
        getCommand(cmd).setExecutor(exec);
        getCommand(cmd).setTabCompleter(exec);
    }

    // -------------------------------------------------------------------------
    // Bases
    // -------------------------------------------------------------------------

    private void loadAllBases() {
        java.io.File settingsDir = new java.io.File(getDataFolder(), "data/settings");
        if (!settingsDir.exists()) return;
        java.io.File[] baseFiles = settingsDir.listFiles();
        if (baseFiles == null) return;
        for (java.io.File f : baseFiles) {
            String name = f.getName();
            if (!name.endsWith(".json")) continue;
            try {
                UUID uuid = UUID.fromString(name.replace(".json", ""));
                Location loc = playerDataManager.loadBase(uuid);
                if (loc != null) bases.put(uuid, loc);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveAllBases() {
        bases.forEach((uuid, loc) -> playerDataManager.saveBase(uuid, loc));
    }

    // -------------------------------------------------------------------------
    // Playtime
    // -------------------------------------------------------------------------

    private void loadAllPlaytime() {
        java.io.File settingsDir = new java.io.File(getDataFolder(), "data/settings");
        if (!settingsDir.exists()) return;
        java.io.File[] playtimeFiles = settingsDir.listFiles();
        if (playtimeFiles == null) return;
        for (java.io.File f : playtimeFiles) {
            String name = f.getName();
            if (!name.endsWith(".json")) continue;
            try {
                UUID uuid = UUID.fromString(name.replace(".json", ""));
                long ms = playerDataManager.loadPlaytime(uuid);
                if (ms > 0) playtimeMap.put(uuid, ms);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveAllPlaytime() {
        long now = System.currentTimeMillis();
        loginTimestamps.forEach((uuid, start) -> {
            playtimeMap.merge(uuid, now - start, Long::sum);
            loginTimestamps.put(uuid, now);
        });
        playtimeMap.forEach((uuid, ms) -> playerDataManager.savePlaytime(uuid, ms));
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public PlayerDataManager        getPlayerDataManager()       { return playerDataManager; }
    public BaseDataManager          getBaseDataManager()         { return baseDataManager; }
    public TeleportRequestManager   getTeleportRequestManager()  { return teleportRequestManager; }
    public Map<UUID, Long>          getPlaytimeMap()             { return playtimeMap; }
    public Map<UUID, Long>          getLoginTimestamps()         { return loginTimestamps; }
    public Map<UUID, Location>      getBases()                   { return bases; }
    public Location                 getBaseLocation(UUID u)      { return bases.get(u); }
    public boolean                  hasBase(UUID u)              { return bases.containsKey(u); }
    public veinminerCommandExecutor getVeinMinerExecutor()       { return veinMinerExecutor; }
    public BaseFlightMain           getBaseFlightMain()          { return baseFlightMain; }

    public boolean isPlayerJailed(Player player) {
        LibMain api = LibMain.getInstance();
        return api != null && api.isPlayerJailed(player.getUniqueId());
    }
}
