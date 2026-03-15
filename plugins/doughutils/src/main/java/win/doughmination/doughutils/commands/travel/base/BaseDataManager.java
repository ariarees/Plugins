/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel.base;

import com.google.gson.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages base metadata: name, trusted players, and name-change cooldowns.
 *
 * Stored in plugins/Doughminationcord/data/settings/<UUID>.json under the "basemeta" key:
 * {
 *   "basemeta": {
 *     "name": "My Base",
 *     "last_name_change": 1234567890,
 *     "trusted": ["uuid1", "uuid2"]
 *   }
 * }
 */
public class BaseDataManager {

    public static final long NAME_COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final JavaPlugin plugin;
    private final File settingsDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // In-memory cache
    private final Map<UUID, String>       baseNames        = new ConcurrentHashMap<>();
    private final Map<UUID, Long>         nameChangeTimes  = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>>    trustedPlayers   = new ConcurrentHashMap<>();

    public BaseDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.settingsDir = new File(plugin.getDataFolder(), "data/settings");
        if (!settingsDir.exists()) settingsDir.mkdirs();
    }

    // -----------------------------------------------------------------------
    // Load all existing player data into memory on startup
    // -----------------------------------------------------------------------

    public void loadAll() {
        File[] files = settingsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".json", ""));
                loadPlayer(uuid);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadPlayer(UUID uuid) {
        JsonObject data = loadRaw(uuid);
        if (!data.has("basemeta") || !data.get("basemeta").isJsonObject()) return;
        JsonObject meta = data.getAsJsonObject("basemeta");

        if (meta.has("name")) {
            baseNames.put(uuid, meta.get("name").getAsString());
        }
        if (meta.has("last_name_change")) {
            nameChangeTimes.put(uuid, meta.get("last_name_change").getAsLong());
        }
        if (meta.has("trusted") && meta.get("trusted").isJsonArray()) {
            Set<UUID> trusted = new HashSet<>();
            for (JsonElement el : meta.getAsJsonArray("trusted")) {
                try { trusted.add(UUID.fromString(el.getAsString())); }
                catch (IllegalArgumentException ignored) {}
            }
            if (!trusted.isEmpty()) trustedPlayers.put(uuid, trusted);
        }
    }

    // -----------------------------------------------------------------------
    // Base Name
    // -----------------------------------------------------------------------

    public Optional<String> getBaseName(UUID owner) {
        return Optional.ofNullable(baseNames.get(owner));
    }

    /**
     * Attempts to set the base name. Returns false (and does not update) if on cooldown.
     * Ops bypass the cooldown.
     */
    public boolean setBaseName(UUID owner, String name, boolean isOp) {
        if (!isOp) {
            long now = System.currentTimeMillis();
            Long last = nameChangeTimes.get(owner);
            if (last != null && now - last < NAME_COOLDOWN_MILLIS) return false;
            nameChangeTimes.put(owner, now);
        }
        baseNames.put(owner, name);
        persist(owner);
        return true;
    }

    /** Returns remaining cooldown millis, or 0 if not on cooldown. */
    public long getNameCooldownRemaining(UUID owner) {
        Long last = nameChangeTimes.get(owner);
        if (last == null) return 0L;
        long remaining = NAME_COOLDOWN_MILLIS - (System.currentTimeMillis() - last);
        return Math.max(0L, remaining);
    }

    // -----------------------------------------------------------------------
    // Trusted Players
    // -----------------------------------------------------------------------

    public Set<UUID> getTrusted(UUID owner) {
        return trustedPlayers.getOrDefault(owner, Collections.emptySet());
    }

    public boolean isTrusted(UUID owner, UUID visitor) {
        Set<UUID> set = trustedPlayers.get(owner);
        return set != null && set.contains(visitor);
    }

    public void trust(UUID owner, UUID target) {
        trustedPlayers.computeIfAbsent(owner, k -> new HashSet<>()).add(target);
        persist(owner);
    }

    public void untrust(UUID owner, UUID target) {
        Set<UUID> set = trustedPlayers.get(owner);
        if (set != null) {
            set.remove(target);
            if (set.isEmpty()) trustedPlayers.remove(owner);
        }
        persist(owner);
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    private void persist(UUID uuid) {
        JsonObject data = loadRaw(uuid);
        JsonObject meta = new JsonObject();

        String name = baseNames.get(uuid);
        if (name != null) meta.addProperty("name", name);

        Long lastChange = nameChangeTimes.get(uuid);
        if (lastChange != null) meta.addProperty("last_name_change", lastChange);

        JsonArray trustedArr = new JsonArray();
        Set<UUID> trusted = trustedPlayers.get(uuid);
        if (trusted != null) trusted.forEach(u -> trustedArr.add(u.toString()));
        meta.add("trusted", trustedArr);

        data.add("basemeta", meta);
        saveRaw(uuid, data);
    }

    private JsonObject loadRaw(UUID uuid) {
        File file = new File(settingsDir, uuid + ".json");
        if (!file.exists()) return new JsonObject();
        try (Reader r = new FileReader(file)) {
            JsonElement el = gson.fromJson(r, JsonElement.class);
            return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : new JsonObject();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load base meta for " + uuid + ": " + e.getMessage());
            return new JsonObject();
        }
    }

    private void saveRaw(UUID uuid, JsonObject data) {
        File file = new File(settingsDir, uuid + ".json");
        try (Writer w = new FileWriter(file)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save base meta for " + uuid + ": " + e.getMessage());
        }
    }
}
