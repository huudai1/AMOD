package net.com.cardengine.capability;

import net.com.cardengine.CardDefinition;
import net.com.cardengine.CardManager;
import net.com.cardengine.api.cleanup.CardResourceManager;
import net.com.cardengine.api.registries.CardRegistry;
import net.com.cardengine.api.registries.IActiveSkill;
import net.com.cardengine.api.registries.IPassiveSkill;
import net.com.cardengine.cooldown.CastingState;
import net.com.cardengine.network.ClientboundOpenSelectionScreenPacket;
import net.com.cardengine.network.ClientboundSyncCastingPacket;
import net.com.cardengine.network.ClientboundSyncStatsPacket;
import net.com.cardengine.network.ModPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerUpgradeData {
    private int level = 1;
    private double experience = 0.0;
    private int rollCount = 3; // Start with 3 rolls stack
    private final Map<String, Double> stats = new HashMap<>();
    private int pendingDraws = 0;
    private final Map<String, Integer> chosenCards = new HashMap<>();
    
    // We keep track of active choices offered to player to avoid selection cheats/exploits.
    private List<String> currentChoices = null;

    // Active Skills and Cooldowns
    private String activeSkillSlot1 = "";
    private String activeSkillSlot2 = "";
    private String passiveSkillSlot = ""; // Single passive skill slot
    private final Map<String, net.com.cardengine.cooldown.CooldownState> cooldowns = new HashMap<>();

    // Cast Time state (transient — not persisted to disk)
    private transient CastingState activeCasting = null;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }

    public int getRollCount() {
        return rollCount;
    }

    public void setRollCount(int rollCount) {
        this.rollCount = rollCount;
    }

    public Map<String, Double> getStats() {
        return stats;
    }

    public int getPendingDraws() {
        return pendingDraws;
    }

    public void setPendingDraws(int pendingDraws) {
        this.pendingDraws = pendingDraws;
    }

    public Map<String, Integer> getChosenCards() {
        return chosenCards;
    }

    public int getSkillLevel(String skillId) {
        if (skillId == null || skillId.isEmpty()) return 1;
        String cardId = net.com.cardengine.api.registries.CardRegistry.SKILL_TO_CARD.get(skillId);
        if (cardId != null) {
            return chosenCards.getOrDefault(cardId, 1);
        }
        for (Map.Entry<String, Integer> entry : chosenCards.entrySet()) {
            CardDefinition def = CardManager.CARDS.get(entry.getKey());
            if (def != null && def.getEffects() != null) {
                for (net.com.cardengine.EffectDefinition eff : def.getEffects()) {
                    if (skillId.equals(eff.getType())) {
                        return Math.max(1, entry.getValue());
                    }
                }
            }
        }
        return 1;
    }

    public double getXpNeeded() {
        double needed = 50.0 + (level - 1) * 10.0;
        return Math.min(500.0, needed);
    }

    public void addExperience(double amount, ServerPlayer player) {
        this.experience += amount;
        boolean leveledUp = false;
        while (this.experience >= getXpNeeded()) {
            this.experience -= getXpNeeded();
            this.level++;
            this.pendingDraws++; // Accumulate a pending draw
            leveledUp = true;
        }

        if (leveledUp) {
            CardEngineStorage.save(player, this);
            triggerPendingDraw(player);
        } else {
            CardEngineStorage.save(player, this);
            sync(player);
        }
    }

    public void triggerPendingDraw(ServerPlayer player) {
        if (this.currentChoices != null && !this.currentChoices.isEmpty()) {
            // Already rendering choices, do not interrupt
            return;
        }
        if (this.pendingDraws > 0) {
            List<CardDefinition> drawn = CardManager.drawCards(player, 3);
            if (!drawn.isEmpty()) {
                this.pendingDraws--;
                this.rollCount = 3; // Reset roll count to 3 for this draw selection
                this.currentChoices = drawn.stream().map(CardDefinition::getId).toList();

                CardEngineStorage.save(player, this);
                ModPackets.sendToPlayer(new ClientboundOpenSelectionScreenPacket(drawn), player);
                sync(player);
            } else {
                // No eligible cards to draw, clear pending draws
                this.pendingDraws = 0;
                CardEngineStorage.save(player, this);
                sync(player);
            }
        }
    }

    public List<String> getCurrentChoices() {
        return currentChoices;
    }

    public void setCurrentChoices(List<String> choices) {
        this.currentChoices = choices;
    }

    public String getActiveSkillSlot1() {
        return activeSkillSlot1;
    }

    public void setActiveSkillSlot1(String activeSkillSlot1) {
        this.activeSkillSlot1 = activeSkillSlot1;
    }

    public String getActiveSkillSlot2() {
        return activeSkillSlot2;
    }

    public void setActiveSkillSlot2(String activeSkillSlot2) {
        this.activeSkillSlot2 = activeSkillSlot2;
    }

    public String getPassiveSkillSlot() {
        return passiveSkillSlot;
    }

    public void setPassiveSkillSlot(String passiveSkillSlot) {
        this.passiveSkillSlot = (passiveSkillSlot != null) ? passiveSkillSlot : "";
    }

    /**
     * Equip a passive skill card into the single passive slot.
     * Fires onUnequip() for the old passive (if any), then onEquip() for the new one.
     * Returns the old passive card ID if one was replaced, or empty string.
     */
    public String equipPassiveSkill(String cardId, ServerPlayer player) {
        if (cardId == null || cardId.isEmpty()) return "";
        String old = this.passiveSkillSlot;
        // Fire lifecycle: unequip old
        if (!old.isEmpty() && !old.equals(cardId)) {
            IPassiveSkill oldSkill = CardRegistry.PASSIVE_SKILLS.get(old);
            if (oldSkill != null && player != null) {
                oldSkill.onUnequip(player);
            }
            if (player != null) {
                CardResourceManager.cleanupBySkill(player, old);
            }
        }
        this.passiveSkillSlot = cardId;
        // Fire lifecycle: equip new
        IPassiveSkill newSkill = CardRegistry.PASSIVE_SKILLS.get(cardId);
        if (newSkill != null && player != null) {
            int starLevel = chosenCards.getOrDefault(cardId, 1);
            newSkill.onEquip(player, starLevel);
        }
        return old;
    }

    /** Upgrade star level of the currently equipped passive and fire onUpgrade(). */
    public void upgradePassiveSkill(ServerPlayer player) {
        if (passiveSkillSlot.isEmpty()) return;
        int newLevel = chosenCards.merge(passiveSkillSlot, 1, Integer::sum);
        IPassiveSkill skill = CardRegistry.PASSIVE_SKILLS.get(passiveSkillSlot);
        if (skill != null && player != null) {
            skill.onUpgrade(player, newLevel);
        }
    }

    public void removePassiveSkill(ServerPlayer player) {
        if (!passiveSkillSlot.isEmpty()) {
            IPassiveSkill skill = CardRegistry.PASSIVE_SKILLS.get(passiveSkillSlot);
            if (skill != null && player != null) {
                skill.onUnequip(player);
            }
            if (player != null) {
                CardResourceManager.cleanupBySkill(player, passiveSkillSlot);
            }
            passiveSkillSlot = "";
        }
    }

    /** @deprecated Use removePassiveSkill(ServerPlayer) to trigger onUnequip lifecycle. */
    @Deprecated
    public void removePassiveSkill() {
        passiveSkillSlot = "";
    }

    /** @deprecated Use equipPassiveSkill(cardId, player) to trigger lifecycle callbacks. */
    @Deprecated
    public String equipPassiveSkill(String cardId) {
        if (cardId == null || cardId.isEmpty()) return "";
        String old = this.passiveSkillSlot;
        this.passiveSkillSlot = cardId;
        return old;
    }

    public Map<String, net.com.cardengine.cooldown.CooldownState> getCooldowns() {
        return cooldowns;
    }

    public void equipSkill(String skillId) {
        equipSkill(skillId, null);
    }

    public void equipSkill(String skillId, ServerPlayer player) {
        if (skillId == null || skillId.isEmpty()) return;
        if (skillId.equals(activeSkillSlot1) || skillId.equals(activeSkillSlot2)) {
            return;
        }
        if (activeSkillSlot1.isEmpty()) {
            equipSkillToSlot(skillId, 1, player);
        } else if (activeSkillSlot2.isEmpty()) {
            equipSkillToSlot(skillId, 2, player);
        } else {
            // Push slot 1 to slot 2, equip new into slot 1
            equipSkillToSlot(activeSkillSlot1, 2, player);
            equipSkillToSlot(skillId, 1, player);
        }
    }

    public void equipSkillToSlot(String skillId, int slot) {
        equipSkillToSlot(skillId, slot, null);
    }

    /**
     * Trang bị Active Skill vào Slot 1 hoặc Slot 2 có hỗ trợ vòng đời đầy đủ:
     * 1. Kích hoạt oldSkill.onUnequip() cho kỹ năng cũ.
     * 2. Tự động gọi CardResourceManager.cleanupBySkill() giải phóng tài nguyên skill cũ.
     * 3. Gán skill mới vào slot.
     * 4. Kích hoạt newSkill.onEquip() cho kỹ năng mới.
     */
    public void equipSkillToSlot(String skillId, int slot, ServerPlayer player) {
        if (skillId == null || skillId.isEmpty()) return;
        if (skillId.equals(activeSkillSlot1) || skillId.equals(activeSkillSlot2)) {
            return;
        }

        if (slot == 1) {
            String oldSkillId = activeSkillSlot1;
            if (!oldSkillId.isEmpty() && !oldSkillId.equals(skillId)) {
                IActiveSkill oldSkill = CardRegistry.ACTIVE_SKILLS.get(oldSkillId);
                if (oldSkill != null && player != null) {
                    oldSkill.onUnequip(player, 1);
                }
                if (player != null) {
                    CardResourceManager.cleanupBySkill(player, oldSkillId);
                }
            }
            activeSkillSlot1 = skillId;
            IActiveSkill newSkill = CardRegistry.ACTIVE_SKILLS.get(skillId);
            if (newSkill != null && player != null) {
                newSkill.onEquip(player, 1);
            }
        } else if (slot == 2) {
            String oldSkillId = activeSkillSlot2;
            if (!oldSkillId.isEmpty() && !oldSkillId.equals(skillId)) {
                IActiveSkill oldSkill = CardRegistry.ACTIVE_SKILLS.get(oldSkillId);
                if (oldSkill != null && player != null) {
                    oldSkill.onUnequip(player, 2);
                }
                if (player != null) {
                    CardResourceManager.cleanupBySkill(player, oldSkillId);
                }
            }
            activeSkillSlot2 = skillId;
            IActiveSkill newSkill = CardRegistry.ACTIVE_SKILLS.get(skillId);
            if (newSkill != null && player != null) {
                newSkill.onEquip(player, 2);
            }
        } else {
            equipSkill(skillId, player);
        }
    }

    /**
     * Tháo gỡ Active Skill ra khỏi Slot (1 hoặc 2) kèm theo kích hoạt onUnequip và thu hồi tài nguyên.
     */
    public void unequipSkillSlot(int slot, ServerPlayer player) {
        if (slot == 1 && !activeSkillSlot1.isEmpty()) {
            String oldSkillId = activeSkillSlot1;
            activeSkillSlot1 = "";
            IActiveSkill oldSkill = CardRegistry.ACTIVE_SKILLS.get(oldSkillId);
            if (oldSkill != null && player != null) {
                oldSkill.onUnequip(player, 1);
            }
            if (player != null) {
                CardResourceManager.cleanupBySkill(player, oldSkillId);
            }
        } else if (slot == 2 && !activeSkillSlot2.isEmpty()) {
            String oldSkillId = activeSkillSlot2;
            activeSkillSlot2 = "";
            IActiveSkill oldSkill = CardRegistry.ACTIVE_SKILLS.get(oldSkillId);
            if (oldSkill != null && player != null) {
                oldSkill.onUnequip(player, 2);
            }
            if (player != null) {
                CardResourceManager.cleanupBySkill(player, oldSkillId);
            }
        }
    }

    public void tickCooldowns(ServerPlayer player) {
        // Tick passive onTick lifecycle every 10 ticks
        if (!passiveSkillSlot.isEmpty() && player.tickCount % 10 == 0) {
            IPassiveSkill passiveSkill = CardRegistry.PASSIVE_SKILLS.get(passiveSkillSlot);
            if (passiveSkill != null) {
                int starLevel = chosenCards.getOrDefault(passiveSkillSlot, 1);
                passiveSkill.onTick(player, starLevel);
            }
        }

        // Tick cast state
        if (activeCasting != null) {
            activeCasting.tick();
            ModPackets.sendToPlayer(new ClientboundSyncCastingPacket(
                    true, activeCasting.getSkillId(), activeCasting.getSlot(),
                    activeCasting.getTotalTicks(), activeCasting.getRemainingTicks(), false), player);
            if (activeCasting.isFinished()) {
                // Fire the skill trigger
                String skillId = activeCasting.getSkillId();
                net.com.cardengine.api.registries.IActiveSkill skill = CardRegistry.ACTIVE_SKILLS.get(skillId);
                if (skill != null) {
                    try { skill.trigger(player); } catch (Exception e) {
                        com.mojang.logging.LogUtils.getLogger().error("Error triggering cast skill: {}", skillId, e);
                    }
                }
                // Start cooldown
                int baseCooldown = CardRegistry.BASE_COOLDOWNS.getOrDefault(skillId, 0);
                int skillLevel = getSkillLevel(skillId);
                double levelScale = net.com.cardengine.network.ServerboundSelectCardPacket.getDownScaleMultiplier(skillLevel);
                double cdr = stats.getOrDefault("card_engine:cooldown_reduction", 0.0);
                double cdrMult = 1.0 - Math.min(0.9, Math.max(0.0, cdr));
                int realCooldown = (int) Math.max(1, Math.round(baseCooldown * levelScale * cdrMult));
                if (!cooldowns.containsKey(skillId)) {
                    cooldowns.put(skillId, new net.com.cardengine.cooldown.CooldownState(realCooldown));
                }
                activeCasting = null;
                // Notify client: cast ended
                ModPackets.sendToPlayer(new ClientboundSyncCastingPacket(false, "", 0, 0, 0, false), player);
            }
        }

        if (cooldowns.isEmpty()) {
            if (player.tickCount % 20 == 0) sync(player);
            return;
        }
        List<String> toRemove = new ArrayList<>();
        boolean changed = false;
        for (Map.Entry<String, net.com.cardengine.cooldown.CooldownState> entry : cooldowns.entrySet()) {
            net.com.cardengine.cooldown.CooldownState state = entry.getValue();
            state.tick();
            if (state.isExpired()) {
                toRemove.add(entry.getKey());
                changed = true;
            }
        }
        for (String key : toRemove) {
            cooldowns.remove(key);
        }
        if (changed || player.tickCount % 20 == 0) {
            sync(player);
        }
    }

    /** Start a cast for the given skill id and slot. Returns false if already casting. */
    public boolean startCasting(String skillId, int slot, int castTicks, ServerPlayer player) {
        if (activeCasting != null) return false;
        activeCasting = new CastingState(skillId, slot, castTicks);
        ModPackets.sendToPlayer(new ClientboundSyncCastingPacket(
                true, skillId, slot, castTicks, castTicks, false), player);
        return true;
    }

    /** Interrupt current casting (e.g. player took damage). */
    public void interruptCasting(ServerPlayer player) {
        if (activeCasting == null) return;
        String skillId = activeCasting.getSkillId();
        int slot = activeCasting.getSlot();
        activeCasting = null;
        ModPackets.sendToPlayer(new ClientboundSyncCastingPacket(false, skillId, slot, 0, 0, true), player);
    }

    public boolean isCasting() {
        return activeCasting != null && !activeCasting.isFinished();
    }

    public void sync(ServerPlayer player) {
        ModPackets.sendToPlayer(new ClientboundSyncStatsPacket(player), player);
    }

    public void copyFrom(PlayerUpgradeData source) {
        this.level = source.level;
        this.experience = source.experience;
        this.rollCount = source.rollCount;
        this.pendingDraws = source.pendingDraws;
        this.currentChoices = source.currentChoices != null ? new ArrayList<>(source.currentChoices) : null;
        this.stats.clear();
        this.stats.putAll(source.stats);
        this.chosenCards.clear();
        this.chosenCards.putAll(source.chosenCards);
        this.activeSkillSlot1 = source.activeSkillSlot1;
        this.activeSkillSlot2 = source.activeSkillSlot2;
        this.passiveSkillSlot = source.passiveSkillSlot;
        this.cooldowns.clear();
        for (Map.Entry<String, net.com.cardengine.cooldown.CooldownState> entry : source.cooldowns.entrySet()) {
            net.com.cardengine.cooldown.CooldownState state = entry.getValue();
            this.cooldowns.put(entry.getKey(), new net.com.cardengine.cooldown.CooldownState(state.getRemainingTicks(), state.getMaxTicks()));
        }
        // activeCasting is transient — do not copy
        this.activeCasting = null;
    }

    public void saveNBTData(CompoundTag tag) {
        tag.putInt("card_engine:level", level);
        tag.putDouble("card_engine:experience", experience);
        tag.putInt("card_engine:roll_count", rollCount);
        tag.putInt("card_engine:pending_draws", pendingDraws);
        if (currentChoices != null) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (String id : currentChoices) {
                list.add(net.minecraft.nbt.StringTag.valueOf(id));
            }
            tag.put("card_engine:current_choices", list);
        }
        CompoundTag statsTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            statsTag.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put("card_engine:stats", statsTag);

        CompoundTag chosenTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : chosenCards.entrySet()) {
            chosenTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("card_engine:chosen_cards", chosenTag);

        tag.putString("card_engine:active_skill_slot_1", activeSkillSlot1);
        tag.putString("card_engine:active_skill_slot_2", activeSkillSlot2);
        tag.putString("card_engine:passive_skill_slot", passiveSkillSlot);
        
        CompoundTag cooldownsTag = new CompoundTag();
        for (Map.Entry<String, net.com.cardengine.cooldown.CooldownState> entry : cooldowns.entrySet()) {
            net.com.cardengine.cooldown.CooldownState state = entry.getValue();
            CompoundTag stateTag = new CompoundTag();
            stateTag.putInt("remaining", state.getRemainingTicks());
            stateTag.putInt("max", state.getMaxTicks());
            cooldownsTag.put(entry.getKey(), stateTag);
        }
        tag.put("card_engine:cooldowns", cooldownsTag);
    }

    public void loadNBTData(CompoundTag tag) {
        if (tag.contains("card_engine:level")) {
            this.level = tag.getInt("card_engine:level");
        } else {
            this.level = 1;
        }
        if (tag.contains("card_engine:experience")) {
            this.experience = tag.getDouble("card_engine:experience");
        } else {
            this.experience = 0.0;
        }
        if (tag.contains("card_engine:roll_count")) {
            this.rollCount = tag.getInt("card_engine:roll_count");
        } else {
            this.rollCount = 3;
        }
        if (tag.contains("card_engine:pending_draws")) {
            this.pendingDraws = tag.getInt("card_engine:pending_draws");
        } else {
            this.pendingDraws = 0;
        }
        if (tag.contains("card_engine:current_choices", 9)) {
            net.minecraft.nbt.ListTag list = tag.getList("card_engine:current_choices", 8);
            this.currentChoices = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                this.currentChoices.add(list.getString(i));
            }
        } else {
            this.currentChoices = null;
        }
        this.stats.clear();
        if (tag.contains("card_engine:stats", 10)) {
            CompoundTag statsTag = tag.getCompound("card_engine:stats");
            for (String key : statsTag.getAllKeys()) {
                this.stats.put(key, statsTag.getDouble(key));
            }
        }
        this.chosenCards.clear();
        if (tag.contains("card_engine:chosen_cards", 10)) {
            CompoundTag chosenTag = tag.getCompound("card_engine:chosen_cards");
            for (String key : chosenTag.getAllKeys()) {
                this.chosenCards.put(key, chosenTag.getInt(key));
            }
        }

        this.activeSkillSlot1 = tag.getString("card_engine:active_skill_slot_1");
        this.activeSkillSlot2 = tag.getString("card_engine:active_skill_slot_2");
        this.passiveSkillSlot = tag.contains("card_engine:passive_skill_slot")
                ? tag.getString("card_engine:passive_skill_slot") : "";

        this.cooldowns.clear();
        if (tag.contains("card_engine:cooldowns", 10)) {
            CompoundTag cooldownsTag = tag.getCompound("card_engine:cooldowns");
            for (String key : cooldownsTag.getAllKeys()) {
                CompoundTag stateTag = cooldownsTag.getCompound(key);
                int remaining = stateTag.getInt("remaining");
                int max = stateTag.getInt("max");
                this.cooldowns.put(key, new net.com.cardengine.cooldown.CooldownState(remaining, max));
            }
        }
    }
}
