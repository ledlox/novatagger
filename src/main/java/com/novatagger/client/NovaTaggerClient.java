package com.novatagger.client;

import com.novatagger.NovaTaggerMod;
import com.novatagger.api.NovaPlayerInfo;
import com.novatagger.config.NovaTaggerConfig;
import com.novatagger.api.NovaTierCache;
import com.novatagger.api.NovaTierFormatter;
import com.novatagger.command.NovaTagCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NovaTaggerClient implements ClientModInitializer {
    /** NovaTiers modes in site order (from novatiers.com js/script.js GAMEMODES). */
    public static final List<String> MODES = List.of(
            "Spear Mace", "Elytra Spear", "Modern SMP", "SMP", "Diamond OP", "Spleef",
            "Pufferfish", "UHC", "Diamond Cart", "Vanilla", "Axe", "Elytra"
    );

    private static NovaTierCache tierCache;
    private static HttpClient httpClient;
    private static KeyBinding cycleModeKey;
    /** -1 = Best (auto), otherwise index into MODES. */
    private static int modeIndex = -1;

    public static NovaTierCache getTierCache() {
        return tierCache;
    }

    public static HttpClient getHttpClient() {
        return httpClient;
    }

    public enum TagSource {
        TAB,
        NAMETAG,
        CHAT
    }

    /** Resolves an online player's UUID by name via the tab list (no Mojang call). */
    public static UUID findOnlineUuid(String name) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return null;
        }
        for (var entry : client.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().name().equalsIgnoreCase(name)) {
                return entry.getProfile().id();
            }
        }
        return null;
    }

    /**
     * Non-blocking tier prepend for render paths (nametag/tab/chat). Returns original
     * when uncached (fires background fetch for next frame), like TierTagger.
     */
    public static Text appendTier(UUID uuid, Text original, TagSource source) {
        NovaTaggerConfig config = NovaTaggerConfig.get();
        if (!config.enabled) {
            return original;
        }
        if (source == TagSource.TAB && !config.showInTab) {
            return original;
        }
        if (source == TagSource.NAMETAG && !config.showAboveHead) {
            return original;
        }
        if (source == TagSource.CHAT) {
            return original;
        }
        if (tierCache == null || uuid == null) {
            return original;
        }
        Optional<NovaPlayerInfo> cached = tierCache.getCached(uuid);
        if (cached.isEmpty()) {
            tierCache.prefetch(uuid);
            return original;
        }
        NovaPlayerInfo info = cached.get();
        if (modeIndex >= 0) {
            String mode = MODES.get(modeIndex);
            String tier = info.displayTier(mode);
            if (!tier.equals("-") && (config.showRetired || !info.isRetired(mode))) {
                return NovaTierFormatter.compactTag(mode, tier, info.isRetired(mode), original);
            }
            return original;
        }
        List<NovaTierFormatter.ModeLine> lines = NovaTierFormatter.sortedLines(info);
        if (lines.isEmpty()) {
            return original;
        }
        NovaTierFormatter.ModeLine best = config.showRetired
                ? lines.get(0)
                : lines.stream().filter(l -> !l.retired()).findFirst().orElse(null);
        if (best == null) {
            return original;
        }
        return NovaTierFormatter.compactTag(best.mode(), best.tier(), best.retired(), original);
    }

    public static String currentModeName() {
        return modeIndex < 0 ? "Best" : MODES.get(modeIndex);
    }

    @Override
    public void onInitializeClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        tierCache = new NovaTierCache(httpClient);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                NovaTagCommand.register(dispatcher));

        cycleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.novatagger.cycle_mode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyBinding.Category.create(Identifier.of("novatagger", "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (cycleModeKey.wasPressed()) {
                modeIndex++;
                if (modeIndex >= MODES.size()) {
                    modeIndex = -1;
                }
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("Displayed gamemode: " + currentModeName())
                                    .styled(s -> s.withColor(0x8b5cf6)),
                            true
                    );
                }
            }
        });

        NovaTaggerMod.LOGGER.info("NovaTagger client initialized!");
    }
}
