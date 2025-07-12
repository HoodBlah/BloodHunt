package com.example.bloodhunt.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String KEY_CATEGORY_BLOODHUNT = "key.category.bloodhunt";
    public static final String KEY_OPEN_SELECTOR = "key.bloodhunt.open_selector";
    
    public static final KeyMapping OPEN_SELECTOR_KEY = new KeyMapping(
        KEY_OPEN_SELECTOR,
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B, // Default to B key
        KEY_CATEGORY_BLOODHUNT
    );
} 