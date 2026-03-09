package win.doughmination.statsend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.util.*;

public class EventManager {

    private final StatsMain plugin;
    private final Gson gson;
    private final File dataFile;

    private boolean eventActive = false;
    private long eventStartTime = 0;
    private long firstDragonDeathTime = 0;

    private Set<UUID> eggHolders = new HashSet<>();
    private UUID daggerHolder = null;
    private Set<UUID> crossedSwordsHolders = new HashSet<>();
    private UUID muscleToneHolder = null;
    private UUID wingHolder = null;
    private UUID packageHolder = null;

    private Map<UUID, Double> dragonDamage = new HashMap<>();
    private boolean firstDragonKilled = false;

    public EventManager(StatsMain plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "event_data.json");
        loadData();
    }

    public void startEvent() {
        eventActive = true;
        eventStartTime = System.currentTimeMillis();
        firstDragonDeathTime = 0;

        eggHolders.clear();
        crossedSwordsHolders.clear();
        dragonDamage.clear();
        daggerHolder = null;
        muscleToneHolder = null;
        wingHolder = null;
        packageHolder = null;
        firstDragonKilled = false;

        plugin.getLogger().info("Dragon event started!");
        saveData();
    }

    public void stopEvent() {
        eventActive = false;
        plugin.getLogger().info("Dragon event stopped!");
        saveData();
    }

    public boolean isEventActive() {
        return eventActive;
    }

    public void addEggHolder(UUID playerId) {
        if (eventActive) {
            eggHolders.add(playerId);
            plugin.getLogger().info("Added egg holder: " + playerId);
            saveData();
        }
    }

    public void setDaggerHolder(UUID playerId) {
        if (eventActive && !firstDragonKilled) {
            daggerHolder = playerId;
            firstDragonKilled = true;
            firstDragonDeathTime = System.currentTimeMillis();
            plugin.getLogger().info("Set dagger holder: " + playerId);
            saveData();
        }
    }

    public void addCrossedSwordsHolder(UUID playerId) {
        if (eventActive && firstDragonKilled) {
            long timeSinceFirstDragon = System.currentTimeMillis() - firstDragonDeathTime;
            if (timeSinceFirstDragon <= 24 * 60 * 60 * 1000) { // 24 hours
                crossedSwordsHolders.add(playerId);
                plugin.getLogger().info("Added crossed swords holder: " + playerId);
                saveData();
            }
        }
    }

    public void addDragonDamage(UUID playerId, double damage) {
        if (eventActive && !firstDragonKilled) {
            // Only accumulate in memory - do NOT save on every hit as dragon fights
            // fire dozens of damage events per second, causing excessive I/O
            dragonDamage.merge(playerId, damage, Double::sum);
        }
    }

    public void calculateMuscleToneHolder() {
        if (eventActive && firstDragonKilled && muscleToneHolder == null) {
            UUID topDamager = null;
            double maxDamage = 0;

            for (Map.Entry<UUID, Double> entry : dragonDamage.entrySet()) {
                if (!entry.getKey().equals(daggerHolder) && entry.getValue() > maxDamage) {
                    maxDamage = entry.getValue();
                    topDamager = entry.getKey();
                }
            }

            if (topDamager != null) {
                muscleToneHolder = topDamager;
                plugin.getLogger().info("Set muscle tone holder: " + topDamager + " with " + maxDamage + " damage");
                saveData();
            }
        }
    }

    public void setWingHolder(UUID playerId) {
        if (eventActive && wingHolder == null) {
            wingHolder = playerId;
            plugin.getLogger().info("Set wing holder: " + playerId);
            saveData();
        }
    }

    public void setPackageHolder(UUID playerId) {
        if (eventActive && packageHolder == null) {
            packageHolder = playerId;
            plugin.getLogger().info("Set package holder: " + playerId);
            saveData();
        }
    }

    public void saveData() {
        try {
            EventData data = new EventData();
            data.eventActive = eventActive;
            data.eventStartTime = eventStartTime;
            data.firstDragonDeathTime = firstDragonDeathTime;
            data.eggHolders = new ArrayList<>(eggHolders);
            data.daggerHolder = daggerHolder;
            data.crossedSwordsHolders = new ArrayList<>(crossedSwordsHolders);
            data.muscleToneHolder = muscleToneHolder;
            data.wingHolder = wingHolder;
            data.packageHolder = packageHolder;
            // Store as String keys - Gson cannot deserialize UUID map keys natively
            Map<String, Double> damageByString = new HashMap<>();
            dragonDamage.forEach((k, v) -> damageByString.put(k.toString(), v));
            data.dragonDamage = damageByString;
            data.firstDragonKilled = firstDragonKilled;

            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save event data: " + e.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            EventData data = gson.fromJson(reader, EventData.class);

            if (data != null) {
                eventActive = data.eventActive;
                eventStartTime = data.eventStartTime;
                firstDragonDeathTime = data.firstDragonDeathTime;
                eggHolders = new HashSet<>(data.eggHolders != null ? data.eggHolders : new ArrayList<>());
                daggerHolder = data.daggerHolder;
                crossedSwordsHolders = new HashSet<>(data.crossedSwordsHolders != null ? data.crossedSwordsHolders : new ArrayList<>());
                muscleToneHolder = data.muscleToneHolder;
                wingHolder = data.wingHolder;
                packageHolder = data.packageHolder;
                // Deserialize String-keyed map back to UUID-keyed map
                dragonDamage = new HashMap<>();
                if (data.dragonDamage != null) {
                    data.dragonDamage.forEach((k, v) -> dragonDamage.put(UUID.fromString(k.toString()), v));
                }
                firstDragonKilled = data.firstDragonKilled;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load event data: " + e.getMessage());
        }
    }

    // Getters
    public Set<UUID> getEggHolders() { return new HashSet<>(eggHolders); }
    public UUID getDaggerHolder() { return daggerHolder; }
    public Set<UUID> getCrossedSwordsHolders() { return new HashSet<>(crossedSwordsHolders); }
    public UUID getMuscleToneHolder() { return muscleToneHolder; }
    public UUID getWingHolder() { return wingHolder; }
    public UUID getPackageHolder() { return packageHolder; }
}