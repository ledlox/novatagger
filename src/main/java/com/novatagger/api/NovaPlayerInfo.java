package com.novatagger.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.novatagger.NovaTaggerMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Mirrors the novatiers.com/users/{dashlessUuid} JSON shape.
 * Sample: {"minecraftUuid":"ffe5c48d...","minecraftUsername":"ledlox","region":"EU",
 * "tiers":{"Spear Mace":"LT3"},"peakTiers":{"Spear Mace":"LT3"},"retiredTiers":{"Spear Mace":false}}
 */
public record NovaPlayerInfo(
        String minecraftUuid,
        String minecraftUsername,
        String region,
        Map<String, String> tiers,
        Map<String, String> peakTiers,
        Map<String, Boolean> retiredTiers
) {
    public static final String API_BASE = "https://novatiers.com/users/";
    static final Gson GSON = new GsonBuilder().create();

    public NovaPlayerInfo {
        tiers = tiers != null ? tiers : Collections.emptyMap();
        peakTiers = peakTiers != null ? peakTiers : Collections.emptyMap();
        retiredTiers = retiredTiers != null ? retiredTiers : Collections.emptyMap();
    }

    /** Display tier for a mode, uppercased (e.g. "LT3"). Falls back to peak if current is missing. */
    public String displayTier(String mode) {
        String current = tiers.get(mode);
        if (current != null && !current.isBlank() && !current.equals("-")) {
            return current.toUpperCase();
        }
        String peak = peakTiers.get(mode);
        if (peak != null && !peak.isBlank() && !peak.equals("-")) {
            return peak.toUpperCase();
        }
        return "-";
    }

    public boolean isRetired(String mode) {
        return Boolean.TRUE.equals(retiredTiers.get(mode));
    }

    public static String toDashless(UUID uuid) {
        return uuid.toString().replace("-", "").toLowerCase();
    }

    public static CompletableFuture<Optional<NovaPlayerInfo>> fetchAsync(HttpClient client, UUID uuid) {
        String endpoint = API_BASE + toDashless(uuid);
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "NovaTagger/1.0.0")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        NovaPlayerInfo info = GSON.fromJson(body, NovaPlayerInfo.class);
                        if (info == null || info.minecraftUsername() == null) {
                            return Optional.<NovaPlayerInfo>empty();
                        }
                        return Optional.of(info);
                    } catch (Exception e) {
                        NovaTaggerMod.LOGGER.warn("Failed to parse NovaTiers response for {}: {}", uuid, e.toString());
                        return Optional.<NovaPlayerInfo>empty();
                    }
                })
                .exceptionally(t -> {
                    NovaTaggerMod.LOGGER.warn("Error fetching NovaTiers info ({}): {}", uuid, t.toString());
                    return Optional.empty();
                });
    }

    /** Mojang username -> UUID lookup for offline /novatag targets. Returns dashless id. */
    public record MojangProfile(String id, String name) {}

    public static CompletableFuture<Optional<UUID>> lookupMojangUuid(HttpClient client, String username) {
        String endpoint = "https://api.mojang.com/users/profiles/minecraft/" + username;
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "NovaTagger/1.0.0")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        return Optional.<UUID>empty();
                    }
                    try {
                        MojangProfile profile = GSON.fromJson(response.body(), MojangProfile.class);
                        if (profile == null || profile.id() == null) {
                            return Optional.<UUID>empty();
                        }
                        String dashed = profile.id().replaceFirst(
                                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                        return Optional.of(UUID.fromString(dashed));
                    } catch (Exception e) {
                        NovaTaggerMod.LOGGER.warn("Failed to parse Mojang response for {}: {}", username, e.toString());
                        return Optional.<UUID>empty();
                    }
                })
                .exceptionally(t -> {
                    NovaTaggerMod.LOGGER.warn("Error looking up Mojang UUID ({}): {}", username, t.toString());
                    return Optional.empty();
                });
    }
}
