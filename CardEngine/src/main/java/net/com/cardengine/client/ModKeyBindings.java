package net.com.cardengine.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String KEY_CATEGORY = "key.categories.card_engine";

    public static final KeyMapping TOGGLE_SELECTION_KEY = new KeyMapping(
            "key.card_engine.toggle_selection",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KEY_CATEGORY
    );

    public static final KeyMapping TOGGLE_STATS_KEY = new KeyMapping(
            "key.card_engine.toggle_stats",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            KEY_CATEGORY
    );

    public static final KeyMapping ACTIVE_SKILL_1_KEY = new KeyMapping(
            "key.card_engine.active_skill_1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );

    public static final KeyMapping ACTIVE_SKILL_2_KEY = new KeyMapping(
            "key.card_engine.active_skill_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            KEY_CATEGORY
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_SELECTION_KEY);
        event.register(TOGGLE_STATS_KEY);
        event.register(ACTIVE_SKILL_1_KEY);
        event.register(ACTIVE_SKILL_2_KEY);
    }
}
