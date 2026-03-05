/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughminationcord;

import win.doughmination.api.LibMain;
import win.doughmination.doughminationcord.commands.*;
import win.doughmination.doughminationcord.commands.chests.*;
import win.doughmination.doughminationcord.commands.moderation.*;
import win.doughmination.doughminationcord.commands.roleplay.*;
import win.doughmination.doughminationcord.commands.travel.*;
import win.doughmination.doughminationcord.data.*;
import win.doughmination.doughminationcord.listeners.flight.*;
import win.doughmination.doughminationcord.listeners.veinminer.*;
import win.doughmination.doughminationcord.listeners.spawneggs.*;
import win.doughmination.doughminationcord.listeners.potions.*;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bstats.bukkit.Metrics;

public class CordMain extends JavaPlugin {

    // In-memory player data
    private final Map<UUID, Long>     playtimeMap      = new HashMap<>();
    private final Map<UUID, Long>     loginTimestamps  = new HashMap<>();
    private final Map<UUID, Location> bases            = new HashMap<>();

    // Data layer — single source of truth for all JSON persistence
    private PlayerDataManager playerDataManager;

    // Executor references kept for shutdown saves and cross-command access
    private VChestCommandExecutor   vChestExecutor;
    private veinminerCommandExecutor veinMinerExecutor;
    private BaseFlightMain          baseFlightMain;

    @Override
    public void onEnable() {

        int pluginId = 29925; // Replace with your actual plugin id
        Metrics metrics = new Metrics(this, pluginId);

        getLogger().info(ChatColor.AQUA + "Doughcord is starting up...");

        if (LibMain.getInstance() == null) {
            getLogger().severe("DoughAPI is not initialized! Ensure it is installed and loaded.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        // Boot the data manager first — everything else depends on it
        playerDataManager = new PlayerDataManager(this);

        // Load persisted data into memory
        loadAllBases();
        loadAllPlaytime();

        // Build retained executors
        veinMinerExecutor = new veinminerCommandExecutor(this);
        vChestExecutor    = new VChestCommandExecutor(this);

        registerCommands();

        RecipeManager.registerRecipes(this);
        PotionRecipeManager.registerRecipes(this);

        baseFlightMain = new BaseFlightMain(this);
        baseFlightMain.onEnable();

        getServer().getPluginManager().registerEvents(new blockVeinminerListener(this, veinMinerExecutor), this);
        getServer().getPluginManager().registerEvents(new PotionUseListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.AQUA + "Doughcord is shutting down...");
        saveAllBases();
        saveAllPlaytime();
        vChestExecutor.saveAll();
    }

    // -----------------------------------------------------------------------
    // Command registration
    // -----------------------------------------------------------------------

    private void registerCommands() {
        reg("setspawn",    new setSpawnCommandExecutor(this));
        reg("spawn",       new spawnCommandExecutor(this));
        reg("tpask",       new tpAskCommandExecutor(this));
        reg("tpaccept",    new tpAcceptCommandExecutor(this));
        reg("tpdeny",      new tpDenyCommandExecutor(this));
        reg("setbase",     new setBaseCommandExecutor(this));
        reg("base",        new baseCommandExecutor(this));
        reg("visitbase",   new visitBaseCommandExecutor(this));
        reg("meow",        new meowCommandExecutor(this));
        reg("bark",        new barkCommandExecutor(this));
        reg("kiss",        new kissCommandExecutor(this));
        reg("playtime",    new playtimeCommandExecutor(this));
        reg("veinminer",   veinMinerExecutor);
        reg("echest",      new EChestCommandExecutor(this));
        reg("vchest",      vChestExecutor);
        reg("chest",       new chestsCommandExecutor(this));
        reg("recipes",     new RecipesCommandExecutor(this));
        GrowthShrinkPotionCommand potionExec = new GrowthShrinkPotionCommand(this);
        reg("growthpotion", potionExec);
        reg("shrinkpotion", potionExec);
        reg("dough",        new DoughCommandExecutor(this));
        reg("version",      new VersionCommandExecutor(this));
        reg("doughreload",  new ReloadCommandExecutor(this));
        BanCommandExecutor banExec = new BanCommandExecutor(this);
        reg("doughban", banExec);
        UnbanCommandExecutor unbanExec = new UnbanCommandExecutor(this);
        reg("unban", unbanExec);
        reg("banlist", new BanlistCommandExecutor(this));
    }

    private <T extends CommandExecutor & TabCompleter> void reg(String cmd, T exec) {
        getCommand(cmd).setExecutor(exec);
        getCommand(cmd).setTabCompleter(exec);
    }

    // -----------------------------------------------------------------------
    // Bases — backed by PlayerDataManager
    // -----------------------------------------------------------------------

    private void loadAllBases() {
        // Scan every existing settings file and pre-load bases into memory
        java.io.File settingsDir = new java.io.File(getDataFolder(), "data/settings");
        if (!settingsDir.exists()) return;
        for (java.io.File f : settingsDir.listFiles()) {
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

    // -----------------------------------------------------------------------
    // Playtime — backed by PlayerDataManager
    // -----------------------------------------------------------------------

    private void loadAllPlaytime() {
        java.io.File settingsDir = new java.io.File(getDataFolder(), "data/settings");
        if (!settingsDir.exists()) return;
        for (java.io.File f : settingsDir.listFiles()) {
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
        // Flush active sessions into the map before saving
        long now = System.currentTimeMillis();
        loginTimestamps.forEach((uuid, start) -> {
            playtimeMap.merge(uuid, now - start, Long::sum);
            loginTimestamps.put(uuid, now);
        });
        playtimeMap.forEach((uuid, ms) -> playerDataManager.savePlaytime(uuid, ms));
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public PlayerDataManager    getPlayerDataManager()  { return playerDataManager; }
    public Map<UUID, Long>      getPlaytimeMap()        { return playtimeMap; }
    public Map<UUID, Long>      getLoginTimestamps()    { return loginTimestamps; }
    public Map<UUID, Location>  getBases()              { return bases; }
    public Location             getBaseLocation(UUID u) { return bases.get(u); }
    public boolean              hasBase(UUID u)         { return bases.containsKey(u); }
    public veinminerCommandExecutor getVeinMinerExecutor() { return veinMinerExecutor; }
    public BaseFlightMain       getBaseFlightMain()     { return baseFlightMain; }

    public boolean isPlayerJailed(Player player) {
        LibMain api = LibMain.getInstance();
        return api != null && api.isPlayerJailed(player.getUniqueId());
    }
}