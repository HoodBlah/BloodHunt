package com.example.bloodhunt.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

public class PathFinder {
    private final Level level;
    private final BlockPos start;
    private final BlockPos target;
    private final int maxDistance;
    private final Set<BlockPos> closedSet;
    private final PriorityQueue<Node> openSet;
    private final Map<BlockPos, Node> allNodes;
    private static final int MAX_ITERATIONS = 1000; // Prevent infinite loops
    private static final BlockPos[] NEIGHBOR_OFFSETS = {
        // Primary directions (more important)
        new BlockPos(1, 0, 0),
        new BlockPos(-1, 0, 0),
        new BlockPos(0, 1, 0),
        new BlockPos(0, -1, 0),
        new BlockPos(0, 0, 1),
        new BlockPos(0, 0, -1),
        // Diagonal directions (less important)
        new BlockPos(1, 1, 0),
        new BlockPos(-1, 1, 0),
        new BlockPos(1, -1, 0),
        new BlockPos(-1, -1, 0),
        new BlockPos(0, 1, 1),
        new BlockPos(0, 1, -1),
        new BlockPos(0, -1, 1),
        new BlockPos(0, -1, -1),
        // Full diagonals (least important)
        new BlockPos(1, 1, 1),
        new BlockPos(-1, 1, 1),
        new BlockPos(1, -1, 1),
        new BlockPos(-1, -1, 1),
        new BlockPos(1, 1, -1),
        new BlockPos(-1, 1, -1),
        new BlockPos(1, -1, -1),
        new BlockPos(-1, -1, -1)
    };

    public PathFinder(Level level, BlockPos start, BlockPos target, int maxDistance) {
        this.level = level;
        this.start = start;
        this.target = target;
        this.maxDistance = maxDistance;
        this.closedSet = new HashSet<>();
        this.openSet = new PriorityQueue<>();
        this.allNodes = new HashMap<>();
    }

    public List<BlockPos> findPath() {
        // If target is too far, return direct path immediately
        if (start.distManhattan(target) > maxDistance) {
            return createDirectPath();
        }

        Node startNode = new Node(start, null);
        startNode.g = 0;
        startNode.h = heuristic(start);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;
        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            Node current = openSet.poll();

            if (current.pos.equals(target)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            // Early exit if we're getting too far from the target
            if (current.pos.distManhattan(target) > maxDistance) {
                continue;
            }

            // Process neighbors in order of importance
            for (BlockPos offset : NEIGHBOR_OFFSETS) {
                BlockPos neighbor = current.pos.offset(offset);
                
                // Skip if already processed or too far
                if (closedSet.contains(neighbor) || neighbor.distManhattan(start) > maxDistance) {
                    continue;
                }

                // Skip if not a valid move
                if (!isValidMove(neighbor)) {
                    continue;
                }

                double tentativeG = current.g + offset.distManhattan(BlockPos.ZERO); // Cost based on movement type

                Node neighborNode = allNodes.get(neighbor);
                if (neighborNode == null) {
                    neighborNode = new Node(neighbor, current);
                    neighborNode.g = tentativeG;
                    neighborNode.h = heuristic(neighbor);
                    openSet.add(neighborNode);
                    allNodes.put(neighbor, neighborNode);
                } else if (tentativeG < neighborNode.g) {
                    neighborNode.parent = current;
                    neighborNode.g = tentativeG;
                    // Re-add to update position in priority queue
                    openSet.remove(neighborNode);
                    openSet.add(neighborNode);
                }
            }
        }

        // If no path found or too many iterations, create direct path
        return createDirectPath();
    }

    private boolean isValidMove(BlockPos pos) {
        if (pos.distManhattan(start) > maxDistance) return false;

        BlockState state = level.getBlockState(pos);
        BlockState stateAbove = level.getBlockState(pos.above());
        
        // Check if current position is passable
        boolean isPassable = state.isAir() || 
            (state.getBlock() instanceof SimpleWaterloggedBlock && state.getFluidState().getType() == Fluids.WATER);
            
        // Check if there's space above (for player height)
        boolean hasSpaceAbove = stateAbove.isAir() || 
            (stateAbove.getBlock() instanceof SimpleWaterloggedBlock && stateAbove.getFluidState().getType() == Fluids.WATER);
        
        return isPassable && hasSpaceAbove;
    }

    private boolean canSeeThrough(BlockPos from, BlockPos to) {
        // Use Bresenham's line algorithm to check if there's a clear line of sight
        int x1 = from.getX();
        int y1 = from.getY();
        int z1 = from.getZ();
        int x2 = to.getX();
        int y2 = to.getY();
        int z2 = to.getZ();
        
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);
        
        int xs = x1 < x2 ? 1 : -1;
        int ys = y1 < y2 ? 1 : -1;
        int zs = z1 < z2 ? 1 : -1;
        
        // Driving axis is X-axis
        if (dx >= dy && dx >= dz) {
            int p1 = 2 * dy - dx;
            int p2 = 2 * dz - dx;
            while (x1 != x2) {
                x1 += xs;
                if (p1 >= 0) {
                    y1 += ys;
                    p1 -= 2 * dx;
                }
                if (p2 >= 0) {
                    z1 += zs;
                    p2 -= 2 * dx;
                }
                p1 += 2 * dy;
                p2 += 2 * dz;
                
                if (!isValidMove(new BlockPos(x1, y1, z1))) {
                    return false;
                }
            }
        }
        // Driving axis is Y-axis
        else if (dy >= dx && dy >= dz) {
            int p1 = 2 * dx - dy;
            int p2 = 2 * dz - dy;
            while (y1 != y2) {
                y1 += ys;
                if (p1 >= 0) {
                    x1 += xs;
                    p1 -= 2 * dy;
                }
                if (p2 >= 0) {
                    z1 += zs;
                    p2 -= 2 * dy;
                }
                p1 += 2 * dx;
                p2 += 2 * dz;
                
                if (!isValidMove(new BlockPos(x1, y1, z1))) {
                    return false;
                }
            }
        }
        // Driving axis is Z-axis
        else {
            int p1 = 2 * dy - dz;
            int p2 = 2 * dx - dz;
            while (z1 != z2) {
                z1 += zs;
                if (p1 >= 0) {
                    y1 += ys;
                    p1 -= 2 * dz;
                }
                if (p2 >= 0) {
                    x1 += xs;
                    p2 -= 2 * dz;
                }
                p1 += 2 * dy;
                p2 += 2 * dx;
                
                if (!isValidMove(new BlockPos(x1, y1, z1))) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private double heuristic(BlockPos pos) {
        // Use octile distance for better diagonal movement estimation
        int dx = Math.abs(pos.getX() - target.getX());
        int dy = Math.abs(pos.getY() - target.getY());
        int dz = Math.abs(pos.getZ() - target.getZ());
        
        // Octile distance calculation
        double straight = Math.abs(dx - dz);
        double diagonal = Math.min(dx, dz);
        return (straight + diagonal * Math.sqrt(2)) + dy;
    }

    private List<BlockPos> reconstructPath(Node end) {
        List<BlockPos> path = new ArrayList<>();
        Node current = end;
        while (current != null) {
            path.add(0, current.pos);
            current = current.parent;
        }
        return optimizePath(path);
    }

    private List<BlockPos> optimizePath(List<BlockPos> path) {
        if (path.size() <= 2) return path;
        
        List<BlockPos> optimized = new ArrayList<>();
        optimized.add(path.get(0));
        
        for (int i = 1; i < path.size() - 1; i++) {
            BlockPos prev = path.get(i - 1);
            BlockPos current = path.get(i);
            BlockPos next = path.get(i + 1);
            
            // Keep points that are not in a straight line or where line of sight is blocked
            if (!isInLine(prev, current, next) || !canSeeThrough(prev, next)) {
                optimized.add(current);
            }
        }
        
        optimized.add(path.get(path.size() - 1));
        return optimized;
    }

    private boolean isInLine(BlockPos a, BlockPos b, BlockPos c) {
        // Check if three points are in a straight line
        int dx1 = b.getX() - a.getX();
        int dy1 = b.getY() - a.getY();
        int dz1 = b.getZ() - a.getZ();
        
        int dx2 = c.getX() - b.getX();
        int dy2 = c.getY() - b.getY();
        int dz2 = c.getZ() - b.getZ();
        
        // Check if the direction vectors are parallel
        return dx1 * dy2 == dy1 * dx2 && 
               dy1 * dz2 == dz1 * dy2 && 
               dx1 * dz2 == dz1 * dx2;
    }

    private List<BlockPos> createDirectPath() {
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = start;
        
        while (!current.equals(target)) {
            path.add(current);
            
            // Move one block closer to target in each dimension
            int x = current.getX();
            int y = current.getY();
            int z = current.getZ();
            
            if (x < target.getX()) x++;
            else if (x > target.getX()) x--;
            
            if (y < target.getY()) y++;
            else if (y > target.getY()) y--;
            
            if (z < target.getZ()) z++;
            else if (z > target.getZ()) z--;
            
            current = new BlockPos(x, y, z);
        }
        
        path.add(target);
        return path;
    }

    private static class Node implements Comparable<Node> {
        BlockPos pos;
        Node parent;
        double g; // Cost from start to this node
        double h; // Estimated cost from this node to target
        
        Node(BlockPos pos, Node parent) {
            this.pos = pos;
            this.parent = parent;
        }
        
        double f() {
            return g + h;
        }
        
        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f(), other.f());
        }
    }
} 