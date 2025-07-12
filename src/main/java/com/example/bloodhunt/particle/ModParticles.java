package com.example.bloodhunt.particle;

import com.example.bloodhunt.Bloodhunt;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = 
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Bloodhunt.MOD_ID);

    public static final RegistryObject<SimpleParticleType> PATH_PARTICLE = 
        PARTICLE_TYPES.register("path_particle", 
            () -> new SimpleParticleType(true));
} 