package com.novatagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.novatagger.client.NovaTaggerClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Tablist: prepends best NovaTiers tier, TierTagger-style. */
@Mixin(PlayerListHud.class)
public class MixinPlayerListHud {
    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text prependNovaTier(Text original, PlayerListEntry entry) {
        return NovaTaggerClient.appendTier(entry.getProfile().id(), original, NovaTaggerClient.TagSource.TAB);
    }
}
