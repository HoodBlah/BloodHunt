package com.example.bloodhunt;

import com.example.bloodhunt.client.KeyBindings;
import com.example.bloodhunt.particle.ModParticles;
import com.example.bloodhunt.particle.PathParticle;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
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
        
        // Register particles
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        
        // Register keybindings
        modEventBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(KeyBindings.OPEN_SELECTOR_KEY);
        });
        
        // Register particle providers
        modEventBus.addListener((RegisterParticleProvidersEvent event) -> {
            Minecraft.getInstance().particleEngine.register(
                ModParticles.PATH_PARTICLE.get(),
                PathParticle.Provider::new
            );
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