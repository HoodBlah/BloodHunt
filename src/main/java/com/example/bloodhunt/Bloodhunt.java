package com.example.bloodhunt;

import com.example.bloodhunt.client.KeyBindings;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Bloodhunt.MOD_ID)
public class Bloodhunt {
    public static final String MOD_ID = "bloodhunt";

    public Bloodhunt() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register keybindings
        modEventBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(KeyBindings.OPEN_SELECTOR_KEY);
        });
        
        // Register client setup
        modEventBus.addListener(this::clientSetup);
        
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void clientSetup(final FMLClientSetupEvent event) {
        // Client-side setup code
        MinecraftForge.EVENT_BUS.register(BloodhuntManager.class);
    }
} 