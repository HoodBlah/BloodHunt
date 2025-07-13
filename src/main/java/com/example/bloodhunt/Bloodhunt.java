package com.example.bloodhunt;

import com.example.bloodhunt.particle.ModParticles;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Bloodhunt.MOD_ID)
public class Bloodhunt {
    public static final String MOD_ID = "bloodhunt";

    public Bloodhunt() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register particles
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }
} 