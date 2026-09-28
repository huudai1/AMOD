package net.com.cardengine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.com.cardengine.api.events.CardDrawEvent;
import net.com.cardengine.api.registries.CardRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CardManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    public static final Map<String, CardDefinition> CARDS = new HashMap<>();

    public static void loadCards() {
        CARDS.clear();
        File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "card_engine/cards");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // Always check and generate default templates for missing files.
        generateDefaults(configDir);

        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    CardDefinition card = GSON.fromJson(reader, CardDefinition.class);
                    if (card != null && card.getId() != null) {
                        CARDS.put(card.getId(), card);
                        LOGGER.info("Loaded card: {} ({})", card.getId(), card.getRarity());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load card config: {}", file.getName(), e);
                }
            }
        }
        LOGGER.info("Card Engine loaded {} cards.", CARDS.size());
    }

    private static void generateDefaults(File dir) {
        for (CardDefinition card : CardRegistry.DEFAULT_CARDS) {
            String fileName = card.getId().replace(":", "_") + ".json";
            File file = new File(dir, fileName);
            if (!file.exists()) {
                LOGGER.info("Generating default card config: {}", fileName);
                try (FileWriter writer = new FileWriter(file)) {
                    GSON.toJson(card, writer);
                } catch (IOException e) {
                    LOGGER.error("Could not write default card file: {}", fileName, e);
                }
            }
        }
    }
    public static List<CardDefinition> drawCards(ServerPlayer player, int amount) {
        List<CardDefinition> pool = new ArrayList<>();
        String playerClass = player.getPersistentData().getString("card_engine:class");
        if (playerClass.isEmpty()) playerClass = "all";

        var upgradeOpt = player.getCapability(net.com.cardengine.capability.PlayerUpgradeProvider.PLAYER_UPGRADE).resolve();
        Map<String, Integer> chosen = upgradeOpt.isPresent() ? upgradeOpt.get().getChosenCards() : Collections.emptyMap();

        for (CardDefinition card : CARDS.values()) {
            String required = card.getRequiredClass();
            if (required.equalsIgnoreCase("all") || required.equalsIgnoreCase(playerClass)) {
                // 1. Check maxStack
                int chosenCount = chosen.getOrDefault(card.getId(), 0);
                if (chosenCount >= card.getMaxStack()) {
                    continue; // Reached maximum selections, skip
                }

                // 2. Check requiredCards
                boolean reqsMet = true;
                if (card.getRequiredCards() != null) {
                    for (String reqCard : card.getRequiredCards()) {
                        if (!chosen.containsKey(reqCard) || chosen.get(reqCard) <= 0) {
                            reqsMet = false;
                            break;
                        }
                    }
                }
                if (!reqsMet) {
                    continue; // Prerequisites not met, skip
                }

                // 3. Passive cards cannot be upgraded or stacked — skip if already chosen/equipped
                if (isPassiveSkillCard(card)) {
                    String currentPassive = upgradeOpt.map(net.com.cardengine.capability.PlayerUpgradeData::getPassiveSkillSlot).orElse("");
                    if (chosen.containsKey(card.getId()) || card.getId().equals(currentPassive)) {
                        continue;
                    }
                }

                pool.add(card);
            }
        }

        // Fire CardDrawEvent to allow mod compatibility
        CardDrawEvent event = new CardDrawEvent(player, pool, amount);
        MinecraftForge.EVENT_BUS.post(event);

        List<CardDefinition> activePool = event.getPool();
        int drawAmount = event.getAmount();

        if (activePool.isEmpty()) {
            return Collections.emptyList();
        }

        List<CardDefinition> selected = new ArrayList<>();
        List<CardDefinition> tempPool = new ArrayList<>(activePool);
        Random rand = new Random();

        for (int i = 0; i < drawAmount && !tempPool.isEmpty(); i++) {
            CardDefinition drawnCard = selectWeighted(tempPool, rand);
            if (drawnCard != null) {
                selected.add(drawnCard);
                tempPool.remove(drawnCard); // Prevent duplicate card choices in the same draw
            }
        }

        return selected;
    }

    public static CardDefinition drawSingleReplacementCard(ServerPlayer player, List<String> excludeIds) {
        List<CardDefinition> pool = new ArrayList<>();
        String playerClass = player.getPersistentData().getString("card_engine:class");
        if (playerClass.isEmpty()) playerClass = "all";

        var upgradeOpt = player.getCapability(net.com.cardengine.capability.PlayerUpgradeProvider.PLAYER_UPGRADE).resolve();
        Map<String, Integer> chosen = upgradeOpt.isPresent() ? upgradeOpt.get().getChosenCards() : Collections.emptyMap();

        for (CardDefinition card : CARDS.values()) {
            if (excludeIds.contains(card.getId())) {
                continue; // Skip excluded cards
            }
            String required = card.getRequiredClass();
            if (required.equalsIgnoreCase("all") || required.equalsIgnoreCase(playerClass)) {
                // Check maxStack
                int chosenCount = chosen.getOrDefault(card.getId(), 0);
                if (chosenCount >= card.getMaxStack()) {
                    continue;
                }

                // Check requiredCards
                boolean reqsMet = true;
                if (card.getRequiredCards() != null) {
                    for (String reqCard : card.getRequiredCards()) {
                        if (!chosen.containsKey(reqCard) || chosen.get(reqCard) <= 0) {
                            reqsMet = false;
                            break;
                        }
                    }
                }
                if (!reqsMet) {
                    continue;
                }

                pool.add(card);
            }
        }

        if (pool.isEmpty()) {
            return null;
        }

        // Weighted draw
        Random rand = new Random();
        return selectWeighted(pool, rand);
    }

    private static CardDefinition selectWeighted(List<CardDefinition> pool, Random rand) {
        double totalWeight = 0;
        for (CardDefinition card : pool) {
            totalWeight += card.getRarityEnum().getDefaultWeight();
        }

        if (totalWeight <= 0) {
            return pool.get(rand.nextInt(pool.size()));
        }

        double value = rand.nextDouble() * totalWeight;
        double sum = 0;
        for (CardDefinition card : pool) {
            sum += card.getRarityEnum().getDefaultWeight();
            if (value <= sum) {
                return card;
            }
        }

        return pool.get(pool.size() - 1);
    }

    public static boolean isPassiveSkillCard(CardDefinition card) {
        if (card == null || card.getEffects() == null || card.getEffects().isEmpty()) return false;
        boolean hasNonStat = false;
        for (EffectDefinition eff : card.getEffects()) {
            if (net.com.cardengine.api.registries.CardRegistry.ACTIVE_SKILLS.containsKey(eff.getType())) {
                return false; // Active skill card
            }
            if (!net.com.cardengine.api.registries.CardRegistry.REGISTERED_STATS.contains(eff.getType())) {
                hasNonStat = true;
            }
        }
        return hasNonStat;
    }
}
