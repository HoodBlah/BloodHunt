package com.example.bloodhunt.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;

@OnlyIn(Dist.CLIENT)
public class BloodTrailParticle extends TextureSheetParticle {
    private final double targetX;
    private final double targetY;
    private final double targetZ;

    protected BloodTrailParticle(ClientLevel level, double x, double y, double z, 
                                double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z);
        
        this.targetX = x + xSpeed;
        this.targetY = y + ySpeed;
        this.targetZ = z + zSpeed;
        
        // Set particle properties
        this.lifetime = 2000; // Extended lifetime (100 seconds)
        this.gravity = 0.0F;
        this.hasPhysics = false;
        
        // Red color for blood
        this.rCol = 0.8F;
        this.gCol = 0.0F;
        this.bCol = 0.0F;
        this.alpha = 0.8F;
        
        // Small particle size
        this.quadSize = 0.1F;
        
        // Calculate velocity towards target
        double dx = targetX - x;
        double dy = targetY - y;
        double dz = targetZ - z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance > 0) {
            double speed = 0.2; // Adjust speed as needed
            this.xd = dx / distance * speed;
            this.yd = dy / distance * speed;
            this.zd = dz / distance * speed;
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
        
        // Check distance to player
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            double dx = minecraft.player.getX() - this.x;
            double dy = minecraft.player.getY() - this.y;
            double dz = minecraft.player.getZ() - this.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            
            // Remove particle if player is within 0.5 blocks
            if (distSq < 0.25) {
                this.remove();
                return;
            }
        }
        
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            
            // Fade out over time
            this.alpha = 0.8F * (1.0F - ((float) this.age / (float) this.lifetime));
        }
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
            BloodTrailParticle particle = new BloodTrailParticle(level, x, y, z, 
                                                                xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
} 