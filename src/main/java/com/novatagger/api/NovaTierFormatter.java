package com.novatagger.api;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Text-only formatting with TierTagger-default colors.
 * TierTagger defaults: HT1 0xebaa3a, LT1 0xd5b355, HT2 0xa4d3e7, LT2 0xa0a7b2,
 * HT3 0xf89f5a, LT3 0xa67b42, HT4 0x81749a, LT4 0x655b79, HT5 0x8f82a8, LT5 0x655b79,
 * retired 0xa2d6fd, fallback 0xD3D3D3.
 */
public final class NovaTierFormatter {
    public static final int RETIRED_COLOR = 0xa2d6fd;
    public static final int FALLBACK_COLOR = 0xD3D3D3;

    private static final Map<String, Integer> TIER_COLORS = Map.of(
            "HT1", 0xebaa3a,
            "LT1", 0xd5b355,
            "HT2", 0xa4d3e7,
            "LT2", 0xa0a7b2,
            "HT3", 0xf89f5a,
            "LT3", 0xa67b42,
            "HT4", 0x81749a,
            "LT4", 0x655b79,
            "HT5", 0x8f82a8,
            "LT5", 0x655b79
    );

    private NovaTierFormatter() {}

    public static int tierColor(String tier, boolean retired) {
        if (retired) {
            return RETIRED_COLOR;
        }
        return TIER_COLORS.getOrDefault(tier.toUpperCase(), FALLBACK_COLOR);
    }

    /** Display string with R prefix when retired (e.g. RLT3), like TierTagger. */
    public static String displayString(String tier, boolean retired) {
        String upper = tier.toUpperCase();
        return retired ? "R" + upper : upper;
    }

    /** Lower is better: HT1=0, LT1=1, HT2=2 ... unranked last. */
    public static int rankValue(String tier) {
        return switch (tier.toUpperCase()) {
            case "HT1" -> 0;
            case "LT1" -> 1;
            case "HT2" -> 2;
            case "LT2" -> 3;
            case "HT3" -> 4;
            case "LT3" -> 5;
            case "HT4" -> 6;
            case "LT4" -> 7;
            case "HT5" -> 8;
            case "LT5" -> 9;
            default -> 100;
        };
    }

    public record ModeLine(String mode, String tier, String peak, boolean retired) {}

    /** All modes sorted best-first, retired last (TierTagger getSortedTiers style). */
    public static List<ModeLine> sortedLines(NovaPlayerInfo info) {
        List<ModeLine> lines = new ArrayList<>();
        for (String mode : info.tiers().keySet()) {
            String tier = info.displayTier(mode);
            if (tier.equals("-")) {
                continue;
            }
            String peak = info.peakTiers().getOrDefault(mode, "-");
            lines.add(new ModeLine(mode, tier.toUpperCase(), peak.toUpperCase(), info.isRetired(mode)));
        }
        lines.sort(Comparator
                .comparing((ModeLine l) -> l.retired())
                .thenComparingInt(l -> rankValue(l.tier()))
                .thenComparing(ModeLine::mode));
        return lines;
    }

    public static MutableText tierText(String tier, boolean retired) {
        String display = displayString(tier, retired);
        int color = tierColor(tier, retired);
        return Text.literal(display).styled(s -> s.withColor(color));
    }

    /** Full /novatag output: header + one line per mode. */
    public static List<Text> rankingsMessage(NovaPlayerInfo info, boolean showPeak) {
        List<Text> out = new ArrayList<>();
        out.add(Text.literal("=== Rankings for " + info.minecraftUsername() + " ===").styled(s -> s.withColor(0x8b5cf6).withBold(true)));
        List<ModeLine> lines = sortedLines(info);
        if (lines.isEmpty()) {
            out.add(Text.literal("Unranked").styled(s -> s.withColor(0x999999)));
            return out;
        }
        for (ModeLine line : lines) {
            MutableText msg = Text.literal(line.mode() + ": ").styled(s -> s.withColor(0xFFFFFF));
            msg.append(tierText(line.tier(), line.retired()));
            if (showPeak && !line.peak().equals("-") && !line.peak().equalsIgnoreCase(line.tier())) {
                msg.append(Text.literal(" (peak: " + line.peak() + ")").styled(s -> s.withColor(0x888888)));
            }
            out.add(msg);
        }
        return out;
    }

    /** Compact single-mode tag for nametag/tab/chat: "HT3 | Name". Caller picks mode. */
    public static MutableText compactTag(String tier, boolean retired, Text name) {
        MutableText tag = tierText(tier, retired);
        tag.append(Text.literal(" | ").styled(s -> s.withColor(0x888888)));
        return tag.append(name.copy());
    }
}
