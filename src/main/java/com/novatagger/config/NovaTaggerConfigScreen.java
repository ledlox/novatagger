package com.novatagger.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Vanilla-widgets config screen (no Cloth Config needed). Opened from Mod Menu. */
public class NovaTaggerConfigScreen extends Screen {
    private enum Tab {
        SETTINGS,
        COLORS
    }

    private static final String[] COLOR_KEYS = {
            "HT1", "LT1", "HT2", "LT2", "HT3", "LT3",
            "HT4", "LT4", "HT5", "LT5", "Retired"
    };

    private final Screen parent;
    private final NovaTaggerConfig config = NovaTaggerConfig.get();
    private Tab activeTab = Tab.SETTINGS;
    private final int[] colorRowY = new int[COLOR_KEYS.length];

    public NovaTaggerConfigScreen(Screen parent) {
        super(Text.literal("NovaTagger Settings"));
        this.parent = parent;
    }

    private Text toggleLabel(String name, boolean value) {
        return Text.literal(name + ": " + (value ? "ON" : "OFF"))
                .styled(s -> s.withColor(value ? 0x55FF55 : 0xFF5555));
    }

    private ButtonWidget toggleButton(int y, String name, java.util.function.Supplier<Boolean> getter,
            java.util.function.Consumer<Boolean> setter) {
        return ButtonWidget.builder(toggleLabel(name, getter.get()), button -> {
                    boolean next = !getter.get();
                    setter.accept(next);
                    button.setMessage(toggleLabel(name, next));
                })
                .dimensions(width / 2 - 100, y, 200, 20)
                .build();
    }

    @Override
    protected void init() {
        clearChildren();
        int cx = width / 2;

        ButtonWidget settingsTab = ButtonWidget.builder(Text.literal("Settings"), b -> {
                    activeTab = Tab.SETTINGS;
                    clearChildren();
                    init();
                })
                .dimensions(cx - 102, 32, 100, 20)
                .build();
        ButtonWidget colorsTab = ButtonWidget.builder(Text.literal("Tier Colors"), b -> {
                    activeTab = Tab.COLORS;
                    clearChildren();
                    init();
                })
                .dimensions(cx + 2, 32, 100, 20)
                .build();
        settingsTab.active = activeTab != Tab.SETTINGS;
        colorsTab.active = activeTab != Tab.COLORS;
        addDrawableChild(settingsTab);
        addDrawableChild(colorsTab);

        if (activeTab == Tab.SETTINGS) {
            initSettings();
        } else {
            initColors();
        }
    }

    private void initSettings() {
        int y = 60;
        addDrawableChild(toggleButton(y, "Mod enabled", () -> config.enabled, v -> config.enabled = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show in tab list", () -> config.showInTab, v -> config.showInTab = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show above heads", () -> config.showAboveHead, v -> config.showAboveHead = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show in chat", () -> config.showInChat, v -> config.showInChat = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show gamemode icons", () -> config.showIcons, v -> config.showIcons = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show retired tiers", () -> config.showRetired, v -> config.showRetired = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show peak in /novatag", () -> config.showPeak, v -> config.showPeak = v));
        y += 32;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 100, y, 200, 20)
                .build());
    }

    private void initColors() {
        int cx = width / 2;
        int y = 58;
        for (int i = 0; i < COLOR_KEYS.length; i++) {
            String key = COLOR_KEYS[i];
            colorRowY[i] = y;
            TextFieldWidget field = new TextFieldWidget(textRenderer, cx - 40, y - 1, 80, 18,
                    Text.literal(key + " color"));
            field.setMaxLength(7);
            field.setText(NovaTaggerConfig.toHex(config.getColor(key)));
            field.setChangedListener(text -> {
                if (NovaTaggerConfig.isValidHex(text)) {
                    config.setColor(key, NovaTaggerConfig.parseHex(text, config.getColor(key)));
                    field.setEditableColor(0xFFFFFF);
                } else {
                    field.setEditableColor(0xFF5555);
                }
            });
            addDrawableChild(field);
            y += 22;
        }
        y += 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset defaults"), button -> {
                    config.resetColors();
                    clearChildren();
                    init();
                })
                .dimensions(cx - 102, y, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(cx + 2, y, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
        if (activeTab == Tab.COLORS) {
            int cx = width / 2;
            for (int i = 0; i < COLOR_KEYS.length; i++) {
                String key = COLOR_KEYS[i];
                int rowY = colorRowY[i];
                int color = config.getColor(key) & 0xFFFFFF;
                context.drawTextWithShadow(textRenderer, key, cx - 150, rowY + 4, 0xFFFFFF);
                // preview swatch with dark border
                context.fill(cx + 50, rowY - 1, cx + 70, rowY + 17, 0xFF000000);
                context.fill(cx + 51, rowY, cx + 69, rowY + 16, 0xFF000000 | color);
                context.drawTextWithShadow(textRenderer, key, cx + 76, rowY + 4, 0xFF000000 | color);
            }
        }
    }

    @Override
    public void close() {
        config.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
