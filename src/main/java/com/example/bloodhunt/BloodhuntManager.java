package com.example.bloodhunt;

import com.example.bloodhunt.client.KeyBindings;
import com.example.bloodhunt.client.gui.EntitySelectorScreen;
import com.example.bloodhunt.pathfinding.PathFinder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
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

import java.util.List;

@Mod.EventBusSubscriber(modid = Bloodhunt.MOD_ID, value = Dist.CLIENT)
public class BloodhuntManager {
    private static final int MAX_PATH_DISTANCE = 100;
    private static List<BlockPos> currentPath = null;
    private static LivingEntity currentTarget = null;
    private static float pathProgress = 0f;
    private static final float PATH_ANIMATION_SPEED = 0.05f; // Adjust for faster/slower animation

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
            // Update path animation
            pathProgress = Math.min(1f, pathProgress + PATH_ANIMATION_SPEED);
            
            // Check if player is close to next point
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && !currentPath.isEmpty()) {
                BlockPos nextPoint = currentPath.get(0);
                Vec3 playerPos = minecraft.player.position();
                double distSq = playerPos.distanceToSqr(
                    nextPoint.getX() + 0.5,
                    nextPoint.getY() + 0.5,
                    nextPoint.getZ() + 0.5
                );
                
                if (distSq < 0.25) { // Within 0.5 blocks
                    currentPath.remove(0);
                    pathProgress = 0f;
                    
                    // If path is empty, stop tracking
                    if (currentPath.isEmpty()) {
                        stopTracking();
                    }
                }
            }
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
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Draw the path
        VertexConsumer builder = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        Matrix4f pose = poseStack.last().pose();

        // Calculate how many points to show based on progress
        int pointsToShow = Math.max(1, (int)(currentPath.size() * pathProgress));
        
        for (int i = 0; i < pointsToShow - 1; i++) {
            BlockPos current = currentPath.get(i);
            BlockPos next = currentPath.get(i + 1);
            
            // Draw line segment
            builder.vertex(pose, current.getX() + 0.5f, current.getY() + 0.5f, current.getZ() + 0.5f)
                .color(0.8f, 0f, 0f, 0.8f)
                .normal(0, 1, 0)
                .endVertex();
            builder.vertex(pose, next.getX() + 0.5f, next.getY() + 0.5f, next.getZ() + 0.5f)
                .color(0.8f, 0f, 0f, 0.8f)
                .normal(0, 1, 0)
                .endVertex();
        }

        // Finish rendering
        minecraft.renderBuffers().bufferSource().endBatch();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        poseStack.popPose();
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
    }

    public static void stopTracking() {
        currentPath = null;
        currentTarget = null;
        pathProgress = 0f;
    }
} 