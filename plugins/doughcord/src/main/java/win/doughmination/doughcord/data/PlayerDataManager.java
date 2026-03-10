/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.data;

import com.google.gson.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.UUID;

/**
 * Manages per-player settings files at:
 *   plugins/Doughminationcord/data/settings/<UUID>.json
 *
 * Schema per file:
 * {
 *   "playtime": 12345678,
 *   "veinminer": { "ores": true, "trees": true },
 *   "flight": true,
 *   "base": { "world": "world", "x": 0.0, "y": 64.0, "z": 0.0, "yaw": 0.0, "pitch": 0.0 }
 * }
 */
public class PlayerDataManager {

    private final File settingsDir;
    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.settingsDir = new File(plugin.getDataFolder(), "data/settings");
        if (!settingsDir.exists()) settingsDir.mkdirs();
    }

    // -----------------------------------------------------------------------
    // First-join initialisation
    // -----------------------------------------------------------------------

    /**
     * Called on player join. If no settings file exists yet, creates one with
     * all default values written out explicitly so the file is human-readable
     * from day one. If a file already exists, this is a no-op.
     */
    public void initPlayerFile(UUID uuid) {
        File file = fileFor(uuid);
        if (file.exists()) return;

        JsonObject data = new JsonObject();

        data.addProperty("playtime", 0L);
        data.addProperty("lastSeen", 0L);
        data.addProperty("flight", false);

        JsonObject vm = new JsonObject();
        vm.addProperty("ores", true);
        vm.addProperty("trees", true);
        data.add("veinminer", vm);

        // "base" is intentionally omitted — it is written when the player runs /base set

        save(uuid, data);
        plugin.getLogger().info("Created settings file for " + uuid);
    }

    // -----------------------------------------------------------------------
    // Load
    // -----------------------------------------------------------------------

    /** Loads the full JSON object for a player, or returns an empty object if none exists. */
    public JsonObject load(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) return new JsonObject();
        try (Reader reader = new FileReader(file)) {
            JsonElement parsed = gson.fromJson(reader, JsonElement.class);
            return (parsed != null && parsed.isJsonObject()) ? parsed.getAsJsonObject() : new JsonObject();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load settings for " + uuid + ": " + e.getMessage());
            return new JsonObject();
        }
    }

    public long loadPlaytime(UUID uuid) {
        JsonObject data = load(uuid);
        return data.has("playtime") ? data.get("playtime").getAsLong() : 0L;
    }

    /** Returns the epoch-millisecond timestamp of the player's last logout, or 0 if never recorded. */
    public long loadLastSeen(UUID uuid) {
        JsonObject data = load(uuid);
        return data.has("lastSeen") ? data.get("lastSeen").getAsLong() : 0L;
    }

    public boolean loadVeinminerOres(UUID uuid) {
        return loadVeinminerFlag(uuid, "ores");
    }

    public boolean loadVeinminerTrees(UUID uuid) {
        return loadVeinminerFlag(uuid, "trees");
    }

    public boolean loadFlightToggle(UUID uuid) {
        JsonObject data = load(uuid);
        return data.has("flight") && data.get("flight").getAsBoolean();
    }

    /** Returns null if no base has been saved yet. */
    public Location loadBase(UUID uuid) {
        JsonObject data = load(uuid);
        if (!data.has("base") || !data.get("base").isJsonObject()) return null;
        JsonObject base = data.getAsJsonObject("base");
        try {
            String worldName = base.get("world").getAsString();
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Unknown world '" + worldName + "' for base of " + uuid);
                return null;
            }
            return new Location(
                    world,
                    base.get("x").getAsDouble(),
                    base.get("y").getAsDouble(),
                    base.get("z").getAsDouble(),
                    base.get("yaw").getAsFloat(),
                    base.get("pitch").getAsFloat()
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Corrupt base data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Save helpers — each saves only the field it owns, preserving the rest
    // -----------------------------------------------------------------------

    public void savePlaytime(UUID uuid, long playtimeMs) {
        JsonObject data = load(uuid);
        data.addProperty("playtime", playtimeMs);
        save(uuid, data);
    }

    /** Saves the epoch-millisecond timestamp of when the player last logged out. */
    public void saveLastSeen(UUID uuid, long timestampMs) {
        JsonObject data = load(uuid);
        data.addProperty("lastSeen", timestampMs);
        save(uuid, data);
    }

    public void saveVeinminer(UUID uuid, boolean ores, boolean trees) {
        JsonObject data = load(uuid);
        JsonObject vm = new JsonObject();
        vm.addProperty("ores", ores);
        vm.addProperty("trees", trees);
        data.add("veinminer", vm);
        save(uuid, data);
    }

    public void saveFlightToggle(UUID uuid, boolean enabled) {
        JsonObject data = load(uuid);
        data.addProperty("flight", enabled);
        save(uuid, data);
    }

    public void saveBase(UUID uuid, Location loc) {
        JsonObject data = load(uuid);
        if (loc == null) {
            data.remove("base");
        } else {
            JsonObject base = new JsonObject();
            base.addProperty("world", loc.getWorld().getName());
            base.addProperty("x",     loc.getX());
            base.addProperty("y",     loc.getY());
            base.addProperty("z",     loc.getZ());
            base.addProperty("yaw",   (double) loc.getYaw());
            base.addProperty("pitch", (double) loc.getPitch());
            data.add("base", base);
        }
        save(uuid, data);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private boolean loadVeinminerFlag(UUID uuid, String key) {
        JsonObject data = load(uuid);
        if (!data.has("veinminer") || !data.get("veinminer").isJsonObject()) return true; // default on
        JsonObject vm = data.getAsJsonObject("veinminer");
        return !vm.has(key) || vm.get(key).getAsBoolean();
    }

    private void save(UUID uuid, JsonObject data) {
        File file = fileFor(uuid);
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save settings for " + uuid + ": " + e.getMessage());
        }
    }

    private File fileFor(UUID uuid) {
        return new File(settingsDir, uuid.toString() + ".json");
    }
}