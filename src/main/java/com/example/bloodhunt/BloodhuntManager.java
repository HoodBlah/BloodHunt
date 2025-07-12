package com.example.bloodhunt;

import com.example.bloodhunt.client.KeyBindings;
import com.example.bloodhunt.client.gui.EntitySelectorScreen;
import com.example.bloodhunt.particle.ModParticles;
import com.example.bloodhunt.pathfinding.PathFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Bloodhunt.MOD_ID, value = Dist.CLIENT)
public class BloodhuntManager {
    private static final int MAX_PATH_DISTANCE = 100;
    private static final int PARTICLE_SPAWN_INTERVAL = 2;
    private static List<BlockPos> currentPath = null;
    private static int particleTimer = 0;
    private static int pathIndex = 0;

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
        if (event.phase == TickEvent.Phase.END) {
            tick();
        }
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
        pathIndex = 0;
        particleTimer = 0;
    }

    public static void stopTracking() {
        currentPath = null;
        pathIndex = 0;
        particleTimer = 0;
    }

    public static void tick() {
        if (currentPath == null || currentPath.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        particleTimer++;
        if (particleTimer >= PARTICLE_SPAWN_INTERVAL) {
            particleTimer = 0;

            // Spawn particles along the path
            if (pathIndex < currentPath.size()) {
                BlockPos pos = currentPath.get(pathIndex);
                Level level = minecraft.level;

                // Spawn blood trail particle
                level.addParticle(
                    ModParticles.BLOOD_TRAIL.get(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    0, 0, 0
                );

                pathIndex++;

                // Reset path when complete
                if (pathIndex >= currentPath.size()) {
                    currentPath = null;
                    pathIndex = 0;
                }
            }
        }
    }
} 