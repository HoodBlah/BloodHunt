package com.example.bloodhunt;

import com.example.bloodhunt.client.KeyBindings;
import com.example.bloodhunt.client.gui.EntitySelectorScreen;
import com.example.bloodhunt.pathfinding.PathFinder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import com.example.bloodhunt.particle.ModParticles;
import java.util.Random;
import net.minecraft.util.RandomSource;

@Mod.EventBusSubscriber(modid = Bloodhunt.MOD_ID, value = Dist.CLIENT)
public class BloodhuntManager {
    private static final int MAX_PATH_DISTANCE = 100;
    private static List<BlockPos> currentPath = null;
    private static LivingEntity currentTarget = null;
    private static float pathProgress = 0f;
    private static final float PATH_ANIMATION_SPEED = 0.05f;
    private static final float RIBBON_WIDTH = 0.15f; // Made thinner
    private static final float WAVE_AMPLITUDE = 0.1f; // Reduced wave
    private static final float WAVE_FREQUENCY = 2.0f;
    private static final float VERTICAL_OFFSET = 0.8f;
    private static final float PATH_START_OFFSET = 1.0f;
    private static final int PARTICLE_SPAWN_RATE = 2; // Spawn particles every N ticks
    private static int particleTimer = 0;
    private static int pathUpdateTimer = 0;
    private static final int PATH_UPDATE_INTERVAL = 6;

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen == null) {
            if (KeyBindings.OPEN_SELECTOR_KEY.consumeClick()) {
                minecraft.setScreen(new EntitySelectorScreen());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && currentPath != null) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || currentTarget == null) return;

            // Update path animation
            pathProgress = Math.min(1f, pathProgress + PATH_ANIMATION_SPEED);
            
            // Spawn particles along the path
            particleTimer++;
            if (particleTimer >= PARTICLE_SPAWN_RATE) {
                particleTimer = 0;
                spawnPathParticles(minecraft);
            }
            
            // Check if we need to update the path
            pathUpdateTimer++;
            if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
                pathUpdateTimer = 0;
                // Only update if target has moved significantly
                BlockPos targetPos = currentTarget.blockPosition();
                if (!currentPath.isEmpty() && !targetPos.equals(currentPath.get(currentPath.size() - 1))) {
                    BlockPos start = minecraft.player.blockPosition();
                    PathFinder pathFinder = new PathFinder(minecraft.level, start, targetPos, MAX_PATH_DISTANCE);
                    currentPath = pathFinder.findPath();
                    pathProgress = 1f; // Don't animate updates
                }
            }
            
            // Check if player is close to next point
            if (!currentPath.isEmpty()) {
                BlockPos nextPoint = currentPath.get(0);
                Vec3 playerPos = minecraft.player.position();
                double distSq = playerPos.distanceToSqr(
                    nextPoint.getX() + 0.5,
                    nextPoint.getY() + 0.5,
                    nextPoint.getZ() + 0.5
                );
                
                if (distSq < 0.25) { // Within 0.5 blocks
                    currentPath.remove(0);
                    
                    // If path is empty, stop tracking
                    if (currentPath.isEmpty()) {
                        stopTracking();
                    }
                }
            }
        }
    }

    private static void spawnPathParticles(Minecraft minecraft) {
        if (currentPath == null || currentPath.isEmpty()) return;
        
        int pointsToShow = Math.max(1, (int)(currentPath.size() * pathProgress));
        RandomSource random = minecraft.level.getRandom();
        
        for (int i = 0; i < pointsToShow - 1; i++) {
            BlockPos current = currentPath.get(i);
            BlockPos next = currentPath.get(i + 1);
            
            // Calculate a random position between current and next point
            double lerp = random.nextDouble();
            double x = current.getX() + 0.5 + (next.getX() - current.getX()) * lerp;
            double y = current.getY() + 0.5 + (next.getY() - current.getY()) * lerp + VERTICAL_OFFSET;
            double z = current.getZ() + 0.5 + (next.getZ() - current.getZ()) * lerp;
            
            // Add some random spread
            x += (random.nextDouble() - 0.5) * 0.2;
            y += (random.nextDouble() - 0.5) * 0.2;
            z += (random.nextDouble() - 0.5) * 0.2;
            
            // Calculate direction to next point for particle movement
            double dx = next.getX() - current.getX();
            double dy = next.getY() - current.getY();
            double dz = next.getZ() - current.getZ();
            
            minecraft.level.addParticle(
                ModParticles.PATH_PARTICLE.get(),
                x, y, z,  // Position
                dx, dy, dz // Movement direction
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (currentPath == null || currentPath.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 playerPos = minecraft.player.position();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Update wave animation
        // waveOffset += 0.1f; // This line is removed as particles handle wave effect
        // if (waveOffset > 2 * Math.PI) waveOffset -= 2 * Math.PI; // This line is removed as particles handle wave effect

        // Calculate visible path
        int pointsToShow = Math.max(1, (int)(currentPath.size() * pathProgress));
        int startIndex = Math.max(0, findClosestPointIndex(playerPos));

        // Get the buffer builder
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        
        // Begin building the triangles
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pose = poseStack.last().pose();

        // Draw the ribbon
        for (int i = startIndex; i < pointsToShow - 1; i++) {
            BlockPos current = currentPath.get(i);
            BlockPos next = currentPath.get(i + 1);
            float progress = (float)i / currentPath.size();

            // If this is the first segment, adjust start position to player's chest
            if (i == startIndex) {
                Vec3 playerLook = minecraft.player.getLookAngle();
                current = new BlockPos(
                    (int)(playerPos.x - playerLook.x * PATH_START_OFFSET),
                    (int)(playerPos.y + 0.8), // Chest height
                    (int)(playerPos.z - playerLook.z * PATH_START_OFFSET)
                );
            }

            // Calculate direction vector
            Vec3 dir = new Vec3(
                next.getX() - current.getX(),
                next.getY() - current.getY(),
                next.getZ() - current.getZ()
            ).normalize();

            // Calculate camera-relative up vector for billboarding
            Vec3 pathToCam = new Vec3(
                cameraPos.x - (current.getX() + 0.5),
                0, // Keep vertical component zero
                cameraPos.z - (current.getZ() + 0.5)
            ).normalize();

            // Calculate right vector using camera direction
            Vec3 right = dir.cross(pathToCam).normalize().scale(RIBBON_WIDTH);

            // Add wave effect to both position and width
            float wave = (float)(Math.sin(progress * WAVE_FREQUENCY * Math.PI) * WAVE_AMPLITUDE);
            float heightWave = (float)(Math.cos(progress * WAVE_FREQUENCY * Math.PI) * WAVE_AMPLITUDE);
            right = right.scale(1.0 + wave);

            // Calculate vertices with vertical offset and wave
            float x1 = (float)(current.getX() + 0.5 + right.x);
            float y1 = (float)(current.getY() + 0.5 + VERTICAL_OFFSET + heightWave);
            float z1 = (float)(current.getZ() + 0.5 + right.z);
            float x2 = (float)(current.getX() + 0.5 - right.x);
            float y2 = (float)(current.getY() + 0.5 + VERTICAL_OFFSET - heightWave);
            float z2 = (float)(current.getZ() + 0.5 - right.z);

            // Calculate alpha based on distance from player
            float alpha = 1.0f - (float)Math.min(1.0, playerPos.distanceTo(new Vec3(current.getX(), current.getY(), current.getZ())) / 20.0);
            alpha *= 0.8f;

            // Add vertices for the main ribbon
            bufferBuilder.vertex(pose, x1, y1, z1)
                .color(1.0f, 0.2f, 0.2f, alpha)
                .endVertex();
            bufferBuilder.vertex(pose, x2, y2, z2)
                .color(1.0f, 0.2f, 0.2f, alpha)
                .endVertex();

            // Calculate next vertices with vertical offset and wave
            Vec3 nextRight = dir.cross(pathToCam).normalize().scale(RIBBON_WIDTH * (1.0 + wave));
            float nextHeightWave = (float)(Math.cos((progress + 0.1f) * WAVE_FREQUENCY * Math.PI) * WAVE_AMPLITUDE);

            x1 = (float)(next.getX() + 0.5 + nextRight.x);
            y1 = (float)(next.getY() + 0.5 + VERTICAL_OFFSET + nextHeightWave);
            z1 = (float)(next.getZ() + 0.5 + nextRight.z);
            x2 = (float)(next.getX() + 0.5 - nextRight.x);
            y2 = (float)(next.getY() + 0.5 + VERTICAL_OFFSET - nextHeightWave);
            z2 = (float)(next.getZ() + 0.5 - nextRight.z);

            // Add vertices for the next segment
            bufferBuilder.vertex(pose, x1, y1, z1)
                .color(1.0f, 0.2f, 0.2f, alpha)
                .endVertex();
            bufferBuilder.vertex(pose, x2, y2, z2)
                .color(1.0f, 0.2f, 0.2f, alpha)
                .endVertex();
        }

        // Draw the triangles
        BufferUploader.drawWithShader(bufferBuilder.end());

        // Reset render state
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1.0f);
        
        poseStack.popPose();
    }

    private static int findClosestPointIndex(Vec3 playerPos) {
        int closestPointIndex = 0;
        double closestDistSq = Double.MAX_VALUE;
        
        for (int i = 0; i < currentPath.size(); i++) {
            BlockPos pos = currentPath.get(i);
            double dx = playerPos.x - (pos.getX() + 0.5);
            double dy = playerPos.y - (pos.getY() + 0.5);
            double dz = playerPos.z - (pos.getZ() + 0.5);
            double distSq = dx * dx + dy * dy + dz * dz;
            
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestPointIndex = i;
            }
        }
        
        return closestPointIndex;
    }

    public static void startTracking(LivingEntity target) {
        if (target == null) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        // Calculate path
        BlockPos start = minecraft.player.blockPosition();
        BlockPos targetPos = target.blockPosition();
        
        PathFinder pathFinder = new PathFinder(minecraft.level, start, targetPos, MAX_PATH_DISTANCE);
        currentPath = pathFinder.findPath();
        currentTarget = target;
        pathProgress = 0f;
        pathUpdateTimer = 0;
    }

    public static void stopTracking() {
        currentPath = null;
        currentTarget = null;
        pathProgress = 0f;
        pathUpdateTimer = 0;
    }
} 