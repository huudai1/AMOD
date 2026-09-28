package net.com.cardengine.capability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardEngineStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ChosenCardDTO {
        public String id;
        public int level;
        public String type;

        public ChosenCardDTO() {}
        public ChosenCardDTO(String id, int level, String type) {
            this.id = id;
            this.level = level;
            this.type = type;
        }
    }

    public static String determineCardType(String cardId) {
        net.com.cardengine.CardDefinition card = net.com.cardengine.CardManager.CARDS.get(cardId);
        if (card == null) {
            return "unknown";
        }
        for (net.com.cardengine.EffectDefinition effect : card.getEffects()) {
            if (net.com.cardengine.api.registries.CardRegistry.ACTIVE_SKILLS.containsKey(effect.getType())) {
                return "active_skill";
            }
        }
        boolean isStatCard = true;
        for (net.com.cardengine.EffectDefinition effect : card.getEffects()) {
            if (!net.com.cardengine.api.registries.CardRegistry.REGISTERED_STATS.contains(effect.getType())) {
                isStatCard = false;
                break;
            }
        }
        if (isStatCard && !card.getEffects().isEmpty()) {
            return "stats";
        }
        return "passive_skill";
    }

    public static class PlayerDataDTO {
        public int level = 1;
        public double experience = 0.0;
        public int rollCount = 3;
        public List<String> currentChoices = new ArrayList<>();
        public Map<String, Double> stats = new HashMap<>();
        public int pendingDraws = 0;
        public JsonElement chosenCards;
        public String activeSkillSlot1 = "";
        public String activeSkillSlot2 = "";
        public String passiveSkillSlot = "";
        public Map<String, Integer> cooldownTicks = new HashMap<>();
        public Map<String, Integer> maxCooldownTicks = new HashMap<>();
        public String customNbt = ""; // Contains all player persistent NBT starting with card_engine:
    }

    public static void save(ServerPlayer player, PlayerUpgradeData data) {
        if (player.getServer() == null) return;
        try {
            Path root = player.getServer().getWorldPath(LevelResource.ROOT);
            Path dataDir = root.resolve("data").resolve("card_engine");
            File dir = dataDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = dataDir.resolve(player.getUUID().toString() + ".json").toFile();

            PlayerDataDTO dto = new PlayerDataDTO();
            dto.level = data.getLevel();
            dto.experience = data.getExperience();
            dto.rollCount = data.getRollCount();
            dto.currentChoices = data.getCurrentChoices() != null ? data.getCurrentChoices() : new ArrayList<>();
            dto.stats = data.getStats();
            dto.pendingDraws = data.getPendingDraws();
            
            JsonArray chosenArray = new JsonArray();
            for (Map.Entry<String, Integer> entry : data.getChosenCards().entrySet()) {
                String cardId = entry.getKey();
                int level = entry.getValue();
                String type = determineCardType(cardId);
                JsonObject cardObj = new JsonObject();
                cardObj.addProperty("id", cardId);
                cardObj.addProperty("level", level);
                cardObj.addProperty("type", type);
                chosenArray.add(cardObj);
            }
            dto.chosenCards = chosenArray;

            dto.activeSkillSlot1 = data.getActiveSkillSlot1();
            dto.activeSkillSlot2 = data.getActiveSkillSlot2();
            dto.passiveSkillSlot = data.getPassiveSkillSlot();
            dto.cooldownTicks = new HashMap<>();
            dto.maxCooldownTicks = new HashMap<>();
            for (Map.Entry<String, net.com.cardengine.cooldown.CooldownState> entry : data.getCooldowns().entrySet()) {
                dto.cooldownTicks.put(entry.getKey(), entry.getValue().getRemainingTicks());
                dto.maxCooldownTicks.put(entry.getKey(), entry.getValue().getMaxTicks());
            }

            // Gather all persistent player NBT keys starting with card_engine:
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag customTags = new CompoundTag();
            for (String key : persistentData.getAllKeys()) {
                if (key.startsWith("card_engine:")) {
                    customTags.put(key, persistentData.get(key).copy());
                }
            }
            dto.customNbt = customTags.isEmpty() ? "" : customTags.toString();

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(dto, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save card engine player data for " + player.getUUID(), e);
        }
    }

    public static void load(ServerPlayer player, PlayerUpgradeData data) {
        if (player.getServer() == null) return;
        try {
            Path root = player.getServer().getWorldPath(LevelResource.ROOT);
            Path dataFile = root.resolve("data").resolve("card_engine").resolve(player.getUUID().toString() + ".json");
            File file = dataFile.toFile();
            if (!file.exists()) {
                // Initialize defaults
                data.setLevel(1);
                data.setExperience(0.0);
                data.setRollCount(3);
                data.setCurrentChoices(null);
                data.getStats().clear();
                save(player, data);
                return;
            }

            try (FileReader reader = new FileReader(file)) {
                PlayerDataDTO dto = GSON.fromJson(reader, PlayerDataDTO.class);
                if (dto != null) {
                    data.setLevel(dto.level);
                    data.setExperience(dto.experience);
                    data.setRollCount(dto.rollCount);
                    data.setCurrentChoices(dto.currentChoices == null || dto.currentChoices.isEmpty() ? null : dto.currentChoices);
                    data.getStats().clear();
                    if (dto.stats != null) {
                        data.getStats().putAll(dto.stats);
                    }
                    data.setPendingDraws(dto.pendingDraws);
                    data.getChosenCards().clear();
                    if (dto.chosenCards != null) {
                        if (dto.chosenCards.isJsonArray()) {
                            for (JsonElement element : dto.chosenCards.getAsJsonArray()) {
                                if (element.isJsonObject()) {
                                    JsonObject obj = element.getAsJsonObject();
                                    if (obj.has("id") && obj.has("level")) {
                                        data.getChosenCards().put(obj.get("id").getAsString(), obj.get("level").getAsInt());
                                    }
                                }
                            }
                        } else if (dto.chosenCards.isJsonObject()) {
                            for (Map.Entry<String, JsonElement> entry : dto.chosenCards.getAsJsonObject().entrySet()) {
                                data.getChosenCards().put(entry.getKey(), entry.getValue().getAsInt());
                            }
                        }
                    }
                    data.setActiveSkillSlot1(dto.activeSkillSlot1 != null ? dto.activeSkillSlot1 : "");
                    data.setActiveSkillSlot2(dto.activeSkillSlot2 != null ? dto.activeSkillSlot2 : "");
                    data.setPassiveSkillSlot(dto.passiveSkillSlot != null ? dto.passiveSkillSlot : "");
                    data.getCooldowns().clear();
                    if (dto.cooldownTicks != null && dto.maxCooldownTicks != null) {
                        for (String key : dto.cooldownTicks.keySet()) {
                            int remaining = dto.cooldownTicks.get(key);
                            int max = dto.maxCooldownTicks.getOrDefault(key, remaining);
                            data.getCooldowns().put(key, new net.com.cardengine.cooldown.CooldownState(remaining, max));
                        }
                    }

                    // Load custom NBT
                    if (dto.customNbt != null && !dto.customNbt.isEmpty()) {
                        try {
                            CompoundTag customTags = TagParser.parseTag(dto.customNbt);
                            CompoundTag persistentData = player.getPersistentData();
                            for (String key : customTags.getAllKeys()) {
                                persistentData.put(key, customTags.get(key).copy());
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to parse custom NBT string for player " + player.getUUID(), e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load card engine player data for " + player.getUUID(), e);
        }
    }
}
