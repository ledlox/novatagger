package com.novatagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.novatagger.client.NovaTaggerClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Above-head nametag source: PlayerEntity.getDisplayName feeds nametag rendering. */
@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text prependNovaTier(Text original) {
        // LAN leak guard: integrated server runs in the same JVM.
        // Only tag on the client thread so the server never bakes tags into broadcast names.
        if (!net.minecraft.client.MinecraftClient.getInstance().isOnThread()) {
            return original;
        }
        PlayerEntity self = (PlayerEntity) (Object) this;
        return NovaTaggerClient.appendTier(self.getUuid(), original, NovaTaggerClient.TagSource.NAMETAG);
    }
}
