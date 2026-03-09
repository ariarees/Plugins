package win.dougmination.plural.api;

import com.google.gson.*;
import win.dougmination.plural.PluralMain;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

public class CloudApiClient {

    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson = new Gson();

    public CloudApiClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** Fetch full system data for a player on join. Returns null if not registered. */
    public PluralMain.PlayerSystemData fetchPlayerData(java.util.UUID uuid) {
        try {
            String uuidStr = uuid.toString().replace("-", "");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/plugin/player/minecraft/" + uuidStr))
                    .timeout(Duration.ofSeconds(6))
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 404) return null;
            if (resp.statusCode() != 200) {
                log("fetchPlayerData HTTP " + resp.statusCode());
                return null;
            }

            JsonObject body = JsonParser.parseString(resp.body()).getAsJsonObject();

            // Build system data — prefer system_name, fall back to discord_tag
            String systemName = safeStr(body, "system_name");
            if (systemName == null) systemName = safeStr(body, "discord_tag");
            PluralMain.PlayerSystemData data = new PluralMain.PlayerSystemData(uuid, systemName != null ? systemName : "");

            JsonArray membersArr = body.getAsJsonArray("members");
            for (JsonElement el : membersArr) {
                JsonObject m = el.getAsJsonObject();
                PluralMain.MemberInfo info = new PluralMain.MemberInfo();
                info.name        = m.get("name").getAsString();
                info.displayName = safeStr(m, "display_name");
                info.pronouns    = safeStr(m, "pronouns");
                info.color       = safeStr(m, "color");
                info.avatarUrl   = safeStr(m, "avatar_url");
                info.description = safeStr(m, "description");
                info.pkMemberId  = safeStr(m, "pk_member_id");
                data.members.put(info.name.toLowerCase(), info);
            }

            if (body.has("active_front") && !body.get("active_front").isJsonNull()) {
                JsonObject af = body.getAsJsonObject("active_front");
                if (af.has("member_names") && !af.get("member_names").isJsonNull()) {
                    for (JsonElement n : af.getAsJsonArray("member_names")) {
                        data.activeFrontNames.add(n.getAsString());
                    }
                }
            }

            return data;
        } catch (Exception e) {
            log("fetchPlayerData error: " + e.getMessage());
            return null;
        }
    }

    /** Push front switch to cloud (async, fire-and-forget). */
    public void pushFrontSwitch(java.util.UUID uuid, List<String> memberNames) {
        runAsync(() -> {
            try {
                String uuidStr = uuid.toString().replace("-", "");

                JsonObject body = new JsonObject();
                JsonArray arr = new JsonArray();
                memberNames.forEach(arr::add);
                body.add("member_names", arr);

                http.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + "/plugin/player/minecraft/" + uuidStr + "/front"))
                                .timeout(Duration.ofSeconds(6))
                                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                );
            } catch (Exception e) { log("pushFrontSwitch error: " + e.getMessage()); }
        });
    }

    /** Clear front on cloud (async). */
    public void pushFrontClear(java.util.UUID uuid) {
        runAsync(() -> {
            try {
                String uuidStr = uuid.toString().replace("-", "");

                http.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + "/plugin/player/minecraft/" + uuidStr + "/front"))
                                .timeout(Duration.ofSeconds(6))
                                .DELETE().build(),
                        HttpResponse.BodyHandlers.ofString()
                );
            } catch (Exception e) { log("pushFrontClear error: " + e.getMessage()); }
        });
    }

    /** Shut down the internal HttpClient thread pool. Call from onDisable(). */
    public void shutdown() {
        // HttpClient.close() available in Java 21+; on older runtimes the client
        // will be GC'd once the plugin classloader releases it.
        try {
            if (http instanceof AutoCloseable ac) {
                ac.close();
            }
        } catch (Exception ignored) {}
    }

    private void runAsync(Runnable r) {
        PluralMain plugin = PluralMain.getInstance();
        if (plugin == null || !plugin.isEnabled()) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);
    }

    private void log(String msg) {
        PluralMain.getInstance().getLogger().warning("[Plural] " + msg);
    }

    private String safeStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }
}