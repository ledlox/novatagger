package com.novatagger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.novatagger.api.NovaPlayerInfo;
import com.novatagger.api.NovaTierCache;
import com.novatagger.api.NovaTierFormatter;
import com.novatagger.client.NovaTaggerClient;
import com.novatagger.config.NovaTaggerConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.UUID;

/** /novatag <player> — text-only rankings output, no [mode] arg. */
public final class NovaTagCommand {
    private NovaTagCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("novatag")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "player");
                            lookupAndShow(ctx.getSource(), name);
                            return 1;
                        }))
                .executes(ctx -> {
                    // /novatag with no args = own tiers
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        lookupAndShow(ctx.getSource(), client.player.getName().getString());
                    } else {
                        ctx.getSource().sendError(Text.literal("Not in game."));
                    }
                    return 1;
                }));
    }

    private static void lookupAndShow(FabricClientCommandSource source, String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        source.sendFeedback(Text.literal("Fetching NovaTiers for " + name + "...").styled(s -> s.withColor(0x888888)));

        // 1. Try online player list first (no Mojang call needed).
        UUID onlineUuid = NovaTaggerClient.findOnlineUuid(name);
        if (onlineUuid != null) {
            fetchByUuid(source, client, onlineUuid);
            return;
        }

        // 2. Offline name -> Mojang UUID -> NovaTiers.
        NovaPlayerInfo.lookupMojangUuid(NovaTaggerClient.getHttpClient(), name).thenAccept(uuidOpt -> {
            if (uuidOpt.isEmpty()) {
                sendFeedback(client, source, Text.literal("Player not found: " + name).styled(s -> s.withColor(0xFF5555)));
                return;
            }
            fetchByUuid(source, client, uuidOpt.get());
        });
    }

    private static void fetchByUuid(FabricClientCommandSource source, MinecraftClient client, UUID uuid) {
        NovaTierCache cache = NovaTaggerClient.getTierCache();
        cache.getAsync(uuid).thenAccept(infoOpt -> {
            if (infoOpt.isEmpty()) {
                sendFeedback(client, source, Text.literal("No NovaTiers data for " + uuid).styled(s -> s.withColor(0xFF5555)));
                return;
            }
            NovaPlayerInfo info = infoOpt.get();
            for (Text line : NovaTierFormatter.rankingsMessage(info, NovaTaggerConfig.get().showPeak)) {
                sendFeedback(client, source, line);
            }
        });
    }

    private static void sendFeedback(MinecraftClient client, FabricClientCommandSource source, Text text) {
        if (client.isOnThread()) {
            source.sendFeedback(text);
        } else {
            client.execute(() -> source.sendFeedback(text));
        }
    }
}
