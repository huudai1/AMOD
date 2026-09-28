package net.com.cardengine.client;

import net.com.cardengine.CardDefinition;
import net.com.cardengine.CardManager;
import net.com.cardengine.api.registries.CardRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CardStatsScreen extends Screen {
    private double scrollAmount = 0;
    private int currentTab = 0; // 0 = Stats, 1 = Upgrades
    private Button btnLeft;
    private Button btnRight;

    public CardStatsScreen() {
        super(Component.literal("Card Upgrade Stats"));
    }

    @Override
    protected void init() {
        super.init();

        // Close / Back Button at the bottom
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            this.onClose();
        }).bounds(this.width / 2 - 50, this.height - 35, 100, 20).build());

        // Left Arrow Button
        this.btnLeft = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            this.currentTab = (this.currentTab == 0) ? 1 : 0;
            this.scrollAmount = 0; // Reset scroll on tab switch
        }).bounds(this.width / 2 - 85, 68, 20, 20).build());

        // Right Arrow Button
        this.btnRight = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            this.currentTab = (this.currentTab == 0) ? 1 : 0;
            this.scrollAmount = 0; // Reset scroll on tab switch
        }).bounds(this.width / 2 + 65, 68, 20, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.currentTab == 0) {
            int listY = 96;
            int rowHeight = 22;
            int visibleHeight = (this.height - 45) - listY;
            List<String> keys = new ArrayList<>(CardRegistry.REGISTERED_STATS);
            int totalRows = (int) Math.ceil(keys.size() / 2.0);
            int maxScroll = Math.max(0, totalRows * rowHeight - visibleHeight);
            this.scrollAmount = Math.max(0, Math.min(maxScroll, this.scrollAmount - delta * 12));
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;

        // Draw title
        guiGraphics.drawCenteredString(font, "§6§l=== CARD UPGRADE STATS ===", centerX, 18, 0xFFFFFF);

        // Retrieve capability details from cache
        int level = ClientStatsHandler.getLevel();
        double xp = ClientStatsHandler.getExperience();
        double xpNeeded = ClientStatsHandler.getXpNeeded();
        int rollCount = ClientStatsHandler.getRollCount();

        // Draw card level & rolls
        guiGraphics.drawCenteredString(font, "Level: §b" + level + "  §f|  Remaining Rolls: §e" + rollCount, centerX,
                38, 0xFFFFFF);

        // Draw XP Progress Bar
        int barW = 200;
        int barH = 6;
        int barX = centerX - barW / 2;
        int barY = 48;

        // Draw background
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0x88000000);
        // Draw fill (cyan)
        double progress = xpNeeded > 0 ? xp / xpNeeded : 0.0;
        int fillW = (int) (barW * Math.min(1.0, progress));
        guiGraphics.fill(barX, barY, barX + fillW, barY + barH, 0xFF00E5FF);
        // Draw border
        int borderCol = 0x55FFFFFF;
        guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY, borderCol); // Top
        guiGraphics.fill(barX - 1, barY + barH, barX + barW + 1, barY + barH + 1, borderCol); // Bottom
        guiGraphics.fill(barX - 1, barY, barX, barY + barH, borderCol); // Left
        guiGraphics.fill(barX + barW, barY, barX + barW + 1, barY + barH, borderCol); // Right

        // XP text indicator
        String xpText = String.format("%.0f / %.0f XP", xp, xpNeeded);
        guiGraphics.drawCenteredString(font, "§7" + xpText, centerX, 56, 0xCCCCCC);

        // Draw Tab Title in the middle of arrows
        String tabTitle = (this.currentTab == 0) ? "§e§l<<< STATS >>>" : "§b§l<<< SKILLS >>>";
        guiGraphics.drawCenteredString(font, tabTitle, centerX, 74, 0xFFFFFF);

        int listY = 96;
        int rowHeight = 22;

        int visibleHeight = 0;
        int totalRows = 0;
        int maxScroll = 0;

        if (this.currentTab == 0) {
            List<String> keys = new ArrayList<>(CardRegistry.REGISTERED_STATS);
            visibleHeight = (this.height - 45) - listY;
            totalRows = (int) Math.ceil(keys.size() / 2.0);
            maxScroll = Math.max(0, totalRows * rowHeight - visibleHeight);
        }

        this.scrollAmount = Math.max(0, Math.min(maxScroll, this.scrollAmount));

        String hoveredStatKey = null;
        String hoveredCardId = null;

        if (this.currentTab == 0) {
            // Draw horizontal line divider
            guiGraphics.fill(centerX - 220, listY - 5, centerX + 220, listY - 4, 0x33FFFFFF);

            // Enable scissor box to clip stats outside scroll viewport
            guiGraphics.enableScissor(centerX - 220, listY, centerX + 220, this.height - 45);

            List<String> keys = new ArrayList<>(CardRegistry.REGISTERED_STATS);
            CompoundTag activeStats = ClientStatsHandler.getStats();

            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                int col = i % 2;
                int row = i / 2;

                int startX = (col == 0) ? (centerX - 210) : (centerX + 10);
                int statY = listY + row * rowHeight - (int) this.scrollAmount;

                // Draw stat icon box
                guiGraphics.fill(startX, statY, startX + 16, statY + 16, 0x22FFFFFF);
                ResourceLocation iconLoc = getStatIcon(key);
                Item iconItem = ForgeRegistries.ITEMS.getValue(iconLoc);
                if (iconItem != null) {
                    guiGraphics.renderFakeItem(new ItemStack(iconItem), startX, statY);
                }

                // Format key: "attribute_card_extension:max_health" -> "Max Health"
                String displayName = key.substring(key.indexOf(":") + 1).replace("_", " ");
                displayName = capitalize(displayName);
                guiGraphics.drawString(font, displayName, startX + 22, statY + 4, 0xFFFFFF);

                // Format value: check if player has this stat, format cleanly
                double val = activeStats.contains(key) ? activeStats.getDouble(key) : 0.0;
                String valStr = (val == (long) val) ? String.valueOf((long) val) : String.format("%.2f", val);
                guiGraphics.drawString(font, "§a" + valStr, startX + 140, statY + 4, 0x55FF55);

                // Draw micro row divider
                guiGraphics.fill(startX, statY + 18, startX + 195, statY + 19, 0x11FFFFFF);

                // Check hover for stat tooltip
                if (mouseX >= startX && mouseX <= startX + 195 && mouseY >= statY && mouseY <= statY + 18) {
                    if (mouseY >= listY && mouseY <= this.height - 45) {
                        hoveredStatKey = key;
                    }
                }
            }

            guiGraphics.disableScissor();
        } else {
            // Tab 1: Active Skills & Passive Skill Slot
            // 1. Render Active Skill Slots
            guiGraphics.drawCenteredString(font, "§6§l=== ACTIVE SKILLS ===", centerX, 100, 0xFFFFFF);

            String skill1 = ClientStatsHandler.getActiveSkillSlot1();
            String skill2 = ClientStatsHandler.getActiveSkillSlot2();

            int slotW = 120;
            int slotH = 32;

            // Draw Slot 1
            int s1X = centerX - 130;
            int sY = 116;
            guiGraphics.fill(s1X, sY, s1X + slotW, sY + slotH, 0x22FFFFFF);
            guiGraphics.fill(s1X, sY, s1X + slotW, sY + 1, 0x44FFFFFF);
            guiGraphics.fill(s1X, sY + slotH - 1, s1X + slotW, sY + slotH, 0x44FFFFFF);
            guiGraphics.fill(s1X, sY, s1X + 1, sY + slotH, 0x44FFFFFF);
            guiGraphics.fill(s1X + slotW - 1, sY, s1X + slotW, sY + slotH, 0x44FFFFFF);

            if (skill1 != null && !skill1.isEmpty()) {
                CardDefinition skillCard = getCardForEffect(skill1);
                ResourceLocation iconLoc = skillCard != null ? new ResourceLocation(skillCard.getIcon())
                        : getStatIcon(skill1);
                Item iconItem = ForgeRegistries.ITEMS.getValue(iconLoc);
                if (iconItem != null) {
                    guiGraphics.renderFakeItem(new ItemStack(iconItem), s1X + 8, sY + 8);
                }
                String name = skillCard != null ? formatCardName(skillCard.getId()) : formatCardName(skill1);
                if (name.length() > 11)
                    name = name.substring(0, 9) + "..";
                guiGraphics.drawString(font, "§f" + name, s1X + 30, sY + 6, 0xFFFFFF);
                guiGraphics.drawString(font, "§e[ phím R ]", s1X + 30, sY + 18, 0x55FF55);

                if (mouseX >= s1X && mouseX <= s1X + slotW && mouseY >= sY && mouseY <= sY + slotH) {
                    hoveredStatKey = skill1;
                }
            } else {
                guiGraphics.drawString(font, "§7Trống", s1X + 12, sY + 12, 0x888888);
                guiGraphics.drawString(font, "§e[ phím R ]", s1X + 55, sY + 12, 0x55FF55);
            }

            // Draw Slot 2
            int s2X = centerX + 10;
            guiGraphics.fill(s2X, sY, s2X + slotW, sY + slotH, 0x22FFFFFF);
            guiGraphics.fill(s2X, sY, s2X + slotW, sY + 1, 0x44FFFFFF);
            guiGraphics.fill(s2X, sY + slotH - 1, s2X + slotW, sY + slotH, 0x44FFFFFF);
            guiGraphics.fill(s2X, sY, s2X + 1, sY + slotH, 0x44FFFFFF);
            guiGraphics.fill(s2X + slotW - 1, sY, s2X + slotW, sY + slotH, 0x44FFFFFF);

            if (skill2 != null && !skill2.isEmpty()) {
                CardDefinition skillCard = getCardForEffect(skill2);
                ResourceLocation iconLoc = skillCard != null ? new ResourceLocation(skillCard.getIcon())
                        : getStatIcon(skill2);
                Item iconItem = ForgeRegistries.ITEMS.getValue(iconLoc);
                if (iconItem != null) {
                    guiGraphics.renderFakeItem(new ItemStack(iconItem), s2X + 8, sY + 8);
                }
                String name = skillCard != null ? formatCardName(skillCard.getId()) : formatCardName(skill2);
                if (name.length() > 11)
                    name = name.substring(0, 9) + "..";
                guiGraphics.drawString(font, "§f" + name, s2X + 30, sY + 6, 0xFFFFFF);
                guiGraphics.drawString(font, "§e[ phím X ]", s2X + 30, sY + 18, 0x55FF55);

                if (mouseX >= s2X && mouseX <= s2X + slotW && mouseY >= sY && mouseY <= sY + slotH) {
                    hoveredStatKey = skill2;
                }
            } else {
                guiGraphics.drawString(font, "§7Trống", s2X + 12, sY + 12, 0x888888);
                guiGraphics.drawString(font, "§e[ phím X ]", s2X + 55, sY + 12, 0x55FF55);
            }

            // Divider between Active and Passive
            guiGraphics.fill(centerX - 160, 168, centerX + 160, 169, 0x33FFFFFF);

            // 2. Passive Skill Slot
            guiGraphics.drawCenteredString(font, "§b§l=== PASSIVE SKILL ===", centerX, 185, 0xFFFFFF);

            String passiveSkill = ClientStatsHandler.getPassiveSkillSlot();
            int psW = 140;
            int psH = 32;
            int psX = centerX - psW / 2;
            int psY = 202;
            guiGraphics.fill(psX, psY, psX + psW, psY + psH, 0x22FFFFFF);
            guiGraphics.fill(psX, psY, psX + psW, psY + 1, 0x44FFFFFF);
            guiGraphics.fill(psX, psY + psH - 1, psX + psW, psY + psH, 0x44FFFFFF);
            guiGraphics.fill(psX, psY, psX + 1, psY + psH, 0x44FFFFFF);
            guiGraphics.fill(psX + psW - 1, psY, psX + psW, psY + psH, 0x44FFFFFF);

            if (passiveSkill != null && !passiveSkill.isEmpty()) {
                CardDefinition psCard = CardManager.CARDS.get(passiveSkill);
                if (psCard == null) psCard = getCardForEffect(passiveSkill);
                ResourceLocation psIconLoc = psCard != null ? new ResourceLocation(psCard.getIcon()) : new ResourceLocation("minecraft", "paper");
                Item psIconItem = ForgeRegistries.ITEMS.getValue(psIconLoc);
                if (psIconItem != null) guiGraphics.renderFakeItem(new ItemStack(psIconItem), psX + 8, psY + 8);
                String psName = psCard != null ? formatCardName(psCard.getId()) : formatCardName(passiveSkill);
                if (psName.length() > 12) psName = psName.substring(0, 10) + "..";
                guiGraphics.drawString(font, "§f" + psName, psX + 30, psY + 6, 0xFFFFFF);
                guiGraphics.drawString(font, "§7[ Passive Slot ]", psX + 30, psY + 18, 0xAAAAAA);
                if (mouseX >= psX && mouseX <= psX + psW && mouseY >= psY && mouseY <= psY + psH) {
                    hoveredStatKey = passiveSkill;
                }
            } else {
                guiGraphics.drawString(font, "§7Trống (Chưa gắn passive)", psX + 12, psY + 12, 0x888888);
            }
        }

        // Draw vertical scrollbar if content overflows in Tab 0
        if (this.currentTab == 0 && maxScroll > 0) {
            int scrollbarX = centerX + 215;
            int scrollbarW = 3;
            // Draw track
            guiGraphics.fill(scrollbarX, listY, scrollbarX + scrollbarW, this.height - 45, 0x33FFFFFF);

            // Draw thumb
            int thumbHeight = Math.max(10, (int) (((double) visibleHeight / (totalRows * rowHeight)) * visibleHeight));
            int thumbY = listY + (int) ((this.scrollAmount / maxScroll) * (visibleHeight - thumbHeight));
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarW, thumbY + thumbHeight, 0xFFFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render Tooltips
        if (hoveredStatKey != null) {
            guiGraphics.renderComponentTooltip(font, getStatTooltip(hoveredStatKey), mouseX, mouseY);
        } else if (hoveredCardId != null) {
            guiGraphics.renderComponentTooltip(font, getCardTooltip(hoveredCardId), mouseX, mouseY);
        }
    }

    private CardDefinition getCardForEffect(String effectType) {
        for (CardDefinition card : CardManager.CARDS.values()) {
            for (net.com.cardengine.EffectDefinition effect : card.getEffects()) {
                if (effect.getType().equals(effectType)) {
                    return card;
                }
            }
        }
        return null;
    }

    private List<Component> getStatTooltip(String statKey) {
        List<Component> tooltip = new ArrayList<>();
        String displayName = statKey.substring(statKey.indexOf(":") + 1).replace("_", " ");
        displayName = capitalize(displayName);
        tooltip.add(Component.literal("§6§l" + displayName));

        CardDefinition matchingCard = getCardForEffect(statKey);
        if (matchingCard != null && matchingCard.getDescription() != null && !matchingCard.getDescription().isEmpty()
                && !matchingCard.getDescription().get(0).equalsIgnoreCase("none")) {
            for (String line : matchingCard.getDescription()) {
                tooltip.add(Component.literal(line));
            }
        } else {
            tooltip.add(Component.literal("§7Không có mô tả cho chỉ số này."));
        }
        return tooltip;
    }


    private List<Component> getCardTooltip(String cardId) {
        List<Component> tooltip = new ArrayList<>();
        CardDefinition card = CardManager.CARDS.get(cardId);
        if (card != null) {
            String name = formatCardName(card.getId());
            int rarityColor = card.getRarityEnum().getColor();
            if (card.getBorderColor() != null) {
                try {
                    rarityColor = Integer.parseInt(card.getBorderColor().replace("#", ""), 16);
                } catch (NumberFormatException ignored) {
                }
            }
            final int finalColor = rarityColor;
            tooltip.add(Component.literal("§l" + name).withStyle(style -> style.withColor(finalColor)));
            tooltip.add(Component.literal("§o" + card.getRarity().toUpperCase())
                    .withStyle(style -> style.withColor(finalColor)));
            tooltip.add(Component.literal(""));
            if (card.getDescription() != null && !card.getDescription().isEmpty()
                    && !card.getDescription().get(0).equalsIgnoreCase("none")) {
                for (String line : card.getDescription()) {
                    tooltip.add(Component.literal(line));
                }
            } else {
                tooltip.add(Component.literal("§7Không có mô tả cho thẻ này."));
            }
        } else {
            tooltip.add(Component.literal("§6" + formatCardName(cardId)));
            tooltip.add(Component.literal("§7Không có mô tả."));
        }
        return tooltip;
    }

    private ResourceLocation getStatIcon(String key) {
        String cleanKey = key.toLowerCase(java.util.Locale.US);
        if (cleanKey.contains("health") || cleanKey.contains("hp")) {
            return new ResourceLocation("minecraft", "apple");
        } else if (cleanKey.contains("damage") || cleanKey.contains("power")) {
            return new ResourceLocation("minecraft", "iron_sword");
        } else if (cleanKey.contains("armor")) {
            return new ResourceLocation("minecraft", "iron_chestplate");
        } else if (cleanKey.contains("resist") || cleanKey.contains("shield")) {
            return new ResourceLocation("minecraft", "shield");
        } else if (cleanKey.contains("speed")) {
            return new ResourceLocation("minecraft", "feather");
        } else if (cleanKey.contains("crit")) {
            return new ResourceLocation("minecraft", "golden_sword");
        } else if (cleanKey.contains("steal") || cleanKey.contains("vamp")) {
            return new ResourceLocation("minecraft", "redstone");
        } else if (cleanKey.contains("regen")) {
            return new ResourceLocation("minecraft", "ghast_tear");
        } else if (cleanKey.contains("xp") || cleanKey.contains("experience")) {
            return new ResourceLocation("minecraft", "experience_bottle");
        } else if (cleanKey.contains("haste") || cleanKey.contains("shift") || cleanKey.contains("clock")) {
            return new ResourceLocation("minecraft", "clock");
        }
        return new ResourceLocation("minecraft", "paper");
    }

    private int getStatColor(String key) {
        int hash = key.hashCode();
        float hue = Math.abs(hash % 360) / 360.0f;
        return java.awt.Color.HSBtoRGB(hue, 0.75f, 0.85f) & 0xFFFFFF;
    }

    private String capitalize(String text) {
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatCardName(String id) {
        String name = id;
        if (name.contains(":")) {
            name = name.split(":")[1];
        }
        name = name.replace("_", " ");

        // Strip rarity suffix
        String lower = name.toLowerCase(java.util.Locale.US);
        String[] rarities = { "special", "mythic", "legendary", "epic", "rare", "uncommon", "common" };
        for (String r : rarities) {
            if (lower.endsWith(" " + r)) {
                name = name.substring(0, name.length() - (r.length() + 1));
                break;
            }
        }

        return capitalize(name);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.TOGGLE_STATS_KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
