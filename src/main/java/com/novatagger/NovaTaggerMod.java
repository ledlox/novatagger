package com.novatagger;

import com.novatagger.config.NovaTaggerConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaTaggerMod implements ModInitializer {
    public static final String MOD_ID = "novatagger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        NovaTaggerConfig.get();
        LOGGER.info("NovaTagger initialized for Minecraft 1.21.11!");
    }
}
