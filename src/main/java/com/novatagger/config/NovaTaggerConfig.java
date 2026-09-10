package com.novatagger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.novatagger.NovaTaggerMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config at config/novatagger.json. No extra dependencies. */
public class NovaTaggerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("novatagger.json");

    private static NovaTaggerConfig instance;

    public boolean showInTab = true;
    public boolean showAboveHead = true;
    public boolean showInChat = true;
    public boolean showRetired = true;
    public boolean showPeak = true;

    public static NovaTaggerConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static NovaTaggerConfig load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                NovaTaggerConfig loaded = GSON.fromJson(reader, NovaTaggerConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                NovaTaggerMod.LOGGER.warn("Failed to read NovaTagger config, using defaults: {}", e.toString());
            }
        }
        NovaTaggerConfig fresh = new NovaTaggerConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            NovaTaggerMod.LOGGER.warn("Failed to save NovaTagger config: {}", e.toString());
        }
    }
}
