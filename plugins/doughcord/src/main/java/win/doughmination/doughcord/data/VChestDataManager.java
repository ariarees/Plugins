/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.data;

import com.google.gson.*;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-player VIP chest files at:
 *   plugins/Doughminationcord/data/vchests/<UUID>.json
 *
 * Schema:
 * {
 *   "slot_0":  "<base64-encoded ItemStack>",
 *   "slot_7":  "<base64-encoded ItemStack>",
 *   ...
 * }
 * Only occupied slots are written.
 */
public class VChestDataManager {

    private final File vchestsDir;
    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public VChestDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.vchestsDir = new File(plugin.getDataFolder(), "data/vchests");
        if (!vchestsDir.exists()) vchestsDir.mkdirs();
    }

    // -----------------------------------------------------------------------
    // Load
    // -----------------------------------------------------------------------

    /** Populates the provided Inventory from the player's saved JSON file. */
    public void loadInto(UUID uuid, Inventory inv) {
        File file = fileFor(uuid);
        if (!file.exists()) return;

        JsonObject data;
        try (Reader reader = new FileReader(file)) {
            JsonElement parsed = gson.fromJson(reader, JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) return;
            data = parsed.getAsJsonObject();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load vchest for " + uuid + ": " + e.getMessage());
            return;
        }

        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("slot_")) continue;
            try {
                int slot = Integer.parseInt(key.substring(5));
                String encoded = entry.getValue().getAsString();
                ItemStack item = decodeItem(encoded);
                if (item != null && slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Skipping invalid vchest slot key '" + key + "' for " + uuid);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    /** Saves every occupied slot from the inventory to the player's JSON file. */
    public void save(UUID uuid, Inventory inv) {
        JsonObject data = new JsonObject();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                String encoded = encodeItem(contents[i]);
                if (encoded != null) data.addProperty("slot_" + i, encoded);
            }
        }

        File file = fileFor(uuid);
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save vchest for " + uuid + ": " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private File fileFor(UUID uuid) {
        return new File(vchestsDir, uuid.toString() + ".json");
    }

    private String encodeItem(ItemStack item) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeObject(item);
            return Base64Coder.encodeLines(baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not encode ItemStack: " + e.getMessage());
            return null;
        }
    }

    private ItemStack decodeItem(String encoded) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64Coder.decodeLines(encoded));
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) bois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("Could not decode ItemStack: " + e.getMessage());
            return null;
        }
    }
}