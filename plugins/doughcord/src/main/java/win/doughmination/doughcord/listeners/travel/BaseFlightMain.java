/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.travel;

import win.doughmination.doughcord.CordMain;
import org.bukkit.Bukkit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BaseFlightMain {

    private final CordMain doughPlugin;
    private final Map<UUID, Boolean> flightToggles;
    private final Map<String, FlyZone> communalFlyZones;
    private org.bukkit.scheduler.BukkitTask flightCheckTask;

    public BaseFlightMain(CordMain plugin) {
        this.doughPlugin = plugin;
        this.flightToggles = new HashMap<>();
        this.communalFlyZones = new HashMap<>();
    }

    public void onEnable() {
        // Register commands and events
        Bukkit.getPluginManager().registerEvents(new FlightListener(this, doughPlugin), doughPlugin);

        doughPlugin.getCommand("flyzone").setExecutor(new FlyZoneCommandExecutor(this));
        doughPlugin.getCommand("rmflyzone").setExecutor(new RemoveFlyZoneCommandExecutor(this));

        // Pre-load flight toggles for any players who already have settings files
        java.io.File settingsDir = new java.io.File(doughPlugin.getDataFolder(), "data/settings");
        if (settingsDir.exists()) {
            java.io.File[] settingsFiles = settingsDir.listFiles();
            if (settingsFiles == null) return;
            for (java.io.File f : settingsFiles) {
                String name = f.getName();
                if (!name.endsWith(".json")) continue;
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(name.replace(".json", ""));
                    boolean toggle = doughPlugin.getPlayerDataManager().loadFlightToggle(uuid);
                    if (toggle) flightToggles.put(uuid, true);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Start flight zone checks
        flightCheckTask = new FlightCheckTask(this, doughPlugin).runTaskTimer(doughPlugin, 20L, 20L);
    }

    public void onDisable() {
        if (flightCheckTask != null) {
            flightCheckTask.cancel();
            flightCheckTask = null;
        }
    }

    public Map<UUID, Boolean> getFlightToggles() {
        return flightToggles;
    }

    public Map<String, FlyZone> getCommunalFlyZones() {
        return communalFlyZones;
    }

    public static class FlyZone {
        private final String name;
        private final org.bukkit.Location corner1;
        private final org.bukkit.Location corner2;

        public FlyZone(String name, org.bukkit.Location corner1, org.bukkit.Location corner2) {
            this.name = name;
            this.corner1 = corner1;
            this.corner2 = corner2;
        }

        public boolean isWithinZone(org.bukkit.Location location) {
            double minX = Math.min(corner1.getX(), corner2.getX());
            double maxX = Math.max(corner1.getX(), corner2.getX());
            double minY = Math.min(corner1.getY(), corner2.getY());
            double maxY = Math.max(corner1.getY(), corner2.getY());
            double minZ = Math.min(corner1.getZ(), corner2.getZ());
            double maxZ = Math.max(corner1.getZ(), corner2.getZ());

            return location.getX() >= minX && location.getX() <= maxX &&
                    location.getY() >= minY && location.getY() <= maxY &&
                    location.getZ() >= minZ && location.getZ() <= maxZ;
        }

        public String getName() {
            return name;
        }
    }
}