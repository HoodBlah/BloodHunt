package com.example.bloodhunt.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PathParticle extends TextureSheetParticle {
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final float moveSpeed;

    protected PathParticle(ClientLevel level, double x, double y, double z, 
                          double targetX, double targetY, double targetZ) {
        super(level, x, y, z);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        
        // Particle properties
        this.lifetime = 20; // 1 second
        this.gravity = 0;
        this.moveSpeed = 0.1f;
        
        // Size and color
        this.quadSize = 0.15F;
        this.setColor(1.0F, 0.2F, 0.2F); // Red color
        this.setAlpha(0.8F);
        
        // Calculate movement
        double dx = targetX - x;
        double dy = targetY - y;
        double dz = targetZ - z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance > 0) {
            this.xd = dx / distance * moveSpeed;
            this.yd = dy / distance * moveSpeed;
            this.zd = dz / distance * moveSpeed;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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

        // Move towards target
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        
        // Fade out over time
        float lifeRatio = (float) this.age / (float) this.lifetime;
        this.alpha = 0.8F * (1.0F - lifeRatio);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                     double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed) {
            PathParticle particle = new PathParticle(level, x, y, z, 
                                                   x + xSpeed, y + ySpeed, z + zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
} 