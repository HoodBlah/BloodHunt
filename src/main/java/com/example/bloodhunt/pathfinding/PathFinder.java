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
        Node startNode = new Node(start, null);
        startNode.g = 0;
        startNode.h = heuristic(start);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.pos.equals(target)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) continue;

                double tentativeG = current.g + 1;

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

        // If no path found, create direct path
        return createDirectPath();
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    BlockPos neighbor = pos.offset(x, y, z);
                    if (isValidMove(neighbor)) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }
        return neighbors;
    }

    private boolean isValidMove(BlockPos pos) {
        if (pos.distManhattan(start) > maxDistance) return false;

        BlockState state = level.getBlockState(pos);
        
        // Check if block is air or waterlogged
        if (state.isAir()) return true;
        if (state.getBlock() instanceof SimpleWaterloggedBlock) {
            FluidState fluidState = state.getFluidState();
            return fluidState.getType() == Fluids.WATER;
        }
        
        return false;
    }

    private double heuristic(BlockPos pos) {
        return pos.distManhattan(target);
    }

    private List<BlockPos> reconstructPath(Node end) {
        List<BlockPos> path = new ArrayList<>();
        Node current = end;
        while (current != null) {
            path.add(0, current.pos);
            current = current.parent;
        }
        return path;
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