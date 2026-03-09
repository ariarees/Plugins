/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough.storage;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocationStorage {

    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    // Store as serialized form to avoid holding strong World references
    private final Map<UUID, JsonObject> locationData = new HashMap<>();

    public LocationStorage(File dataFolder) {
        this.file = new File(dataFolder, "pre_jail_locations.json");
    }

    // -------------------------------------------------------------------------
    // Load / Save
    // -------------------------------------------------------------------------

    public void load() {
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) return;

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                locationData.put(uuid, entry.getValue().getAsJsonObject());
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("[JailedDough] Failed to load pre_jail_locations.json: " + e.getMessage());
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<UUID, JsonObject> entry : locationData.entrySet()) {
            root.add(entry.getKey().toString(), entry.getValue());
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[JailedDough] Failed to save pre_jail_locations.json: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void store(UUID uuid, Location location) {
        locationData.put(uuid, serializeLocation(location));
        save();
    }

    public Location get(UUID uuid) {
        JsonObject obj = locationData.get(uuid);
        return obj != null ? deserializeLocation(obj) : null;
    }

    public boolean has(UUID uuid) {
        return locationData.containsKey(uuid);
    }

    public void remove(UUID uuid) {
        locationData.remove(uuid);
        save();
    }

    // -------------------------------------------------------------------------
    // Serialization helpers
    // -------------------------------------------------------------------------

    private JsonObject serializeLocation(Location loc) {
        JsonObject obj = new JsonObject();
        obj.addProperty("world", loc.getWorld().getName());
        obj.addProperty("x", loc.getX());
        obj.addProperty("y", loc.getY());
        obj.addProperty("z", loc.getZ());
        obj.addProperty("yaw", loc.getYaw());
        obj.addProperty("pitch", loc.getPitch());
        return obj;
    }

    private Location deserializeLocation(JsonObject obj) {
        World world = Bukkit.getWorld(obj.get("world").getAsString());
        double x     = obj.get("x").getAsDouble();
        double y     = obj.get("y").getAsDouble();
        double z     = obj.get("z").getAsDouble();
        float  yaw   = obj.get("yaw").getAsFloat();
        float  pitch = obj.get("pitch").getAsFloat();
        return new Location(world, x, y, z, yaw, pitch);
    }
}