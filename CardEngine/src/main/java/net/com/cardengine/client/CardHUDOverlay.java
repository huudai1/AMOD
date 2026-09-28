package net.com.cardengine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Map;

public class CardHUDOverlay {
    public static final IGuiOverlay HUD_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || mc.player == null) {
            return;
        }

        Font font = mc.font;
        int hotbarX = width / 2;

        // 1. Draw pending card choices indicator next to hotbar
        int pendingCount = ClientStatsHandler.getPendingChoicesCount();
        if (pendingCount > 0) {
            int indX = hotbarX - 120;
            int indY = height - 21;

            // Draw a premium mini card indicator box
            guiGraphics.fill(indX, indY, indX + 22, indY + 20, 0xDD0D0D0D);

            // Draw border around indicator box (colored gold/yellow)
            int boxBorder = 0xFFFFD700;
            guiGraphics.fill(indX, indY, indX + 22, indY + 1, boxBorder); // Top
            guiGraphics.fill(indX, indY + 19, indX + 22, indY + 20, boxBorder); // Bottom
            guiGraphics.fill(indX, indY, indX + 1, indY + 20, boxBorder); // Left
            guiGraphics.fill(indX + 21, indY, indX + 22, indY + 20, boxBorder); // Right

            // Render paper item icon (represents card)
            net.minecraft.world.item.Item paper = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", "paper"));
            if (paper != null) {
                guiGraphics.renderFakeItem(new net.minecraft.world.item.ItemStack(paper), indX + 3, indY + 2);
            }

            // Draw counter number
            String countText = String.valueOf(pendingCount);
            guiGraphics.drawString(font, countText, indX + 14, indY + 11, 0xFFFFFF);
        }

        // 2. Draw Active Skill Slots (right side of hotbar)
        renderActiveSkillSlot(guiGraphics, font, 1, hotbarX + 98, height - 21);
        renderActiveSkillSlot(guiGraphics, font, 2, hotbarX + 122, height - 21);

        // 3. Draw Cast Bar (centered above hotbar)
        renderCastBar(guiGraphics, font, width, height, partialTick);
    };

    /** Width of the cast bar in pixels */
    private static final int CAST_BAR_W = 160;
    /** Height of the cast bar in pixels */
    private static final int CAST_BAR_H = 12;

    /** Tracks how long (ms) we keep showing the "INTERRUPTED!" message after a cast is cancelled */
    private static long interruptedDisplayUntil = 0L;

    private static void renderCastBar(GuiGraphics gg, Font font, int width, int height, float partialTick) {
        boolean interrupted = ClientStatsHandler.isCastingInterrupted();
        if (interrupted) {
            interruptedDisplayUntil = System.currentTimeMillis() + 800;
        }

        boolean casting = ClientStatsHandler.isCasting();
        boolean showInterrupt = System.currentTimeMillis() < interruptedDisplayUntil;

        if (!casting && !showInterrupt) return;

        int cx = width / 2;
        // Position: just above the hotbar (hotbar is ~22px, add gap)
        int barY = height - 50;
        int barX = cx - CAST_BAR_W / 2;

        // Background track
        gg.fill(barX - 1, barY - 1, barX + CAST_BAR_W + 1, barY + CAST_BAR_H + 1, 0xCC000000);
        gg.fill(barX, barY, barX + CAST_BAR_W, barY + CAST_BAR_H, 0xFF1A1A2E);

        if (showInterrupt && !casting) {
            // Show interrupted message on the track
            String msg = "✗ INTERRUPTED!";
            int tw = font.width(msg);
            gg.drawString(font, msg, cx - tw / 2, barY + 2, 0xFFFF3333, true);
            return;
        }

        // Filled progress bar
        float progress = ClientStatsHandler.getCastingProgress();
        int fillWidth = (int) (CAST_BAR_W * progress);

        // Gradient-ish: teal → gold as it fills
        int barColor = blendColor(0xFF00D2FF, 0xFFFFD700, progress);
        gg.fill(barX, barY, barX + fillWidth, barY + CAST_BAR_H, barColor);

        // Pulsing label "CASTING..." (alpha oscillates with time)
        long now = System.currentTimeMillis();
        float pulse = 0.65f + 0.35f * (float) Math.sin((now % 1000) / 1000.0 * Math.PI * 2);
        int alpha = (int) (pulse * 255) << 24;
        String label = "CASTING...";
        int tw = font.width(label);
        gg.drawString(font, label, cx - tw / 2, barY + 2, (alpha | 0x00FFFFFF), false);

        // Time remaining text (right of bar)
        int remaining = ClientStatsHandler.getCastingRemainingTicks();
        double secs = remaining / 20.0;
        String timeText = String.format(java.util.Locale.US, "%.1fs", secs);
        int timeW = font.width(timeText);
        gg.drawString(font, timeText, barX + CAST_BAR_W - timeW, barY - 9, 0xFFCCCCCC, true);

        // Skill icon (left of bar)
        String iconName = getSkillIcon(ClientStatsHandler.getCastingSkillId());
        String[] parts = iconName.split(":");
        String ns = parts.length > 1 ? parts[0] : "minecraft";
        String pth = parts.length > 1 ? parts[1] : parts[0];
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation(ns, pth));
        if (item != null) {
            gg.renderFakeItem(new net.minecraft.world.item.ItemStack(item), barX - 18, barY - 2);
        }
    }

    /** Linearly blend two ARGB colors by a 0.0–1.0 factor. */
    private static int blendColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static void renderActiveSkillSlot(GuiGraphics guiGraphics, Font font, int slot, int x, int y) {
        String skillId = (slot == 1) ? ClientStatsHandler.getActiveSkillSlot1() : ClientStatsHandler.getActiveSkillSlot2();
        if (skillId == null || skillId.isEmpty()) {
            return; // Slot is empty, don't render anything
        }

        // Draw background box
        guiGraphics.fill(x, y, x + 22, y + 20, 0xDD0D0D0D);

        // Draw border (cyan for slot 1, magenta for slot 2)
        int borderCol = (slot == 1) ? 0xFF00FFCC : 0xFFFF00CC;
        guiGraphics.fill(x, y, x + 22, y + 1, borderCol);
        guiGraphics.fill(x, y + 19, x + 22, y + 20, borderCol);
        guiGraphics.fill(x, y, x + 1, y + 20, borderCol);
        guiGraphics.fill(x + 21, y, x + 22, y + 20, borderCol);

        // Resolve and render item icon
        String iconName = getSkillIcon(skillId);
        String[] parts = iconName.split(":");
        String namespace = parts.length > 1 ? parts[0] : "minecraft";
        String path = parts.length > 1 ? parts[1] : parts[0];
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(namespace, path));
        if (item != null) {
            guiGraphics.renderFakeItem(new net.minecraft.world.item.ItemStack(item), x + 3, y + 2);
        }

        // Render keybind indicator (small text on top-left of slot)
        String keyText = (slot == 1) ? "R" : "X";
        guiGraphics.drawString(font, keyText, x + 2, y + 2, 0xBBFFFFFF, true);

        // Render Cooldown overlay if active
        net.com.cardengine.cooldown.CooldownState cdState = ClientStatsHandler.getCooldowns().get(skillId);
        if (cdState != null && cdState.getRemainingTicks() > 0) {
            float ratio = (float) cdState.getRemainingTicks() / cdState.getMaxTicks();
            int cdHeight = (int) (ratio * 18);
            // Draw dark overlay wiping up
            guiGraphics.fill(x + 1, y + 1 + (18 - cdHeight), x + 21, y + 19, 0xAA000000);

            // Draw remaining seconds text
            double seconds = cdState.getRemainingTicks() / 20.0;
            String secText = String.format(java.util.Locale.US, "%.1f", seconds);
            int textWidth = font.width(secText);
            guiGraphics.drawString(font, secText, x + 11 - textWidth / 2, y + 6, 0xFFFF3333, true);
        }
    }

    private static String getSkillIcon(String skillId) {
        for (net.com.cardengine.CardDefinition card : net.com.cardengine.CardManager.CARDS.values()) {
            for (net.com.cardengine.EffectDefinition effect : card.getEffects()) {
                if (skillId.equals(effect.getType())) {
                    return card.getIcon();
                }
            }
        }
        return "minecraft:paper";
    }
}
