package com.example.bloodhunt.render;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.List;
import java.util.ArrayList;

public class BloodTrailRenderer {
    private static final float TRAIL_WIDTH = 0.25f; // Even thinner for elegance
    private static final float TRAIL_HEIGHT = 0.3f;
    private static final float VERTICAL_OFFSET = 0.15f;
    private static final float UNDULATION_FREQUENCY = 1.5f;
    private static final float UNDULATION_AMPLITUDE = 0.1f;
    private static final float FLOW_SPEED = 2.5f;
    private static final int CURVE_SEGMENTS = 8; // Number of segments per curve
    private static float animationTime = 0f;

    private static Vec3 bezierPoint(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;
        
        Vec3 point = p0.scale(uuu);
        point = point.add(p1.scale(3 * uu * t));
        point = point.add(p2.scale(3 * u * tt));
        point = point.add(p3.scale(ttt));
        
        return point;
    }

    private static Vec3 bezierTangent(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float u = 1 - t;
        Vec3 tangent = p1.subtract(p0).scale(3 * u * u);
        tangent = tangent.add(p2.subtract(p1).scale(6 * u * t));
        tangent = tangent.add(p3.subtract(p2).scale(3 * t * t));
        return tangent.normalize();
    }

    public static void renderTrail(PoseStack poseStack, List<BlockPos> path, Vec3 cameraPos, float partialTicks) {
        if (path == null || path.size() < 2) return;

        animationTime += partialTicks * 0.05f;
        if (animationTime > 1000f) animationTime = 0f;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f pose = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // Create control points for the entire path
        List<Vec3> controlPoints = new ArrayList<>();
        for (BlockPos pos : path) {
            controlPoints.add(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        }

        // Generate smooth path using cubic Bezier curves
        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Vec3 p0 = controlPoints.get(i);
            Vec3 p3 = controlPoints.get(Math.min(i + 1, controlPoints.size() - 1));
            
            // Calculate control points for smooth curve
            Vec3 p1, p2;
            if (i == 0) {
                // First segment
                Vec3 dir = p3.subtract(p0).normalize();
                p1 = p0.add(dir.scale(0.5));
                p2 = p3.subtract(dir.scale(0.5));
            } else if (i == controlPoints.size() - 2) {
                // Last segment
                Vec3 dir = p3.subtract(p0).normalize();
                p1 = p0.add(dir.scale(0.5));
                p2 = p3.subtract(dir.scale(0.5));
            } else {
                // Middle segments - use previous and next points for smooth transition
                Vec3 prevDir = p0.subtract(controlPoints.get(i - 1)).normalize();
                Vec3 nextDir = controlPoints.get(i + 2).subtract(p3).normalize();
                p1 = p0.add(prevDir.scale(0.5));
                p2 = p3.add(nextDir.scale(0.5));
            }

            // Render segments along the curve
            for (int j = 0; j < CURVE_SEGMENTS; j++) {
                float t = j / (float)CURVE_SEGMENTS;
                float nextT = (j + 1) / (float)CURVE_SEGMENTS;
                
                // Calculate positions along the curve
                Vec3 pos = bezierPoint(p0, p1, p2, p3, t);
                Vec3 nextPos = bezierPoint(p0, p1, p2, p3, nextT);
                
                // Calculate tangents for perpendicular vectors
                Vec3 tangent = bezierTangent(p0, p1, p2, p3, t);
                Vec3 perpendicular = new Vec3(-tangent.z, 0, tangent.x).normalize();

                // Calculate progress for effects
                float segmentProgress = (i * CURVE_SEGMENTS + j) / (float)((controlPoints.size() - 1) * CURVE_SEGMENTS);
                
                // Calculate undulation and flow effects
                float undulation = (float) Math.sin(animationTime * UNDULATION_FREQUENCY + segmentProgress * 10f) * UNDULATION_AMPLITUDE;
                float flow = (float) Math.sin(animationTime * FLOW_SPEED - segmentProgress * 5f) * 0.5f + 0.5f;
                
                // Calculate fade effects
                float fadeEffect = Math.min(segmentProgress * 3, Math.min((1 - segmentProgress) * 3, 1));
                
                // Calculate vertices
                Vec3 right = perpendicular.scale(TRAIL_WIDTH * (1 + flow * 0.2f));
                
                // Add vertices with smooth color transition
                float alpha = 0.6f * fadeEffect * (0.8f + flow * 0.2f);
                float r = 0.9f + flow * 0.1f;
                float g = 0.2f + flow * 0.1f;
                float b = 0.2f + flow * 0.1f;

                // Left vertex
                bufferBuilder.vertex(pose, 
                    (float)(pos.x + right.x), 
                    (float)(pos.y + VERTICAL_OFFSET + undulation), 
                    (float)(pos.z + right.z))
                    .color(r, g, b, alpha)
                    .endVertex();

                // Right vertex
                bufferBuilder.vertex(pose, 
                    (float)(pos.x - right.x), 
                    (float)(pos.y + VERTICAL_OFFSET + undulation), 
                    (float)(pos.z - right.z))
                    .color(r, g, b, alpha)
                    .endVertex();
            }
        }

        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        poseStack.popPose();
    }
} 