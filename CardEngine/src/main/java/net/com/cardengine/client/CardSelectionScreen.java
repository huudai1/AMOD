package net.com.cardengine.client;

import net.com.cardengine.CardDefinition;
import net.com.cardengine.CardRarity;
import net.com.cardengine.network.ModPackets;
import net.com.cardengine.network.ServerboundSelectCardPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.gui.components.Button;
import net.com.cardengine.network.ServerboundRerollCardsPacket;
import java.util.ArrayList;
import java.util.List;

public class CardSelectionScreen extends Screen {
    private final List<CardDefinition> cards;
    private int hoveredIndex = -1;
    private int lastHoveredIndex = -1;
    private final boolean[] slotRolled = new boolean[3];
    private long openTime = -1;
    private CardDefinition pendingCard = null;
    private String pendingSkillId = null;
    private CardDefinition pendingPassiveCard = null; // passive slot replacement

    public CardSelectionScreen(List<CardDefinition> cards) {
        super(Component.literal("Card Selection"));
        this.cards = new ArrayList<>(cards);
    }

    public void updateCards(List<CardDefinition> newCards) {
        this.clearWidgets();
        this.cards.clear();
        this.cards.addAll(newCards);
        this.init();
        this.hoveredIndex = -1;
        this.lastHoveredIndex = -1;
    }

    public void refreshButtons() {
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        if (this.openTime == -1) {
            this.openTime = System.currentTimeMillis();
        }

        // ── Passive slot replacement confirmation ────────────────────────────
        if (pendingPassiveCard != null) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            String currentName = formatSkillName(ClientStatsHandler.getPassiveSkillSlot());
            String newName = formatCardName(pendingPassiveCard.getId());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Confirm Replace: " + newName), button -> {
                        ModPackets.sendToServer(new ServerboundSelectCardPacket(pendingPassiveCard.getId(), 3));
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.GLASS_BREAK, 1.0F));
                        this.onClose();
                    }).bounds(centerX - 130, centerY - 12, 260, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
                pendingPassiveCard = null;
                this.refreshButtons();
            }).bounds(centerX - 50, centerY + 18, 100, 20).build());

            return;
        }

        // ── Active slot replacement confirmation ─────────────────────────────
        if (pendingCard != null) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            String s1 = ClientStatsHandler.getActiveSkillSlot1();
            String s2 = ClientStatsHandler.getActiveSkillSlot2();

            String s1Name = formatSkillName(s1);
            String s2Name = formatSkillName(s2);

            this.addRenderableWidget(Button.builder(Component.literal("Replace Slot 1: " + s1Name), button -> {
                ModPackets.sendToServer(new ServerboundSelectCardPacket(pendingCard.getId(), 1));
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.GLASS_BREAK, 1.0F));
                this.onClose();
            }).bounds(centerX - 120, centerY - 30, 240, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Replace Slot 2: " + s2Name), button -> {
                ModPackets.sendToServer(new ServerboundSelectCardPacket(pendingCard.getId(), 2));
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.GLASS_BREAK, 1.0F));
                this.onClose();
            }).bounds(centerX - 120, centerY, 240, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
                pendingCard = null;
                pendingSkillId = null;
                this.refreshButtons();
            }).bounds(centerX - 50, centerY + 40, 100, 20).build());

            return;
        }

        int cardWidth = 110;
        int cardHeight = 160;
        int padding = 24;
        int totalWidth = cards.size() * cardWidth + (cards.size() - 1) * padding;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - cardHeight) / 2 + 10;

        // Add Reroll Button under each card slot (only 1 reroll allowed per slot)
        int rollCount = ClientStatsHandler.getRollCount();
        for (int i = 0; i < cards.size(); i++) {
            int cardX = startX + i * (cardWidth + padding);
            int index = i;
            Button rollBtn = Button.builder(Component.literal("Roll"), button -> {
                ModPackets.sendToServer(new ServerboundRerollCardsPacket(index));
                if (index >= 0 && index < slotRolled.length) {
                    slotRolled[index] = true;
                }
                ClientStatsHandler.decrementRollCount();
                button.active = false;
                this.refreshButtons();
            }).bounds(cardX, startY + cardHeight + 8, cardWidth, 20).build();

            boolean hasAlreadyRolled = index >= 0 && index < slotRolled.length && slotRolled[index];
            rollBtn.active = rollCount > 0 && !hasAlreadyRolled;
            this.addRenderableWidget(rollBtn);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.TOGGLE_SELECTION_KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        if (this.minecraft.level != null) {
            long elapsed = System.currentTimeMillis() - this.openTime;
            float fadeFactor = Math.min(1.0f, elapsed / 300.0f);
            int alpha = (int) (160 * fadeFactor); // Max 160 alpha
            guiGraphics.fill(0, 0, this.width, this.height, (alpha << 24) | 0x0A0A0A);
        } else {
            super.renderBackground(guiGraphics);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw dark blurred/semi-transparent background
        this.renderBackground(guiGraphics);

        long elapsed = System.currentTimeMillis() - this.openTime;
        float fadeFactor = Math.min(1.0f, elapsed / 300.0f);

        // Apply shader color alpha for fading in textures/text
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fadeFactor);

        if (pendingPassiveCard != null) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            String currentName = formatSkillName(ClientStatsHandler.getPassiveSkillSlot());
            String newName = formatCardName(pendingPassiveCard.getId());

            guiGraphics.drawCenteredString(font, "§e§lPASSIVE SLOT OCCUPIED", centerX, centerY - 80, 0xFFAA00);
            guiGraphics.drawCenteredString(font,
                    "Replace  §c" + currentName + "§f  with  §a" + newName + "§f?",
                    centerX, centerY - 60, 0xFFFFFF);
            guiGraphics.drawCenteredString(font,
                    "§7Your current passive will be unequipped.",
                    centerX, centerY - 48, 0x888888);

            super.render(guiGraphics, mouseX, mouseY, partialTick);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        if (pendingCard != null) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            guiGraphics.drawCenteredString(font, "§c§lACTIVE SKILL SLOTS FULL", centerX, centerY - 80, 0xFF5555);
            
            String newSkillName = formatCardName(pendingCard.getId());
            guiGraphics.drawCenteredString(font, "Choose which slot to replace with §a" + newSkillName + "§f:", centerX, centerY - 60, 0xFFFFFF);

            super.render(guiGraphics, mouseX, mouseY, partialTick);
            
            // Reset shader color
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        // Draw title
        guiGraphics.drawCenteredString(font, "§6§lSELECT AN UPGRADE CARD", this.width / 2, 20, 0xFFFFFF);

        int cardWidth = 110;
        int cardHeight = 160;
        int padding = 24;
        int totalWidth = cards.size() * cardWidth + (cards.size() - 1) * padding;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - cardHeight) / 2 + 10;

        hoveredIndex = -1;

        for (int i = 0; i < cards.size(); i++) {
            CardDefinition card = cards.get(i);
            int cardX = startX + i * (cardWidth + padding);
            int cardY = startY;

            boolean isHovered = mouseX >= cardX && mouseX <= cardX + cardWidth &&
                    mouseY >= cardY && mouseY <= cardY + cardHeight;

            if (isHovered) {
                hoveredIndex = i;
            }

            // Draw card base & features
            drawCard(guiGraphics, card, cardX, cardY, cardWidth, cardHeight, isHovered, fadeFactor);
        }

        // Handle hover sound change
        if (hoveredIndex != lastHoveredIndex) {
            if (hoveredIndex != -1) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
            }
            lastHoveredIndex = hoveredIndex;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Reset shader color
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private int fadeColor(int color, float fadeFactor) {
        int alpha = (color >> 24) & 0xFF;
        if (alpha == 0)
            alpha = 255;
        int fadedAlpha = (int) (alpha * fadeFactor);
        return (fadedAlpha << 24) | (color & 0xFFFFFF);
    }

    private void drawCard(GuiGraphics guiGraphics, CardDefinition card, int x, int y, int width, int height,
            boolean hovered, float fadeFactor) {
        // Apply scaling effect if hovered
        int currentX = x;
        int currentY = y;
        int currentW = width;
        int currentH = height;

        if (hovered) {
            int scale = 4;
            currentX -= scale;
            currentY -= scale;
            currentW += scale * 2;
            currentH += scale * 2;
        }

        // Get rarity color
        int rarityColor = card.getRarityEnum().getColor();
        if (card.getBorderColor() != null) {
            try {
                rarityColor = Integer.parseInt(card.getBorderColor().replace("#", ""), 16);
            } catch (NumberFormatException ignored) {
            }
        }

        // Draw neon glow shadow if hovered
        if (hovered) {
            int glowColor = (0x44 << 24) | (rarityColor & 0xFFFFFF);
            guiGraphics.fill(currentX - 5, currentY - 5, currentX + currentW + 5, currentY + currentH + 5,
                    fadeColor(glowColor, fadeFactor));
        }

        // Draw card background
        if (card.getCustomFrame() != null) {
            // Render custom frame texture
            ResourceLocation frameTexture = new ResourceLocation(card.getCustomFrame());
            guiGraphics.blit(frameTexture, currentX, currentY, 0, 0, currentW, currentH, currentW, currentH);
        } else {
            // Draw default dark glassmorphic container
            int bgColor = 0xDD0D0D0D; // 86% opacity dark gray
            guiGraphics.fill(currentX, currentY, currentX + currentW, currentY + currentH,
                    fadeColor(bgColor, fadeFactor));

            // Draw border line using rarity color
            int borderCol = 0xFF000000 | rarityColor;
            guiGraphics.fill(currentX, currentY, currentX + currentW, currentY + 1, fadeColor(borderCol, fadeFactor)); // Top
            guiGraphics.fill(currentX, currentY + currentH - 1, currentX + currentW, currentY + currentH,
                    fadeColor(borderCol, fadeFactor)); // Bottom
            guiGraphics.fill(currentX, currentY, currentX + 1, currentY + currentH, fadeColor(borderCol, fadeFactor)); // Left
            guiGraphics.fill(currentX + currentW - 1, currentY, currentX + currentW, currentY + currentH,
                    fadeColor(borderCol, fadeFactor)); // Right
        }

        // Overlapping Icon Box layout centered on top border of card
        int iconBoxW = 28;
        int iconBoxH = 28;
        int iconBoxX = currentX + currentW / 2 - iconBoxW / 2;
        int iconBoxY = currentY - iconBoxH / 2;

        // Draw Icon Box background and border
        int boxBgColor = 0xFF151515;
        guiGraphics.fill(iconBoxX, iconBoxY, iconBoxX + iconBoxW, iconBoxY + iconBoxH,
                fadeColor(boxBgColor, fadeFactor));

        int boxBorderCol = 0xFF000000 | rarityColor;
        guiGraphics.fill(iconBoxX, iconBoxY, iconBoxX + iconBoxW, iconBoxY + 1, fadeColor(boxBorderCol, fadeFactor));
        guiGraphics.fill(iconBoxX, iconBoxY + iconBoxH - 1, iconBoxX + iconBoxW, iconBoxY + iconBoxH,
                fadeColor(boxBorderCol, fadeFactor));
        guiGraphics.fill(iconBoxX, iconBoxY, iconBoxX + 1, iconBoxY + iconBoxH, fadeColor(boxBorderCol, fadeFactor));
        guiGraphics.fill(iconBoxX + iconBoxW - 1, iconBoxY, iconBoxX + iconBoxW, iconBoxY + iconBoxH,
                fadeColor(boxBorderCol, fadeFactor));

        // Render Item Icon inside box
        ResourceLocation itemLoc = new ResourceLocation(card.getIcon());
        Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            // Center 16x16 item inside 28x28 box -> offset by 6
            guiGraphics.renderFakeItem(stack, iconBoxX + 6, iconBoxY + 6);
        }

        // Draw Card Level Stars matching the border/rarity color
        int targetLevel = ClientStatsHandler.getChosenCards().getOrDefault(card.getId(), 0) + 1;
        int cardLevel = Math.max(1, Math.min(5, targetLevel));
        StringBuilder stars = new StringBuilder();
        for (int s = 0; s < cardLevel; s++) {
            stars.append("★ ");
        }
        guiGraphics.drawCenteredString(font, stars.toString().trim(), currentX + currentW / 2, currentY + 12,
                rarityColor);

        // Draw Card Name
        String name = formatCardName(card.getId());
        guiGraphics.drawCenteredString(font, "§l" + name, currentX + currentW / 2, currentY + 24, 0xFFFFFF);

        // Draw Rarity Text
        String rarityName = card.getRarity().toUpperCase();
        guiGraphics.drawCenteredString(font, "§o" + rarityName, currentX + currentW / 2, currentY + 36, rarityColor);

        // Draw divider line
        guiGraphics.fill(currentX + 10, currentY + 46, currentX + currentW - 10, currentY + 47,
                fadeColor(0x33FFFFFF, fadeFactor));

        // Draw Description
        int descY = currentY + 54;
        int maxTextWidth = currentW - 16; // 8 pixels margin on left and right
        for (String line : card.getDescription()) {
            List<net.minecraft.util.FormattedCharSequence> wrappedLines = font.split(net.minecraft.network.chat.Component.literal(line), maxTextWidth);
            for (net.minecraft.util.FormattedCharSequence wrappedLine : wrappedLines) {
                guiGraphics.drawCenteredString(font, wrappedLine, currentX + currentW / 2, descY, 0xCCCCCC);
                descY += 10;
            }
            // Add a small extra gap between paragraphs if there are multiple lines in the original description
            descY += 2;
        }
    }

    private String formatCardName(String id) {
        String name = id;
        if (name.contains(":")) {
            name = name.split(":")[1];
        }
        name = name.replace("_", " ");
        
        // Strip rarity suffix
        String lower = name.toLowerCase(java.util.Locale.US);
        String[] rarities = {"special", "mythic", "legendary", "epic", "rare", "uncommon", "common"};
        for (String r : rarities) {
            if (lower.endsWith(" " + r)) {
                name = name.substring(0, name.length() - (r.length() + 1));
                lower = name.toLowerCase(java.util.Locale.US);
                break;
            }
        }

        // Strip trailing " card" word
        if (lower.endsWith(" card")) {
            name = name.substring(0, name.length() - 5);
        }
        
        // Capitalize words
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pendingCard != null || pendingPassiveCard != null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (hoveredIndex != -1) {
            CardDefinition card = cards.get(hoveredIndex);

            // Check if card contains an active skill effect
            String activeSkillId = null;
            if (card.getEffects() != null) {
                for (net.com.cardengine.EffectDefinition effect : card.getEffects()) {
                    if (net.com.cardengine.api.registries.CardRegistry.ACTIVE_SKILLS.containsKey(effect.getType())) {
                        activeSkillId = effect.getType();
                        break;
                    }
                }
            }

            if (activeSkillId != null) {
                String s1 = ClientStatsHandler.getActiveSkillSlot1();
                String s2 = ClientStatsHandler.getActiveSkillSlot2();

                // If both slots are full and the skill is not already equipped
                if (s1 != null && !s1.isEmpty() && s2 != null && !s2.isEmpty()
                        && !activeSkillId.equals(s1) && !activeSkillId.equals(s2)) {
                    pendingCard = card;
                    pendingSkillId = activeSkillId;
                    this.refreshButtons();
                    return true;
                }
            }

            // Check if card is a passive skill — only 1 passive allowed at a time
            if (isPassiveSkillCard(card)) {
                String currentPassive = ClientStatsHandler.getPassiveSkillSlot();
                if (currentPassive != null && !currentPassive.isEmpty()
                        && !currentPassive.equals(card.getId())) {
                    pendingPassiveCard = card;
                    this.refreshButtons();
                    return true;
                }
            }

            // Send selected card to server
            ModPackets.sendToServer(new ServerboundSelectCardPacket(card.getId()));

            // Play select sound (Glass Break)
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.GLASS_BREAK, 1.0F));

            // Close screen
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String formatSkillName(String skillId) {
        if (skillId == null || skillId.isEmpty()) return "Empty";
        for (CardDefinition card : net.com.cardengine.CardManager.CARDS.values()) {
            if (card.getEffects() != null) {
                for (net.com.cardengine.EffectDefinition effect : card.getEffects()) {
                    if (effect.getType().equals(skillId)) {
                        return formatCardName(card.getId());
                    }
                }
            }
        }
        return formatCardName(skillId);
    }

    private boolean isPassiveSkillCard(CardDefinition card) {
        return net.com.cardengine.CardManager.isPassiveSkillCard(card);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
