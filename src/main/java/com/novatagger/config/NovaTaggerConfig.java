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

    public boolean enabled = true;
    public boolean showInTab = true;
    public boolean showAboveHead = true;
    public boolean showInChat = true;
    public boolean showRetired = true;
    public boolean showPeak = true;
    public boolean showIcons = true;

    // Tier colors (editable in the "Tier Colors" tab). Defaults match TierTagger.
    public int colorHT1 = 0xE8BA3A;
    public int colorLT1 = 0xD5B355;
    public int colorHT2 = 0xC4D3E7;
    public int colorLT2 = 0xA9A7B2;
    public int colorHT3 = 0xF89F5A;
    public int colorLT3 = 0xC67B42;
    public int colorHT4 = 0x81749A;
    public int colorLT4 = 0x555579;
    public int colorHT5 = 0x9F82A8;
    public int colorLT5 = 0x555579;
    public int colorRetired = 0xA2D6FF;

    /** Tier key ("HT1", "LT1", ... "Retired") -> default color. */
    public static int defaultColor(String key) {
        return switch (key) {
            case "HT1" -> 0xE8BA3A;
            case "LT1" -> 0xD5B355;
            case "HT2" -> 0xC4D3E7;
            case "LT2" -> 0xA9A7B2;
            case "HT3" -> 0xF89F5A;
            case "LT3" -> 0xC67B42;
            case "HT4" -> 0x81749A;
            case "LT4" -> 0x555579;
            case "HT5" -> 0x9F82A8;
            case "LT5" -> 0x555579;
            case "Retired" -> 0xA2D6FF;
            default -> 0xD3D3D3;
        };
    }

    public void resetColors() {
        colorHT1 = defaultColor("HT1");
        colorLT1 = defaultColor("LT1");
        colorHT2 = defaultColor("HT2");
        colorLT2 = defaultColor("LT2");
        colorHT3 = defaultColor("HT3");
        colorLT3 = defaultColor("LT3");
        colorHT4 = defaultColor("HT4");
        colorLT4 = defaultColor("LT4");
        colorHT5 = defaultColor("HT5");
        colorLT5 = defaultColor("LT5");
        colorRetired = defaultColor("Retired");
    }

    public int getColor(String key) {
        return switch (key) {
            case "HT1" -> colorHT1;
            case "LT1" -> colorLT1;
            case "HT2" -> colorHT2;
            case "LT2" -> colorLT2;
            case "HT3" -> colorHT3;
            case "LT3" -> colorLT3;
            case "HT4" -> colorHT4;
            case "LT4" -> colorLT4;
            case "HT5" -> colorHT5;
            case "LT5" -> colorLT5;
            case "Retired" -> colorRetired;
            default -> 0xD3D3D3;
        };
    }

    public void setColor(String key, int value) {
        switch (key) {
            case "HT1" -> colorHT1 = value;
            case "LT1" -> colorLT1 = value;
            case "HT2" -> colorHT2 = value;
            case "LT2" -> colorLT2 = value;
            case "HT3" -> colorHT3 = value;
            case "LT3" -> colorLT3 = value;
            case "HT4" -> colorHT4 = value;
            case "LT4" -> colorLT4 = value;
            case "HT5" -> colorHT5 = value;
            case "LT5" -> colorLT5 = value;
            case "Retired" -> colorRetired = value;
        }
    }

    /** TierTagger-style lookup: retired flag wins, else per-tier color, else gray fallback. */
    public int getTierColor(String tier, boolean retired) {
        if (retired) {
            return colorRetired;
        }
        return getColor(tier.toUpperCase());
    }

    /** Accepts "#E8BA3A", "E8BA3A", returns fallback when invalid. */
    public static int parseHex(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        String hex = s.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean isValidHex(String s) {
        if (s == null) {
            return false;
        }
        String hex = s.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            char c = hex.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public static String toHex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

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
                    loaded.sanitize();
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

    /** Old configs lack color fields (GSON leaves 0) — fill in defaults so tiers never go black. */
    private void sanitize() {
        if (colorHT1 == 0) colorHT1 = defaultColor("HT1");
        if (colorLT1 == 0) colorLT1 = defaultColor("LT1");
        if (colorHT2 == 0) colorHT2 = defaultColor("HT2");
        if (colorLT2 == 0) colorLT2 = defaultColor("LT2");
        if (colorHT3 == 0) colorHT3 = defaultColor("HT3");
        if (colorLT3 == 0) colorLT3 = defaultColor("LT3");
        if (colorHT4 == 0) colorHT4 = defaultColor("HT4");
        if (colorLT4 == 0) colorLT4 = defaultColor("LT4");
        if (colorHT5 == 0) colorHT5 = defaultColor("HT5");
        if (colorLT5 == 0) colorLT5 = defaultColor("LT5");
        if (colorRetired == 0) colorRetired = defaultColor("Retired");
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
