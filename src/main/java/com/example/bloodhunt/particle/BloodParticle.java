package com.example.bloodhunt.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BloodParticle extends TextureSheetParticle {
    private final double startY;
    private static final float SHAKE_AMOUNT = 0.005F;
    private static final float MAX_RISE = 0.03F;

    protected BloodParticle(ClientLevel level, double x, double y, double z, double targetX, double targetY, double targetZ) {
        super(level, x, y, z);
        this.startY = y;
        
        // Set particle properties
        this.setSize(0.03F, 0.03F); // Smaller particles
        this.lifetime = 20 + this.random.nextInt(10); // Shorter lifetime
        
        // Minimal initial motion
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        
        // Deep red color with glow effect
        float colorVar = (this.random.nextFloat() * 0.1F) - 0.05F;
        this.setColor(
            0.8F + colorVar, // Brighter red for glow
            0.2F, // More green for glow
            0.2F  // More blue for glow
        );
        this.setAlpha(0.6F);
        
        // No rotation
        this.roll = 0;
        this.oRoll = 0;

        // Start slightly transparent
        this.alpha = 0.3F;

        // Make particle fullbright
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Subtle random movement
        this.xd = (this.random.nextFloat() - 0.5F) * SHAKE_AMOUNT;
        this.zd = (this.random.nextFloat() - 0.5F) * SHAKE_AMOUNT;
        
        // Very slight rise with a maximum height
        if (this.y < this.startY + MAX_RISE) {
            this.yd = 0.0005F;
        } else {
            this.yd = -0.0005F;
        }

        // Move the particle
        this.move(this.xd, this.yd, this.zd);
        
        // Fade in and out
        if (this.age < this.lifetime * 0.2) {
            // Fade in
            this.alpha = Math.min(0.6F, this.alpha + 0.08F);
        } else if (this.age > this.lifetime * 0.7) { // Start fading earlier
            // Fade out
            this.alpha = Math.max(0.0F, this.alpha - 0.08F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // Maximum light value (15 << 20 | 15 << 4) for fullbright effect
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            BloodParticle particle = new BloodParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
} 