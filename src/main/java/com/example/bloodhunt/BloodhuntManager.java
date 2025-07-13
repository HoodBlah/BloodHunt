package com.example.bloodhunt;

import com.example.bloodhunt.client.KeyBindings;
import com.example.bloodhunt.client.gui.EntitySelectorScreen;
import com.example.bloodhunt.particle.ModParticles;
import com.example.bloodhunt.pathfinding.PathFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Bloodhunt.MOD_ID, value = Dist.CLIENT)
public class BloodhuntManager {
    private static final int MAX_PATH_DISTANCE = 100;
    private static List<BlockPos> currentPath = null;
    private static LivingEntity currentTarget = null;
    private static int pathUpdateTimer = 0;
    private static final int PATH_UPDATE_INTERVAL = 20; // Increased from 6 to 20 ticks (1 second)
    private static final Random random = new Random();
    private static final PathManager pathManager = new PathManager();
    private static BlockPos lastPlayerPos = null;
    private static BlockPos lastTargetPos = null;
    private static final double MIN_MOVEMENT_THRESHOLD = 1.0; // Minimum blocks moved before updating

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
            if (minecraft.player == null || currentTarget == null || minecraft.level == null) return;

            // Spawn particles for the current path
            pathManager.spawnParticles(minecraft.player, minecraft.level);
            
            pathUpdateTimer++;
            if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
                pathUpdateTimer = 0;
                
                BlockPos playerPos = minecraft.player.blockPosition();
                BlockPos targetPos = currentTarget.blockPosition();
                
                // Check if either player or target has moved significantly
                boolean needsUpdate = false;
                if (lastPlayerPos == null || lastTargetPos == null) {
                    needsUpdate = true;
                } else {
                    double playerMovement = Math.sqrt(lastPlayerPos.distSqr(playerPos));
                    double targetMovement = Math.sqrt(lastTargetPos.distSqr(targetPos));
                    needsUpdate = playerMovement > MIN_MOVEMENT_THRESHOLD || targetMovement > MIN_MOVEMENT_THRESHOLD;
                }
                
                // Only update path if needed
                if (needsUpdate) {
                    // Update last positions
                    lastPlayerPos = playerPos;
                    lastTargetPos = targetPos;
                    
                    // Recalculate path
                    PathFinder pathFinder = new PathFinder(minecraft.level, playerPos, targetPos, MAX_PATH_DISTANCE);
                    currentPath = pathFinder.findPath();
                    pathManager.updateTargetPath(minecraft.player.getUUID(), currentPath);
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
                    pathManager.updateTargetPath(minecraft.player.getUUID(), currentPath);
                    
                    // If path is empty, stop tracking
                    if (currentPath.isEmpty()) {
                        stopTracking();
                    }
                }
            }
        }
    }

    public static void startTracking(LivingEntity target) {
        if (target == null) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        // Calculate initial path
        BlockPos start = minecraft.player.blockPosition();
        BlockPos targetPos = target.blockPosition();
        
        PathFinder pathFinder = new PathFinder(minecraft.level, start, targetPos, MAX_PATH_DISTANCE);
        currentPath = pathFinder.findPath();
        pathManager.updateTargetPath(minecraft.player.getUUID(), currentPath);
        
        currentTarget = target;
        pathUpdateTimer = 0;
        lastPlayerPos = start;
        lastTargetPos = targetPos;
    }

    public static void stopTracking() {
        currentPath = null;
        currentTarget = null;
        pathUpdateTimer = 0;
        lastPlayerPos = null;
        lastTargetPos = null;
        
        // Clear the path
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            pathManager.clearPath(minecraft.player.getUUID());
        }
    }
} 