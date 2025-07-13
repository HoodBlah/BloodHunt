package com.example.bloodhunt.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

public class BloodTrailParticle extends TextureSheetParticle {
    private final double startY;

    protected BloodTrailParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y + 0.5D, z);
        this.startY = y + 0.5D;
        
        // Dust particle settings
        this.quadSize = 0.15F;
        
        // Very minimal motion
        this.xd = xd * 0.01D;
        this.yd = yd * 0.01D;
        this.zd = zd * 0.01D;
        
        // Shorter lifetime
        this.lifetime = 10 + random.nextInt(5);
        
        // Set color to deep red
        this.rCol = 0.8F;
        this.gCol = 0.0F;
        this.bCol = 0.0F;
        this.alpha = 1.0F;
        
        // No gravity since we want them to stay at hip level
        this.gravity = 0;
        
        // No collision
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        
        // Keep at hip level
        if (Math.abs(this.y - this.startY) > 0.05) {
            this.yd += (this.startY - this.y) * 0.1;
        }
        
        // Fade out near end of lifetime
        if (this.age > this.lifetime - 3) {
            this.alpha = Math.max(0.0F, this.alpha - 0.2F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            BloodTrailParticle particle = new BloodTrailParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
} 