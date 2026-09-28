package net.com.cardengine.client;

import net.com.cardengine.CardEngineMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CardEngineMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CustomRenderHealthBar {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isCreative() || mc.player.isSpectator()) return;

        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true); // Disable vanilla health bar
            renderCustomHealthBar(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), mc.player);
        } else if (event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) {
            event.setCanceled(true); // Disable vanilla armor bar
            renderCustomArmorBar(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), mc.player);
        }
    }

    private static void renderCustomHealthBar(GuiGraphics guiGraphics, int width, int height, Player player) {
        float maxHealth = player.getMaxHealth();
        float health = player.getHealth();
        float absorption = player.getAbsorptionAmount();

        // Determine heart types (normal, poison, wither)
        int heartType = 0; // normal
        if (player.hasEffect(MobEffects.POISON)) {
            heartType = 1;
        } else if (player.hasEffect(MobEffects.WITHER)) {
            heartType = 2;
        }

        // Determine if hearts should blink (regeneration, etc.)
        boolean blinking = false;
        if (player.hasEffect(MobEffects.REGENERATION) && player.tickCount % 20 < 5) {
            blinking = true;
        }

        int startX = width / 2 - 91;
        int startYBottom = height - 39;

        // Number of heart slots occupied by current red health (capped at 10)
        int redHeartSlots = (int) Math.ceil(Math.min(health, 20f) / 2f);

        // --- PASS 1: Draw red health hearts (slots 0 .. redHeartSlots-1) ---
        for (int i = 0; i < 10; i++) {
            int heartX = startX + i * 8;
            // Background outline for every slot
            guiGraphics.blit(ICONS, heartX, startYBottom, blinking ? 25 : 16, 0, 9, 9);

            if (i < redHeartSlots) {
                // Draw filled/half red heart for this slot
                float currentHP = health - i * 2;
                if (currentHP >= 2) {
                    int u = (heartType == 0) ? 52 : (heartType == 1 ? 88 : 124);
                    guiGraphics.blit(ICONS, heartX, startYBottom, u, 0, 9, 9);
                } else if (currentHP > 0) {
                    int u = (heartType == 0) ? 61 : (heartType == 1 ? 97 : 133);
                    guiGraphics.blit(ICONS, heartX, startYBottom, u, 0, 9, 9);
                }
            }
        }

        // Render the excess health text indicator on the left if health > 20 HP
        if (health > 20f) {
            double extraHearts = (health - 20.0) / 2.0;
            String text;
            if (extraHearts % 1.0 == 0.0) {
                text = "+" + (int) extraHearts;
            } else {
                text = String.format(java.util.Locale.US, "+%.1f", extraHearts);
            }

            int textX = startX - 4 - Minecraft.getInstance().font.width(text);
            guiGraphics.drawString(Minecraft.getInstance().font, text, textX, startYBottom + 1, 0xFFFF0000);
        }

        // --- PASS 2: Draw absorption (yellow) hearts in the remaining empty slots ---
        if (absorption > 0f) {
            int absorbHeartsCount = (int) Math.ceil(absorption / 2f);
            // Absorption starts immediately after red health slots
            int startSlot = redHeartSlots;
            if (startSlot < 0) startSlot = 0;

            for (int i = 0; i < absorbHeartsCount; i++) {
                int slot = startSlot + i;
                if (slot >= 10) {
                    break; // Keep strictly within the 10 hearts row
                }

                int heartX = startX + slot * 8;
                // Draw absorption outline background (already drawn in pass 1, but re-draw to be safe)
                guiGraphics.blit(ICONS, heartX, startYBottom, blinking ? 25 : 16, 0, 9, 9);

                float currentAbsorb = absorption - i * 2;
                if (currentAbsorb >= 2) {
                    guiGraphics.blit(ICONS, heartX, startYBottom, 160, 0, 9, 9);
                } else if (currentAbsorb > 0) {
                    guiGraphics.blit(ICONS, heartX, startYBottom, 169, 0, 9, 9);
                }
            }
        }
    }

    private static void renderCustomArmorBar(GuiGraphics guiGraphics, int width, int height, Player player) {
        int armor = player.getArmorValue();
        if (armor <= 0) return;

        int startX = width / 2 - 91;
        int startY = height - 49; // Always at standard position directly above the single row of hearts

        for (int i = 0; i < 10; i++) {
            int x = startX + i * 8;
            int currentArmor = armor - i * 2;
            if (currentArmor >= 2) {
                guiGraphics.blit(ICONS, x, startY, 34, 9, 9, 9);
            } else if (currentArmor > 0) {
                guiGraphics.blit(ICONS, x, startY, 25, 9, 9, 9);
            } else {
                guiGraphics.blit(ICONS, x, startY, 16, 9, 9, 9);
            }
        }
    }
}
