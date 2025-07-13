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
    private static final int ANIMATION_CYCLE = 30; // 3 second cycle
    private final Map<UUID, Integer> pathAnimationOffsets = new HashMap<>();
    
    public void updateTargetPath(UUID playerId, List<BlockPos> path) {
        if (path == null || path.isEmpty()) {
            targetPaths.remove(playerId);
            pathAnimationOffsets.remove(playerId);
        } else {
            targetPaths.put(playerId, new ArrayList<>(path));
            pathAnimationOffsets.putIfAbsent(playerId, 0);
        }
    }

    public void clearPath(UUID playerId) {
        targetPaths.remove(playerId);
        pathAnimationOffsets.remove(playerId);
    }

    public void spawnParticles(Player player, Level level) {
        UUID playerId = player.getUUID();
        List<BlockPos> targetPath = targetPaths.get(playerId);
        
        if (targetPath != null && targetPath.size() > 1) {
            // Update animation offset
            int offset = pathAnimationOffsets.getOrDefault(playerId, 0);
            offset = (offset + 1) % ANIMATION_CYCLE;
            pathAnimationOffsets.put(playerId, offset);

            // Calculate total path length
            double totalLength = 0;
            List<Double> segmentLengths = new ArrayList<>();
            
            for (int i = 0; i < targetPath.size() - 1; i++) {
                BlockPos current = targetPath.get(i);
                BlockPos next = targetPath.get(i + 1);
                double length = Math.sqrt(
                    Math.pow(next.getX() - current.getX(), 2) +
                    Math.pow(next.getY() - current.getY(), 2) +
                    Math.pow(next.getZ() - current.getZ(), 2)
                );
                totalLength += length;
                segmentLengths.add(length);
            }

            // Wave parameters
            double waveWidth = totalLength * 0.3; // Width of the pulse (30% of path length)
            double waveProgress = (offset / (double)ANIMATION_CYCLE) * (totalLength + waveWidth * 2);
            double wavePeak = waveProgress - waveWidth;
            
            // Track current distance along path
            double currentLength = 0;
            
            for (int i = 0; i < targetPath.size() - 1; i++) {
                BlockPos current = targetPath.get(i);
                BlockPos next = targetPath.get(i + 1);
                double segmentLength = segmentLengths.get(i);
                
                // Spawn particles along this segment
                int particlesInSegment = Math.max(2, (int)(segmentLength * 2));
                for (int p = 0; p < particlesInSegment; p++) {
                    double segmentProgress = p / (double)particlesInSegment;
                    double distanceAlongPath = currentLength + segmentLength * segmentProgress;
                    
                    // Calculate particle position
                    double x = current.getX() + (next.getX() - current.getX()) * segmentProgress;
                    double y = current.getY() + (next.getY() - current.getY()) * segmentProgress;
                    double z = current.getZ() + (next.getZ() - current.getZ()) * segmentProgress;
                    
                    // Calculate alpha based on distance from wave peak
                    double distanceFromPeak = Math.abs(distanceAlongPath - wavePeak);
                    double alpha = Math.max(0, 1 - (distanceFromPeak / waveWidth));
                    
                    if (alpha > 0.05) { // Only spawn visible particles
                        // Add small random offset
                        double offsetX = level.random.nextDouble() * 0.1 - 0.05;
                        double offsetZ = level.random.nextDouble() * 0.1 - 0.05;
                        
                        // Create particle with alpha-based size
                        DustParticleOptions particle = new DustParticleOptions(
                            new Vector3f(1.0F, 0.0F, 0.0F), 
                            (float)(0.8f * alpha)
                        );
                        
                        level.addParticle(
                            particle,
                            x + 0.5 + offsetX,
                            y + 0.5,
                            z + 0.5 + offsetZ,
                            0, 0, 0
                        );
                    }
                }
                
                currentLength += segmentLength;
            }
        }
    }
} 