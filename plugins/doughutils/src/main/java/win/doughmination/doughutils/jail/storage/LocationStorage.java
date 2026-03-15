/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.jail.storage;

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
    private final Map<UUID, JsonObject> locationData = new HashMap<>();

    public LocationStorage(File dataFolder) {
        this.file = new File(dataFolder, "pre_jail_locations.json");
    }

    public void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                locationData.put(UUID.fromString(entry.getKey()), entry.getValue().getAsJsonObject());
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("[DoughUtils] Failed to load pre_jail_locations.json: " + e.getMessage());
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
            Bukkit.getLogger().severe("[DoughUtils] Failed to save pre_jail_locations.json: " + e.getMessage());
        }
    }

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
        return new Location(world,
            obj.get("x").getAsDouble(), obj.get("y").getAsDouble(), obj.get("z").getAsDouble(),
            obj.get("yaw").getAsFloat(), obj.get("pitch").getAsFloat());
    }
}
