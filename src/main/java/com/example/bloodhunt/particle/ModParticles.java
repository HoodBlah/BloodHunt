package com.example.bloodhunt.particle;

import com.example.bloodhunt.Bloodhunt;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Bloodhunt.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = 
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Bloodhunt.MOD_ID);

    public static final RegistryObject<SimpleParticleType> BLOOD_PARTICLE = 
        PARTICLE_TYPES.register("blood_particle", 
            () -> new SimpleParticleType(true));

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BLOOD_PARTICLE.get(), BloodParticle.Provider::new);
    }
} 