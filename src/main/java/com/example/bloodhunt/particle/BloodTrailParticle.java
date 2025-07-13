package com.example.bloodhunt.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

public class BloodTrailParticle extends TextureSheetParticle {
    private final double startY;
    private static final float GLOW_STRENGTH = 1.0F;

    protected BloodTrailParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y + 0.5D, z);
        this.startY = y + 0.5D;
        
        // Slightly larger particles for better visibility
        this.quadSize = 0.2F;
        
        // Very minimal motion
        this.xd = xd * 0.01D;
        this.yd = yd * 0.01D;
        this.zd = zd * 0.01D;
        
        // Shorter lifetime
        this.lifetime = 10 + random.nextInt(5);
        
        // Set color to deep red
        this.rCol = 0.9F;
        this.gCol = 0.1F;
        this.bCol = 0.1F;
        this.alpha = 0.8F;
        
        // Enable gravity and collision
        this.gravity = 0.01F;
        this.hasPhysics = true;
        
        // Make particle glow
        this.setParticleSpeed(0, 0, 0);
    }

    @Override
    public void tick() {
        super.tick();
        
        // Fade out over time
        float lifeRatio = (float) this.age / (float) this.lifetime;
        this.alpha = 0.8F * (1.0F - lifeRatio);
        
        // Maintain minimum brightness for glow effect
        this.rCol = Math.max(0.9F, this.rCol);
        this.gCol = Math.max(0.1F, this.gCol);
        this.bCol = Math.max(0.1F, this.bCol);
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Use PARTICLE_SHEET_TRANSLUCENT for glowing effect
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // Make particles emit light
    @Override
    public int getLightColor(float partialTick) {
        // This creates a bright red glow effect (15 is max light level)
        int skyLight = 15;
        int blockLight = 15;
        return (skyLight << 20) | (blockLight << 4);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            BloodTrailParticle particle = new BloodTrailParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
} 