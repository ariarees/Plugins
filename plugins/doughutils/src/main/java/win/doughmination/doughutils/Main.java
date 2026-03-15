/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bstats.bukkit.Metrics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import win.doughmination.doughutils.commands.chests.*;
import win.doughmination.doughutils.commands.moderation.*;
import win.doughmination.doughutils.commands.other.*;
import win.doughmination.doughutils.commands.roleplay.*;
import win.doughmination.doughutils.commands.travel.base.*;
import win.doughmination.doughutils.commands.travel.*;
import win.doughmination.doughutils.data.*;
import win.doughmination.doughutils.endstats.*;
import win.doughmination.doughutils.jail.commands.*;
import win.doughmination.doughutils.jail.listeners.*;
import win.doughmination.doughutils.jail.storage.*;
import win.doughmination.doughutils.listeners.*;
import win.doughmination.doughutils.listeners.potions.*;
import win.doughmination.doughutils.listeners.spawneggs.*;
import win.doughmination.doughutils.listeners.travel.*;
import win.doughmination.doughutils.listeners.veinminer.*;
import win.doughmination.doughutils.wingsync.*;
import win.doughmination.doughutils.wingsync.commands.*;
import win.doughmination.doughutils.wingsync.listeners.*;

public class Main extends JavaPlugin {

    private Metrics metrics;
    private final Map<UUID, Long>     playtimeMap     = new HashMap<>();
    private final Map<UUID, Long>     loginTimestamps = new HashMap<>();
    private final Map<UUID, Location> bases           = new HashMap<>();
    private final Map<UUID, Long>     jailMap         = new ConcurrentHashMap<>();

    private PlayerDataManager        playerDataManager;
    private BanManager               banManager;
    private VChestCommandExecutor    vChestExecutor;
    private veinminerCommandExecutor veinMinerExecutor;
    private BaseFlightMain           baseFlightMain;
    private TeleportRequestManager   teleportRequestManager;
    private BaseCommandExecutor      baseCommandExecutor;
    private BaseDataManager          baseDataManager;
    private BackLocationManager      backLocationManager;

    // End stats
    private EventManager             eventManager;

    // Jail
    private Location                 jailLocation;
    private LocationStorage          locationStorage;
    private TimerStorage             timerStorage;

    // WingSync
    private WingSyncManager          wingSyncManager;

    @Override
    public void onEnable() {
        int pluginId = 30146;
        metrics = new Metrics(this, pluginId);

        getLogger().info("DoughUtils is starting up...");

        saveDefaultConfig();

        playerDataManager = new PlayerDataManager(this);

        banManager = new BanManager(this);
        banManager.load();

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

        // End stats
        eventManager = new EventManager(this);
        getServer().getPluginManager().registerEvents(new EventListener(eventManager), this);

        // Jail
        locationStorage = new LocationStorage(getDataFolder());
        locationStorage.load();
        timerStorage = new TimerStorage(getDataFolder());
        timerStorage.load();
        loadJailLocation();
        startUnjailTask();
        getServer().getPluginManager().registerEvents(new JailListener(this), this);

        // WingSync
        wingSyncManager = new WingSyncManager(this);
        wingSyncManager.onEnable();
        getServer().getPluginManager().registerEvents(new GeneralListener(this), this);

        SpawnEggRecipes.resetSymbolMap();
        for (SpawnEggRecipes recipe : SpawnEggRecipes.values()) {
            Bukkit.removeRecipe(new NamespacedKey(this, recipe.getKey()));
        }
        RecipeManager.registerRecipes(this);
        PotionRecipeManager.registerRecipes(this);

        String version = getConfig().getString("version", "unknown");
        ModrinthUpdateChecker.check(this, "wHpRTEmg", "doughutils", version);
    }

    @Override
    public void onDisable() {
        getLogger().info("DoughUtils is shutting down...");
        if (baseFlightMain != null) baseFlightMain.onDisable();
        if (teleportRequestManager != null) teleportRequestManager.onDisable();
        if (baseCommandExecutor != null) baseCommandExecutor.shutdown();
        saveAllBases();
        saveAllPlaytime();
        vChestExecutor.saveAll();
        banManager.save();
        if (eventManager != null) eventManager.saveData();
        if (locationStorage != null) locationStorage.save();
        if (timerStorage != null) timerStorage.save();
        saveJailLocation();
        if (wingSyncManager != null) wingSyncManager.onDisable();
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

        // End stats
        getCommand("estart").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("dragonevent.admin")) { sender.sendRichMessage("<red>No permission.</red>"); return true; }
            eventManager.startEvent();
            sender.sendRichMessage("<green>Dragon event started!</green>");
            getServer().broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<yellow>Dragon Event has begun! Head to the End to participate!</yellow>"));
            return true;
        });
        getCommand("estop").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("dragonevent.admin")) { sender.sendRichMessage("<red>No permission.</red>"); return true; }
            eventManager.stopEvent();
            sender.sendRichMessage("<red>Dragon event stopped and reset!</red>");
            return true;
        });

        // Jail
        getCommand("setjail").setExecutor(new SetJailCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("jailreload").setExecutor(new JailReloadCommand(this));
        getCommand("jailhelp").setExecutor(new JailHelpCommand());

        // WingSync
        getCommand("wsreload").setExecutor(new WsReloadCommand(this));
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

    public BackLocationManager      getBackLocationManager()      { return backLocationManager; }
    public PlayerDataManager        getPlayerDataManager()        { return playerDataManager; }
    public BanManager               getBanManager()               { return banManager; }
    public BaseDataManager          getBaseDataManager()          { return baseDataManager; }
    public TeleportRequestManager   getTeleportRequestManager()   { return teleportRequestManager; }
    public Map<UUID, Long>          getPlaytimeMap()              { return playtimeMap; }
    public Map<UUID, Long>          getLoginTimestamps()          { return loginTimestamps; }
    public Map<UUID, Location>      getBases()                    { return bases; }
    public Location                 getBaseLocation(UUID u)       { return bases.get(u); }
    public boolean                  hasBase(UUID u)               { return bases.containsKey(u); }
    public veinminerCommandExecutor getVeinMinerExecutor()        { return veinMinerExecutor; }
    public BaseFlightMain           getBaseFlightMain()           { return baseFlightMain; }
    public EventManager             getEventManager()             { return eventManager; }
    public LocationStorage          getLocationStorage()          { return locationStorage; }
    public TimerStorage             getTimerStorage()             { return timerStorage; }
    public WingSyncManager          getWingSyncManager()          { return wingSyncManager; }

    // -------------------------------------------------------------------------
    // Jail location (config-backed)
    // -------------------------------------------------------------------------

    public Location getJailLocation()         { return jailLocation; }
    public void setJailLocation(Location loc) { this.jailLocation = loc; }

    public void loadJailLocation() {
        if (!getConfig().contains("jail.world")) return;
        org.bukkit.World world = getServer().getWorld(getConfig().getString("jail.world"));
        if (world == null) return;
        jailLocation = new Location(world,
            getConfig().getDouble("jail.x"),
            getConfig().getDouble("jail.y"),
            getConfig().getDouble("jail.z"),
            (float) getConfig().getDouble("jail.yaw"),
            (float) getConfig().getDouble("jail.pitch"));
    }

    public void saveJailLocation() {
        if (jailLocation == null) return;
        getConfig().set("jail.world",  jailLocation.getWorld().getName());
        getConfig().set("jail.x",      jailLocation.getX());
        getConfig().set("jail.y",      jailLocation.getY());
        getConfig().set("jail.z",      jailLocation.getZ());
        getConfig().set("jail.yaw",    (double) jailLocation.getYaw());
        getConfig().set("jail.pitch",  (double) jailLocation.getPitch());
        saveConfig();
    }

    // -------------------------------------------------------------------------
    // Auto-unjail background task
    // -------------------------------------------------------------------------

    private void startUnjailTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : new java.util.HashMap<>(timerStorage.getAll()).entrySet()) {
                UUID id = entry.getKey();
                if (now < entry.getValue()) continue;

                Player player = getServer().getPlayer(id);
                if (player != null) {
                    Location returnLoc = locationStorage.get(id);
                    if (returnLoc != null) {
                        player.teleport(returnLoc);
                        player.sendRichMessage("<green>You have been released from jail!</green>");
                    }
                }
                locationStorage.remove(id);
                timerStorage.remove(id);
                unjailPlayer(id);
            }
        }, 20L, 20L);
    }

    // -------------------------------------------------------------------------
    // Jail API (replaces LibMain.isPlayerJailed / canUseCommand)
    // -------------------------------------------------------------------------

    public void jailPlayer(UUID uuid, long releaseAtMs) {
        jailMap.put(uuid, releaseAtMs);
    }

    public void unjailPlayer(UUID uuid) {
        jailMap.remove(uuid);
    }

    public boolean isPlayerJailed(UUID uuid) {
        Long release = jailMap.get(uuid);
        if (release == null) return false;
        if (release > 0 && System.currentTimeMillis() >= release) {
            jailMap.remove(uuid);
            return false;
        }
        return true;
    }

    public boolean isPlayerJailed(Player player) {
        return isPlayerJailed(player.getUniqueId());
    }

    /** Returns false (blocking the command) if the player is jailed. */
    public boolean canUseCommand(Player player) {
        return !isPlayerJailed(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Ban API (delegates to BanManager)
    // -------------------------------------------------------------------------

    public void banPlayer(UUID uuid, String name, String reason, String bannedBy, UUID bannedByUUID) {
        banManager.banPlayer(uuid, name, reason, bannedBy, bannedByUUID);
    }

    public void unbanPlayer(UUID uuid, String unbannedBy) {
        banManager.unbanPlayer(uuid, unbannedBy);
    }

    public boolean isPlayerBanned(UUID uuid) {
        return banManager.isPlayerBanned(uuid);
    }

    public BanData getBanData(UUID uuid) {
        return banManager.getBanData(uuid);
    }

    public Map<UUID, BanData> getAllBans() {
        return banManager.getAllBans();
    }
}

