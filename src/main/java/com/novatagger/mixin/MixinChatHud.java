package com.novatagger.mixin;

import com.novatagger.client.NovaTaggerClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

/**
 * Chat: turns {@code <ledlox> hello} into {@code <LT3 | ledlox> hello}.
 * Hooks vanilla player chat ({@code chat.type.text}), which carries the sender's
 * display Text (with click/hover styles) as its first arg — we tag that arg in
 * place so styles survive. Sender UUID resolves via the online tab list.
 */
@Mixin(ChatHud.class)
public class MixinChatHud {
    // 1-arg overload: system / client feedback messages.
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
    private Text tagSystemSender(Text message) {
        return tagChatMessage(message);
    }

    // 3-arg overload: real player chat from the server. This is the one that matters.
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), argsOnly = true)
    private Text tagPlayerSender(Text message) {
        return tagChatMessage(message);
    }

    private Text tagChatMessage(Text message) {
        if (!(message.getContent() instanceof TranslatableTextContent translatable)) {
            return message;
        }
        if (!translatable.getKey().equals("chat.type.text")) {
            return message;
        }
        Object[] args = translatable.getArgs();
        if (args.length < 1 || !(args[0] instanceof Text sender)) {
            return message;
        }
        UUID uuid = NovaTaggerClient.findOnlineUuid(sender.getString());
        if (uuid == null) {
            return message;
        }
        Text tagged = NovaTaggerClient.appendTier(uuid, sender, NovaTaggerClient.TagSource.CHAT);
        if (tagged == sender) {
            return message;
        }
        Object[] newArgs = args.clone();
        newArgs[0] = tagged;
        MutableText rebuilt = Text.translatable(translatable.getKey(), newArgs);
        rebuilt.setStyle(message.getStyle());
        rebuilt.getSiblings().addAll(message.getSiblings());
        return rebuilt;
    }
}
