/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import org.bstats.bukkit.Metrics;
import win.doughmination.api.LibMain;
import win.doughmination.jaileddough.commands.*;
import win.doughmination.jaileddough.listeners.*;
import win.doughmination.jaileddough.storage.*;

import java.util.Map;
import java.util.UUID;

public class JailMain extends JavaPlugin {

    private Location jailLocation;
    private LocationStorage locationStorage;
    private TimerStorage timerStorage;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onLoad() {
        getLogger().info("JailedDough is loading...");
        LibMain doughApi = LibMain.getInstance();
        if (doughApi == null) {
            getLogger().warning("DoughAPI instance is null during onLoad. This may resolve during onEnable.");
        } else {
            getLogger().info("DoughAPI instance is accessible during onLoad.");
        }
    }

    @Override
    public void onEnable() {
        new Metrics(this, 29924);

        new BukkitRunnable() {
            @Override
            public void run() {
                LibMain doughApi = LibMain.getInstance();
                if (doughApi == null) {
                    getLogger().severe("DoughAPI is still not initialised! Disabling JailedDough.");
                    getServer().getPluginManager().disablePlugin(JailMain.this);
                    return;
                }

                getLogger().info("DoughAPI successfully accessed. Hash: " + System.identityHashCode(doughApi));

                saveDefaultConfig();
                loadJailLocation();

                locationStorage = new LocationStorage(getDataFolder());
                locationStorage.load();

                timerStorage = new TimerStorage(getDataFolder());
                timerStorage.load();

                registerCommands();
                registerListeners();
                startUnjailTask();

                getLogger().info("JailedDough enabled successfully!");
            }
        }.runTaskLater(this, 1L);
    }

    @Override
    public void onDisable() {
        saveJailLocation();
        if (locationStorage != null) locationStorage.save();
        if (timerStorage != null) timerStorage.save();
        getLogger().info("JailedDough has been disabled!");
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    private void registerCommands() {
        getCommand("setjail").setExecutor(new SetJailCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("jailreload").setExecutor(new JailReloadCommand(this));
        getCommand("jailhelp").setExecutor(new JailHelpCommand());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JailListener(), this);
    }

    // -------------------------------------------------------------------------
    // Auto-unjail task
    // -------------------------------------------------------------------------

    private void startUnjailTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Map.Entry<UUID, Long> entry : timerStorage.getAll().entrySet()) {
                    UUID id = entry.getKey();
                    if (now < entry.getValue()) continue;

                    Player player = getServer().getPlayer(id);
                    if (player != null) {
                        Location returnLoc = locationStorage.get(id);
                        if (returnLoc != null) {
                            player.teleport(returnLoc);
                            player.sendRichMessage("<green>You have been released from jail!</green>");
                        } else {
                            getLogger().warning("No pre-jail location for " + player.getName() + " during auto-unjail!");
                        }
                    }

                    locationStorage.remove(id);
                    timerStorage.remove(id);

                    LibMain doughApi = LibMain.getInstance();
                    if (doughApi != null) {
                        doughApi.clearPlayerJailData(id);
                    } else {
                        getLogger().severe("DoughAPI is null during auto-unjail task!");
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------

    public void loadJailLocation() {
        if (!getConfig().contains("jail")) return;
        World world = getServer().getWorld(getConfig().getString("jail.world"));
        jailLocation = new Location(
                world,
                getConfig().getDouble("jail.x"),
                getConfig().getDouble("jail.y"),
                getConfig().getDouble("jail.z"),
                (float) getConfig().getDouble("jail.yaw"),
                (float) getConfig().getDouble("jail.pitch")
        );
    }

    public void saveJailLocation() {
        if (jailLocation == null) return;
        getConfig().set("jail.world", jailLocation.getWorld().getName());
        getConfig().set("jail.x",     jailLocation.getX());
        getConfig().set("jail.y",     jailLocation.getY());
        getConfig().set("jail.z",     jailLocation.getZ());
        getConfig().set("jail.yaw",   (double) jailLocation.getYaw());
        getConfig().set("jail.pitch", (double) jailLocation.getPitch());
        saveConfig();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Location getJailLocation()           { return jailLocation; }
    public void setJailLocation(Location loc)   { this.jailLocation = loc; }

    public LocationStorage getLocationStorage() { return locationStorage; }
    public TimerStorage getTimerStorage()       { return timerStorage; }
}