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
import win.doughmination.doughcord.commands.chests.*;
import win.doughmination.doughcord.commands.moderation.*;
import win.doughmination.doughcord.commands.other.*;
import win.doughmination.doughcord.commands.roleplay.*;
import win.doughmination.doughcord.commands.travel.base.*;
import win.doughmination.doughcord.commands.travel.*;
import win.doughmination.doughcord.data.*;
import win.doughmination.doughcord.listeners.*;
import win.doughmination.doughcord.listeners.potions.*;
import win.doughmination.doughcord.listeners.spawneggs.*;
import win.doughmination.doughcord.listeners.travel.*;
import win.doughmination.doughcord.listeners.veinminer.*;

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
    private BackLocationManager backLocationManager;

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

        backLocationManager = new BackLocationManager();

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
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);

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
        reg("doughban",     banExec);
        UnbanCommandExecutor unbanExec = new UnbanCommandExecutor(this);
        reg("doughpardon",  unbanExec);
        reg("doughbanlist", new BanlistCommandExecutor(this));
        reg("spin", new spinCommandExecutor(this));
        reg("seen",  new SeenCommandExecutor(this));
        reg("tps",   new TpsCommandExecutor(this));
        reg("craft", new CraftCommandExecutor(this));
        reg("back",  new BackCommandExecutor(this));
        reg("beef",  new BeefCommandExecutor(this));
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
        // Flush any players still online at shutdown (e.g. /stop with players connected).
        // Normal logout saving is handled by PlayerSessionListener#onPlayerQuit.
        long now = System.currentTimeMillis();
        for (Player p : getServer().getOnlinePlayers()) {
            java.util.UUID uuid = p.getUniqueId();
            long sessionStart = loginTimestamps.getOrDefault(uuid, now);
            playtimeMap.merge(uuid, now - sessionStart, Long::sum);
            playerDataManager.savePlaytime(uuid, playtimeMap.getOrDefault(uuid, 0L));
            playerDataManager.saveLastSeen(uuid, now);
            veinMinerExecutor.saveAndUnloadForPlayer(uuid);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public BackLocationManager getBackLocationManager() { return backLocationManager; }
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
