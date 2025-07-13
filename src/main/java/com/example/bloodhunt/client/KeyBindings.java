package com.example.bloodhunt.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.example.bloodhunt.Bloodhunt;

@Mod.EventBusSubscriber(modid = Bloodhunt.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    public static final KeyMapping OPEN_SELECTOR_KEY = new KeyMapping(
        "key.bloodhunt.open_selector",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_B,
        "key.categories.bloodhunt"
    );

    @SubscribeEvent
    public static void init(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SELECTOR_KEY);
    }
} 