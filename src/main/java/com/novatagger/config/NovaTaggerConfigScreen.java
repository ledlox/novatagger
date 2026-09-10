package com.novatagger.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Vanilla-widgets config screen (no Cloth Config needed). Opened from Mod Menu. */
public class NovaTaggerConfigScreen extends Screen {
    private final Screen parent;
    private final NovaTaggerConfig config = NovaTaggerConfig.get();

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
        int y = 60;
        addDrawableChild(toggleButton(y, "Show in tab list", () -> config.showInTab, v -> config.showInTab = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show above heads", () -> config.showAboveHead, v -> config.showAboveHead = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show in chat", () -> config.showInChat, v -> config.showInChat = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show retired tiers", () -> config.showRetired, v -> config.showRetired = v));
        y += 24;
        addDrawableChild(toggleButton(y, "Show peak in /novatag", () -> config.showPeak, v -> config.showPeak = v));
        y += 32;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 100, y, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void close() {
        config.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
