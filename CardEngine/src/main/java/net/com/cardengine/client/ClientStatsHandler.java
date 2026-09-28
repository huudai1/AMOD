package net.com.cardengine.client;

import net.com.cardengine.CardDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import java.util.List;
import java.util.Map;

public class ClientStatsHandler {
    private static int level = 1;
    private static double experience = 0.0;
    private static CompoundTag stats = new CompoundTag();
    private static int pendingChoicesCount = 0;
    private static int rollCount = 0;

    private static String activeSkillSlot1 = "";
    private static String activeSkillSlot2 = "";
    private static String passiveSkillSlot = "";
    private static final Map<String, net.com.cardengine.cooldown.CooldownState> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, Integer> chosenCards = new java.util.concurrent.ConcurrentHashMap<>();

    // Casting state fields
    private static boolean isCasting = false;
    private static String castingSkillId = "";
    private static int castingSlot = 0;
    private static int castingTotalTicks = 0;
    private static int castingRemainingTicks = 0;
    private static boolean castingInterrupted = false;

    public static void updateStats(int lvl, double exp, CompoundTag tag, int choicesCount, int rolls) {
        level = lvl;
        experience = exp;
        stats = tag;
        pendingChoicesCount = choicesCount;
        rollCount = rolls;

        if (tag != null) {
            activeSkillSlot1 = tag.getString("active_skill_slot_1");
            activeSkillSlot2 = tag.getString("active_skill_slot_2");
            passiveSkillSlot = tag.contains("passive_skill_slot") ? tag.getString("passive_skill_slot") : "";

            cooldowns.clear();
            if (tag.contains("cooldowns", 10)) {
                CompoundTag cdTag = tag.getCompound("cooldowns");
                for (String key : cdTag.getAllKeys()) {
                    CompoundTag stateTag = cdTag.getCompound(key);
                    int remaining = stateTag.getInt("remaining");
                    int max = stateTag.getInt("max");
                    cooldowns.put(key, new net.com.cardengine.cooldown.CooldownState(remaining, max));
                }
            }

            chosenCards.clear();
            if (tag.contains("chosen_cards", 10)) {
                CompoundTag chosenTag = tag.getCompound("chosen_cards");
                for (String key : chosenTag.getAllKeys()) {
                    chosenCards.put(key, chosenTag.getInt(key));
                }
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CardSelectionScreen selectionScreen) {
            selectionScreen.refreshButtons();
        }
    }

    public static void decrementRollCount() {
        if (rollCount > 0) {
            rollCount--;
        }
    }

    public static int getLevel() {
        return level;
    }

    public static int getRollCount() {
        return rollCount;
    }

    public static double getExperience() {
        return experience;
    }

    public static int getPendingChoicesCount() {
        return pendingChoicesCount;
    }

    public static CompoundTag getStats() {
        return stats;
    }

    public static double getXpNeeded() {
        double needed = 50.0 + (level - 1) * 10.0;
        return Math.min(500.0, needed);
    }

    public static String getActiveSkillSlot1() {
        return activeSkillSlot1;
    }

    public static String getActiveSkillSlot2() {
        return activeSkillSlot2;
    }

    public static String getPassiveSkillSlot() {
        return passiveSkillSlot;
    }

    public static Map<String, net.com.cardengine.cooldown.CooldownState> getCooldowns() {
        return cooldowns;
    }

    public static Map<String, Integer> getChosenCards() {
        return chosenCards;
    }

    public static void updateCastingState(boolean casting, String skillId, int slot, int totalTicks, int remainingTicks, boolean interrupted) {
        isCasting = casting;
        castingSkillId = skillId != null ? skillId : "";
        castingSlot = slot;
        castingTotalTicks = totalTicks;
        castingRemainingTicks = remainingTicks;
        castingInterrupted = interrupted;
    }

    public static boolean isCasting() {
        return isCasting && castingRemainingTicks > 0;
    }

    public static String getCastingSkillId() {
        return castingSkillId;
    }

    public static int getCastingSlot() {
        return castingSlot;
    }

    public static int getCastingTotalTicks() {
        return castingTotalTicks;
    }

    public static int getCastingRemainingTicks() {
        return castingRemainingTicks;
    }

    public static boolean isCastingInterrupted() {
        return castingInterrupted;
    }

    public static float getCastingProgress() {
        if (castingTotalTicks <= 0) return 1.0F;
        return 1.0F - ((float) castingRemainingTicks / (float) castingTotalTicks);
    }

    public static void tickClientCooldown() {
        if (isCasting && castingRemainingTicks > 0) {
            castingRemainingTicks--;
            if (castingRemainingTicks <= 0) {
                isCasting = false;
            }
        }
        if (cooldowns.isEmpty()) return;
        List<String> toRemove = new java.util.ArrayList<>();
        for (Map.Entry<String, net.com.cardengine.cooldown.CooldownState> entry : cooldowns.entrySet()) {
            net.com.cardengine.cooldown.CooldownState state = entry.getValue();
            state.tick();
            if (state.isExpired()) {
                toRemove.add(entry.getKey());
            }
        }
        for (String key : toRemove) {
            cooldowns.remove(key);
        }
    }

    public static void openSelectionScreen(List<CardDefinition> cards) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CardSelectionScreen selectionScreen) {
            selectionScreen.updateCards(cards);
        } else {
            mc.setScreen(new CardSelectionScreen(cards));
        }

        boolean highRarity = false;
        for (CardDefinition card : cards) {
            net.com.cardengine.CardRarity rarity = card.getRarityEnum();
            if (rarity == net.com.cardengine.CardRarity.EPIC ||
                rarity == net.com.cardengine.CardRarity.LEGENDARY ||
                rarity == net.com.cardengine.CardRarity.MYTHIC ||
                rarity == net.com.cardengine.CardRarity.SPECIAL) {
                highRarity = true;
                break;
            }
        }

        if (highRarity) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }
}
