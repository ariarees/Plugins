/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough.storage;

import com.google.gson.*;
import org.bukkit.Bukkit;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimerStorage {

    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Long> timers = new HashMap<>();

    public TimerStorage(File dataFolder) {
        this.file = new File(dataFolder, "jail_timers.json");
    }

    // -------------------------------------------------------------------------
    // Load / Save
    // -------------------------------------------------------------------------

    public void load() {
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) return;

            long now = System.currentTimeMillis();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                long releaseAt = entry.getValue().getAsLong();
                // Only load timers that haven't already expired.
                if (releaseAt > now) {
                    timers.put(UUID.fromString(entry.getKey()), releaseAt);
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("[JailedDough] Failed to load jail_timers.json: " + e.getMessage());
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<UUID, Long> entry : timers.entrySet()) {
            root.addProperty(entry.getKey().toString(), entry.getValue());
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[JailedDough] Failed to save jail_timers.json: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void store(UUID uuid, long releaseAtMs) {
        timers.put(uuid, releaseAtMs);
        save();
    }

    public Long get(UUID uuid) {
        return timers.get(uuid);
    }

    public boolean has(UUID uuid) {
        return timers.containsKey(uuid);
    }

    public void remove(UUID uuid) {
        timers.remove(uuid);
        save();
    }

    public Map<UUID, Long> getAll() {
        return timers;
    }
}