package com.example.bloodhunt.client.gui;

import com.example.bloodhunt.BloodhuntManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class EntitySelectorScreen extends Screen {
    private List<LivingEntity> nearbyEntities;
    private int selectedIndex = -1;
    private static final int BUTTON_HEIGHT = 32; // Increased height for entity preview
    private static final int BUTTON_WIDTH = 200;
    private static final int MAX_RANGE = 50;
    private static final int SCROLL_BAR_WIDTH = 6;
    private static final int ENTITY_RENDER_SIZE = 24; // Size of entity preview
    private int scrollOffset = 0;
    private boolean isDragging = false;
    private float scrollProgress = 0;
    private List<Button> entityButtons = new ArrayList<>();
    private Button endHuntButton;
    private int contentHeight;
    private int visibleHeight;

    public EntitySelectorScreen() {
        super(Component.translatable("screen.bloodhunt.entity_selector"));
        this.nearbyEntities = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        updateNearbyEntities();
        
        // Calculate visible height for scrolling area
        visibleHeight = this.height - 80; // Leave space for title and padding
        
        // Create entity buttons (they will be positioned during render)
        entityButtons.clear();
        for (int i = 0; i < nearbyEntities.size(); i++) {
            final int index = i;
            LivingEntity entity = nearbyEntities.get(i);
            String name = entity.getName().getString();
            int distance = (int) entity.distanceTo(minecraft.player);
            
            Button entityButton = Button.builder(
                Component.literal("    " + name + " (" + distance + "m)"), // Add padding for entity render
                btn -> selectEntity(index))
                .pos(this.width / 2 - BUTTON_WIDTH / 2, 0) // Y position will be set during render
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
            entityButtons.add(entityButton);
        }

        // Calculate total content height
        contentHeight = nearbyEntities.size() * (BUTTON_HEIGHT + 5);

        // Add End Hunt button in top right
        endHuntButton = Button.builder(
            Component.literal("End Hunt"),
            btn -> {
                BloodhuntManager.stopTracking();
                this.onClose();
            })
            .pos(this.width - BUTTON_WIDTH / 2 - 10, 10)
            .size(BUTTON_WIDTH / 2, BUTTON_HEIGHT)
            .build();
        this.addRenderableWidget(endHuntButton);
    }

    private void renderEntityPreview(GuiGraphics graphics, LivingEntity entity, int x, int y, float scale) {
        if (entity == null || minecraft == null) return;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        
        // Position the entity in front of GUI
        poseStack.translate(
            x + ENTITY_RENDER_SIZE / 2.0F,  // Center horizontally
            y + ENTITY_RENDER_SIZE,         // Bottom align
            50.0                            // Put in front of GUI
        );
        
        // Scale based on entity size
        float entityHeight = entity.getBbHeight();
        float entityWidth = entity.getBbWidth();
        float scaleFactor = scale / Math.max(entityHeight, entityWidth);
        poseStack.scale(scaleFactor, -scaleFactor, scaleFactor); // Negative Y to fix upside down
        
        // Get the render dispatcher and buffer
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        
        // Render with proper buffer handling
        var bufferSource = minecraft.renderBuffers().bufferSource();
        dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 180.0F, 1.0F, poseStack, bufferSource, 15728880);
        bufferSource.endBatch();
        
        poseStack.popPose();
        dispatcher.setRenderShadow(true);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentHeight > visibleHeight) {
            float amount = (float) (delta * 20);
            scrollProgress = Mth.clamp(scrollProgress - amount / (contentHeight - visibleHeight), 0.0F, 1.0F);
            updateButtonPositions();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && contentHeight > visibleHeight) {
            float dragAmount = (float) dragY / (visibleHeight - BUTTON_HEIGHT);
            scrollProgress = Mth.clamp(scrollProgress + dragAmount, 0.0F, 1.0F);
            updateButtonPositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contentHeight > visibleHeight) {
            // Check if click is in scroll bar area
            int scrollBarX = this.width / 2 + BUTTON_WIDTH / 2 + 4;
            if (mouseX >= scrollBarX && mouseX <= scrollBarX + SCROLL_BAR_WIDTH &&
                mouseY >= 40 && mouseY <= 40 + visibleHeight) {
                isDragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateButtonPositions() {
        scrollOffset = Math.round(scrollProgress * (contentHeight - visibleHeight));
        
        // Update button positions
        for (int i = 0; i < entityButtons.size(); i++) {
            Button button = entityButtons.get(i);
            int yPos = 40 + i * (BUTTON_HEIGHT + 5) - scrollOffset;
            
            // Only add buttons that are visible
            if (yPos >= 40 && yPos <= 40 + visibleHeight) {
                button.setY(yPos);
                if (!this.renderables.contains(button)) {
                    this.addRenderableWidget(button);
                }
            } else {
                this.renderables.remove(button);
            }
        }
    }

    private void updateNearbyEntities() {
        if (minecraft.player == null || minecraft.level == null) return;
        
        nearbyEntities.clear();
        List<Entity> allEntities = minecraft.level.getEntities(minecraft.player, 
            minecraft.player.getBoundingBox().inflate(MAX_RANGE));
            
        for (Entity entity : allEntities) {
            if (entity instanceof LivingEntity && entity != minecraft.player) {
                nearbyEntities.add((LivingEntity) entity);
            }
        }
        
        // Sort by distance
        nearbyEntities.sort(Comparator.comparingDouble(
            entity -> entity.distanceToSqr(minecraft.player)));
    }

    private void selectEntity(int index) {
        if (index >= 0 && index < nearbyEntities.size()) {
            selectedIndex = index;
            LivingEntity target = nearbyEntities.get(index);
            BloodhuntManager.startTracking(target);
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        
        // Draw title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw entity count
        String countText = nearbyEntities.size() + " Entities Found";
        graphics.drawString(this.font, countText, 10, 15, 0xFFFFFF);
        
        // Update button positions
        updateButtonPositions();
        
        // Draw scroll bar if needed
        if (contentHeight > visibleHeight) {
            int scrollBarX = this.width / 2 + BUTTON_WIDTH / 2 + 4;
            int scrollBarHeight = Math.max(20, (int)((float)visibleHeight * visibleHeight / contentHeight));
            int scrollBarY = 40 + (int)((visibleHeight - scrollBarHeight) * scrollProgress);
            
            // Draw scroll bar background
            graphics.fill(scrollBarX, 40, scrollBarX + SCROLL_BAR_WIDTH, 40 + visibleHeight, 0x33FFFFFF);
            // Draw scroll bar
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + SCROLL_BAR_WIDTH, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
        }
        
        super.render(graphics, mouseX, mouseY, partialTicks);
        
        // Draw scissored content area
        graphics.enableScissor(
            0,
            40,
            this.width,
            40 + visibleHeight
        );
        
        // Render entity previews
        for (int i = 0; i < entityButtons.size(); i++) {
            Button button = entityButtons.get(i);
            if (this.renderables.contains(button)) {
                LivingEntity entity = nearbyEntities.get(i);
                renderEntityPreview(
                    graphics,
                    entity,
                    button.getX() + 4, // Small padding from button edge
                    button.getY() + (BUTTON_HEIGHT - ENTITY_RENDER_SIZE) / 2, // Center vertically
                    16.0F // Scale factor for preview
                );
            }
        }
        
        graphics.disableScissor();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 