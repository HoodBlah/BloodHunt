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
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class EntitySelectorScreen extends Screen {
    private List<LivingEntity> nearbyEntities;
    private int selectedIndex = -1;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 200;
    private static final int MAX_RANGE = 50; // Maximum range to search for entities

    public EntitySelectorScreen() {
        super(Component.translatable("screen.bloodhunt.entity_selector"));
        this.nearbyEntities = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        updateNearbyEntities();
        
        // Add entity buttons
        for (int i = 0; i < nearbyEntities.size(); i++) {
            final int index = i;
            LivingEntity entity = nearbyEntities.get(i);
            String name = entity.getName().getString();
            int distance = (int) entity.distanceTo(minecraft.player);
            
            this.addRenderableWidget(Button.builder(
                Component.literal(name + " (" + distance + "m)"),
                button -> selectEntity(index))
                .pos(this.width / 2 - BUTTON_WIDTH / 2, 40 + i * (BUTTON_HEIGHT + 5))
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        }

        // Add End Hunt button at the bottom
        int bottomY = Math.max(40 + nearbyEntities.size() * (BUTTON_HEIGHT + 5), this.height - 60);
        this.addRenderableWidget(Button.builder(
            Component.literal("End Hunt"),
            button -> {
                BloodhuntManager.stopTracking();
                this.onClose();
            })
            .pos(this.width / 2 - BUTTON_WIDTH / 2, bottomY)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
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
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 