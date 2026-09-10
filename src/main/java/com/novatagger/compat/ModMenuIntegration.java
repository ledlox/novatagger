package com.novatagger.compat;

import com.novatagger.config.NovaTaggerConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Hooks NovaTagger into Mod Menu's config button. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return NovaTaggerConfigScreen::new;
    }
}
