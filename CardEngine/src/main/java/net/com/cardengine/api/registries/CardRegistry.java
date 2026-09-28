package net.com.cardengine.api.registries;

import net.com.cardengine.CardDefinition;
import net.minecraft.world.entity.ai.attributes.Attribute;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Map for lambda or simple instances of CardEngine
    public static final Map<String, CardEngine> CARD_ENGINES = Collections.synchronizedMap(new HashMap<>());

    // Active Skills Map
    public static final Map<String, IActiveSkill> ACTIVE_SKILLS = Collections.synchronizedMap(new HashMap<>());
    public static final Map<String, Integer> BASE_COOLDOWNS = Collections.synchronizedMap(new HashMap<>());
    public static final Map<String, Integer> BASE_CAST_TIMES = Collections.synchronizedMap(new HashMap<>());

    // Passive Skills Map
    public static final Map<String, IPassiveSkill> PASSIVE_SKILLS = Collections.synchronizedMap(new HashMap<>());

    // Skill to Card mappings
    public static final Map<String, String> SKILL_TO_CARD = Collections.synchronizedMap(new HashMap<>());
    public static final Map<String, String> CARD_TO_SKILL = Collections.synchronizedMap(new HashMap<>());

    // Map for class-based CardEngines (for @CardParam injection)
    public static final Map<String, Class<? extends CardEngine>> ENGINE_CLASSES = Collections
            .synchronizedMap(new HashMap<>());

    // Map for standard Minecraft Attributes
    public static final Map<String, Attribute> ATTRIBUTE_STATS = Collections.synchronizedMap(new HashMap<>());

    // List of registered display stats (shown on the L-screen)
    public static final List<String> REGISTERED_STATS = Collections.synchronizedList(new ArrayList<>());

    // List of default card templates to generate JSON files
    public static final List<CardDefinition> DEFAULT_CARDS = Collections.synchronizedList(new ArrayList<>());

    // --- STEP 1: Register Mechanism Logic (registerCard) ---

    // 1. Simple stat (does not need a lambda, automatically updates the player's
    // stat map)
    public static void registerCard(String mechName) {
        registerStat(mechName);
    }

    // 2. Minecraft Attribute-bound mechanism
    public static void registerCard(String mechName, Attribute attribute) {
        registerStat(mechName);
        ATTRIBUTE_STATS.put(mechName, attribute);
    }

    // 3. Custom lambda or action instance
    public static void registerCard(String mechName, CardEngine engine) {
        registerStat(mechName);
        CARD_ENGINES.put(mechName, engine);
    }

    // 4. Class-based mechanism with @CardParam variables
    public static void registerCard(String mechName, Class<? extends CardEngine> clazz) {
        registerStat(mechName);
        ENGINE_CLASSES.put(mechName, clazz);
    }

    // --- STEP 2: Register Card Template and Link to Pool (register) ---

    public static void register(String id, String mechanismName) {
        // 1. Automatically register the mechanism name as a display stat (Shown on
        // L-screen)
        registerStat(mechanismName);

        // 2. Check for duplicate card template registrations
        for (CardDefinition def : DEFAULT_CARDS) {
            if (def.getId().equals(id)) {
                LOGGER.warn("Duplicate card registration detected for ID: {}", id);
                return;
            }
        }

        // 3. Create default card definition template
        boolean isAction = CARD_ENGINES.containsKey(mechanismName) || ENGINE_CLASSES.containsKey(mechanismName);
        registerDefaultCardTemplate(id, mechanismName, "minecraft:paper", "common", 1.0, isAction);
    }

    // Legacy/Helper to manually register custom stats
    public static void registerStat(String key) {
        if (!REGISTERED_STATS.contains(key)) {
            REGISTERED_STATS.add(key);
        }
    }

    public static void registerActiveSkill(String id, int baseCooldownTicks, int castTimeTicks, IActiveSkill skill) {
        ACTIVE_SKILLS.put(id, skill);
        BASE_COOLDOWNS.put(id, baseCooldownTicks);
        BASE_CAST_TIMES.put(id, Math.max(0, castTimeTicks));
    }

    public static void registerPassiveSkill(String id, IPassiveSkill skill) {
        PASSIVE_SKILLS.put(id, skill);
    }

    public static void registerStatsCard(String id, String mechanismName) {
        registerStat(mechanismName);
        for (CardDefinition def : DEFAULT_CARDS) {
            if (def.getId().equals(id)) {
                LOGGER.warn("Duplicate card registration detected for ID: {}", id);
                return;
            }
        }
        registerDefaultCardTemplate(id, mechanismName, "minecraft:paper", "common", 1.0, false, "stat");
    }

    public static void registerASkillCard(String id, String mechanismName) {
        SKILL_TO_CARD.put(mechanismName, id);
        CARD_TO_SKILL.put(id, mechanismName);
        for (CardDefinition def : DEFAULT_CARDS) {
            if (def.getId().equals(id)) {
                LOGGER.warn("Duplicate card registration detected for ID: {}", id);
                return;
            }
        }
        boolean isAction = CARD_ENGINES.containsKey(mechanismName) || ENGINE_CLASSES.containsKey(mechanismName)
                || ACTIVE_SKILLS.containsKey(mechanismName);
        registerDefaultCardTemplate(id, mechanismName, "minecraft:paper", "common", 1.0, isAction, "active_skill");
    }

    public static void linkSkillAndCard(String skillId, String cardId) {
        SKILL_TO_CARD.put(skillId, cardId);
        CARD_TO_SKILL.put(cardId, skillId);
    }

    public static void registerPSkillCard(String id, String mechanismName) {
        SKILL_TO_CARD.put(mechanismName, id);
        CARD_TO_SKILL.put(id, mechanismName);
        for (CardDefinition def : DEFAULT_CARDS) {
            if (def.getId().equals(id)) {
                LOGGER.warn("Duplicate card registration detected for ID: {}", id);
                return;
            }
        }
        boolean isAction = CARD_ENGINES.containsKey(mechanismName) || ENGINE_CLASSES.containsKey(mechanismName);
        registerDefaultCardTemplate(id, mechanismName, "minecraft:paper", "common", 1.0, isAction, "passive_skill");
    }

    // Legacy helper to support older mods FML setup, if any
    public static void registerDefaultCard(CardDefinition card) {
        for (CardDefinition def : DEFAULT_CARDS) {
            if (def.getId().equals(card.getId())) {
                LOGGER.warn("Duplicate default card template registration detected for ID: {}", card.getId());
                return;
            }
        }
        DEFAULT_CARDS.add(card);
    }

    // --- Internal Helpers ---

    private static void registerDefaultCardTemplate(String id, String mechanismName, String defaultIcon,
            String defaultRarity, double defaultValue, boolean isAction) {
        registerDefaultCardTemplate(id, mechanismName, defaultIcon, defaultRarity, defaultValue, isAction, "stat");
    }

    private static void registerDefaultCardTemplate(String id, String mechanismName, String defaultIcon,
            String defaultRarity, double defaultValue, boolean isAction, String cardType) {
        List<String> description = new ArrayList<>();
        description.add("none");

        // Check if mechanism is class-based and has @CardParam variables to export
        Map<String, Double> defaultParams = new HashMap<>();
        Class<? extends CardEngine> clazz = ENGINE_CLASSES.get(mechanismName);
        if (clazz != null) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(CardParam.class)) {
                    CardParam param = field.getAnnotation(CardParam.class);
                    defaultParams.put(field.getName(), param.value());
                }
            }
        }

        net.com.cardengine.EffectDefinition effect = new net.com.cardengine.EffectDefinition();
        effect.setType(mechanismName);
        if (!defaultParams.isEmpty()) {
            effect.setParams(defaultParams);
        } else {
            effect.setValue(defaultValue);
        }

        CardDefinition defaultCard = new CardDefinition(
                id,
                defaultIcon,
                defaultRarity,
                "all",
                description,
                List.of(effect));
        defaultCard.setCardType(cardType);
        DEFAULT_CARDS.add(defaultCard);
    }

    private static String formatDisplayName(String key) {
        String path = key;
        int colonIdx = key.indexOf(':');
        if (colonIdx != -1) {
            path = key.substring(colonIdx + 1);
        }
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static String formatValue(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            if (Math.abs(value) < 1.0) {
                return String.format("%.0f%%", value * 100);
            }
            return String.format("%.2f", value);
        }
    }
}
