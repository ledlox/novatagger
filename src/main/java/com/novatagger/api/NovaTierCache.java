package com.novatagger.api;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory only cache (clears on restart) with 5-minute expiry.
 * Never blocks: returns cached value or empty now, fills in background.
 */
public final class NovaTierCache {
    private static final long EXPIRY_MS = Duration.ofMinutes(5).toMillis();

    private record Entry(Optional<NovaPlayerInfo> info, long expiresAt) {
        boolean fresh() {
            return System.currentTimeMillis() < expiresAt;
        }
    }

    private final ConcurrentHashMap<UUID, Entry> cache = new ConcurrentHashMap<>();
    private final HttpClient client;

    public NovaTierCache(HttpClient client) {
        this.client = client;
    }

    /** Cached value if fresh, otherwise empty (triggers background refresh). */
    public Optional<NovaPlayerInfo> getCached(UUID uuid) {
        Entry entry = cache.get(uuid);
        if (entry != null && entry.fresh()) {
            return entry.info();
        }
        return Optional.empty();
    }

    /** Async fetch with cache: fresh cache hit resolves immediately, else fetches and stores. */
    public CompletableFuture<Optional<NovaPlayerInfo>> getAsync(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        Entry entry = cache.get(uuid);
        if (entry != null && entry.fresh()) {
            return CompletableFuture.completedFuture(entry.info());
        }
        return NovaPlayerInfo.fetchAsync(client, uuid).thenApply(info -> {
            cache.put(uuid, new Entry(info, System.currentTimeMillis() + EXPIRY_MS));
            return info;
        });
    }

    /** Fire-and-forget prefetch (used by render paths so data is ready next frame). */
    public void prefetch(UUID uuid) {
        Entry entry = cache.get(uuid);
        if (entry != null && entry.fresh()) {
            return;
        }
        getAsync(uuid);
    }
}
