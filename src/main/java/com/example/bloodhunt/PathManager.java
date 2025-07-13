package com.example.bloodhunt;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import java.util.*;

public class PathManager {
    private final Map<UUID, List<BlockPos>> targetPaths = new HashMap<>();
    private static final int ANIMATION_CYCLE = 40; // Increased from 30 to 40 ticks (2 seconds)
    private final Map<UUID, Integer> pathAnimationOffsets = new HashMap<>();
    private static final int PARTICLE_SPACING = 3; // Show particles every 3 blocks
    private static final int MAX_PARTICLES = 50; // Maximum number of particles to spawn per tick
    private static final double PARTICLE_SPEED = 0.02; // Reduced particle speed
    private final Map<UUID, Long> lastParticleTime = new HashMap<>();
    private static final long PARTICLE_DELAY = 50; // Minimum milliseconds between particle spawns
    
    public void updateTargetPath(UUID playerId, List<BlockPos> path) {
        if (path == null || path.isEmpty()) {
            targetPaths.remove(playerId);
            pathAnimationOffsets.remove(playerId);
            lastParticleTime.remove(playerId);
        } else {
            targetPaths.put(playerId, new ArrayList<>(path));
            pathAnimationOffsets.putIfAbsent(playerId, 0);
            lastParticleTime.putIfAbsent(playerId, System.currentTimeMillis());
        }
    }

    public void clearPath(UUID playerId) {
        targetPaths.remove(playerId);
        pathAnimationOffsets.remove(playerId);
        lastParticleTime.remove(playerId);
    }

    public void spawnParticles(Player player, Level level) {
        UUID playerId = player.getUUID();
        List<BlockPos> targetPath = targetPaths.get(playerId);
        
        if (targetPath != null && targetPath.size() > 1) {
            // Check if enough time has passed since last particle spawn
            long currentTime = System.currentTimeMillis();
            long lastTime = lastParticleTime.getOrDefault(playerId, 0L);
            if (currentTime - lastTime < PARTICLE_DELAY) {
                return;
            }
            lastParticleTime.put(playerId, currentTime);
            
            // Update animation offset
            int offset = pathAnimationOffsets.getOrDefault(playerId, 0);
            offset = (offset + 1) % ANIMATION_CYCLE;
            pathAnimationOffsets.put(playerId, offset);

            // Calculate total path length and segment lengths
            double totalLength = 0;
            List<Double> segmentLengths = new ArrayList<>();
            List<Vec3> pathPoints = new ArrayList<>();
            
            // Convert BlockPos to Vec3 and calculate lengths
            for (int i = 0; i < targetPath.size(); i++) {
                BlockPos pos = targetPath.get(i);
                // Position particles slightly above ground and centered in block
                Vec3 point = new Vec3(
                    pos.getX() + 0.5,
                    pos.getY() + 0.2, // Lower to just above ground
                    pos.getZ() + 0.5
                );
                pathPoints.add(point);
                
                if (i > 0) {
                    double length = point.distanceTo(pathPoints.get(i - 1));
                    totalLength += length;
                    segmentLengths.add(length);
                }
            }

            // Calculate number of particles based on path length
            int particleCount = Math.min(MAX_PARTICLES, (int)(totalLength / PARTICLE_SPACING));
            if (particleCount < 1) particleCount = 1;

            // Spawn particles along the path
            double particleSpacing = totalLength / particleCount;
            double progress = (double) offset / ANIMATION_CYCLE;
            
            for (int i = 0; i < particleCount; i++) {
                double particleProgress = (progress + (double) i / particleCount) % 1.0;
                Vec3 particlePos = getPositionAlongPath(pathPoints, segmentLengths, totalLength * particleProgress);
                
                if (particlePos != null) {
                    // Calculate fade based on distance from player
                    double distanceToPlayer = player.position().distanceTo(particlePos);
                    float alpha = (float) Math.max(0.4, 1.0 - (distanceToPlayer / 32.0)); // Increased minimum alpha
                    
                    // Add slight random offset for more natural look
                    double offsetX = (level.random.nextDouble() - 0.5) * 0.1;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 0.1;
                    
                    // Check if the position is valid (not inside a block)
                    BlockPos blockPos = new BlockPos(
                        (int)Math.floor(particlePos.x + offsetX),
                        (int)Math.floor(particlePos.y),
                        (int)Math.floor(particlePos.z + offsetZ)
                    );
                    
                    if (level.getBlockState(blockPos).isAir()) {
                        // Spawn particle with enhanced visibility
                        level.addParticle(
                            new DustParticleOptions(
                                new Vector3f(0.9f, 0.1f, 0.1f), // Brighter red
                                alpha
                            ),
                            particlePos.x + offsetX,
                            particlePos.y,
                            particlePos.z + offsetZ,
                            0,
                            PARTICLE_SPEED,
                            0
                        );
                    }
                }
            }
        }
    }

    private Vec3 getPositionAlongPath(List<Vec3> points, List<Double> segmentLengths, double targetDistance) {
        if (points.size() < 2) return null;
        
        double currentDistance = 0;
        
        for (int i = 0; i < segmentLengths.size(); i++) {
            double segmentLength = segmentLengths.get(i);
            
            if (currentDistance + segmentLength >= targetDistance) {
                // Calculate position within this segment
                double segmentProgress = (targetDistance - currentDistance) / segmentLength;
                Vec3 start = points.get(i);
                Vec3 end = points.get(i + 1);
                
                return start.lerp(end, segmentProgress);
            }
            
            currentDistance += segmentLength;
        }
        
        return points.get(points.size() - 1);
    }
} 