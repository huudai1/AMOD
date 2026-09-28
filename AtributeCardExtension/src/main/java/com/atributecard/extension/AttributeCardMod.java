package com.atributecard.extension;

import net.com.cardengine.CardDefinition;
import net.com.cardengine.EffectDefinition;
import net.com.cardengine.api.registries.CardRegistry;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AttributeCardMod.MODID)
public class AttributeCardMod {
    public static final String MODID = "attribute_card_extension";

    public AttributeCardMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Step 1: Register mechanisms
        CardRegistry.registerCard("attribute_card_extension:max_health", Attributes.MAX_HEALTH);
        CardRegistry.registerCard("attribute_card_extension:attack_damage", Attributes.ATTACK_DAMAGE);
        CardRegistry.registerCard("attribute_card_extension:attack_speed", Attributes.ATTACK_SPEED);
        CardRegistry.registerCard("attribute_card_extension:armor", Attributes.ARMOR);
        CardRegistry.registerCard("attribute_card_extension:magic_resist", Attributes.ARMOR_TOUGHNESS);
        CardRegistry.registerCard("attribute_card_extension:movement_speed", Attributes.MOVEMENT_SPEED);
        
        CardRegistry.registerCard("attribute_card_extension:ability_power");
        CardRegistry.registerCard("attribute_card_extension:crit_chance");
        CardRegistry.registerCard("attribute_card_extension:life_steal");
        CardRegistry.registerCard("attribute_card_extension:omnivamp");
        CardRegistry.registerCard("attribute_card_extension:health_regen");
        CardRegistry.registerCard("attribute_card_extension:ability_haste");
        CardRegistry.registerCard("attribute_card_extension:bonus_xp");

        // Step 2: Loop to dynamically build 72 card templates and register them
        String[] stats = {
            "max_health", "attack_damage", "attack_speed", "armor", "magic_resist", "movement_speed",
            "ability_power", "crit_chance", "life_steal", "omnivamp", "health_regen", "ability_haste",
            "bonus_xp"
        };

        String[] rarities = {
            "common", "uncommon", "rare", "epic", "legendary", "mythic"
        };

        for (String stat : stats) {
            String mechanismName = "attribute_card_extension:" + stat;
            for (int rIdx = 0; rIdx < rarities.length; rIdx++) {
                String rarity = rarities[rIdx];
                String cardId = mechanismName + "_" + rarity;
                double value = getStatValueForRarity(stat, rIdx);
                String icon = getStatIcon(stat);

                // Build description
                List<String> description = getStatDescription(stat);

                EffectDefinition effect = new EffectDefinition(mechanismName, value);
                
                CardDefinition card = new CardDefinition(
                    cardId,
                    icon,
                    rarity,
                    "all",
                    description,
                    List.of(effect)
                );

                CardRegistry.registerDefaultCard(card);
            }
        }
    }

    private static List<String> getStatDescription(String stat) {
        List<String> desc = new ArrayList<>();
        switch (stat) {
            case "max_health":
                desc.add("§7Increases maximum health, allowing you");
                desc.add("§7to survive longer in combat.");
                break;
            case "attack_damage":
                desc.add("§7Increases the raw physical damage");
                desc.add("§7dealt by your melee weapons.");
                break;
            case "attack_speed":
                desc.add("§7Increases the swing speed of your");
                desc.add("§7weapons, letting you strike faster.");
                break;
            case "armor":
                desc.add("§7Reduces physical damage taken");
                desc.add("§7from enemy attacks.");
                break;
            case "magic_resist":
                desc.add("§7Reduces damage taken from");
                desc.add("§7magical sources and spells.");
                break;
            case "movement_speed":
                desc.add("§7Increases your base walking");
                desc.add("§7and running speed.");
                break;
            case "ability_power":
                desc.add("§7Increases the damage of spells, magic,");
                desc.add("§7and status effects like poison.");
                break;
            case "crit_chance":
                desc.add("§7Grants a chance to deal double");
                desc.add("§7damage on physical attacks.");
                break;
            case "life_steal":
                desc.add("§7Restores health based on a portion");
                desc.add("§7of melee damage dealt.");
                break;
            case "omnivamp":
                desc.add("§7Restores health based on a portion");
                desc.add("§7of all damage dealt.");
                break;
            case "health_regen":
                desc.add("§7Passive health regeneration that");
                desc.add("§7heals you every 5 seconds.");
                break;
            case "ability_haste":
                desc.add("§7Reduces the cooldowns of all");
                desc.add("§7active skills and spells.");
                break;
            case "bonus_xp":
                desc.add("§7Increases the amount of experience");
                desc.add("§7gained from all sources.");
                break;
            default:
                desc.add("§7Increases " + formatStatDisplayName(stat) + ".");
                break;
        }
        return desc;
    }

    private static double getStatValueForRarity(String stat, int rIdx) {
        int level = rIdx + 1;
        switch (stat) {
            case "max_health":
            case "armor":
            case "magic_resist":
            case "ability_power":
                return (double) level;
            case "attack_damage":
                return 0.25 * level;
            case "attack_speed":
                return 0.10 * level;
            case "movement_speed":
                return 0.02 * level;
            case "crit_chance":
                return 0.01 * level;
            case "life_steal":
            case "omnivamp":
                return 0.0025 * level;
            case "health_regen":
                return 0.5 * level;
            case "ability_haste":
                double[] hasteVals = {5.0, 12.0, 25.0, 50.0, 100.0, 200.0};
                return hasteVals[rIdx];
            case "bonus_xp":
                if (rIdx == 4) return 1.00; // Legendary: +100%
                if (rIdx == 5) return 2.00; // Mythic: +200%
                return 0.25 + 0.05 * rIdx; // Common-Epic: 25% - 40%
            default:
                return 0.0;
        }
    }

    private static String getStatIcon(String stat) {
        switch (stat) {
            case "max_health": return "minecraft:apple";
            case "attack_damage": return "minecraft:iron_sword";
            case "attack_speed": return "minecraft:sugar";
            case "armor": return "minecraft:iron_chestplate";
            case "magic_resist": return "minecraft:shield";
            case "movement_speed": return "minecraft:feather";
            case "ability_power": return "minecraft:blaze_rod";
            case "crit_chance": return "minecraft:golden_sword";
            case "life_steal": return "minecraft:redstone";
            case "omnivamp": return "minecraft:fermented_spider_eye";
            case "health_regen": return "minecraft:ghast_tear";
            case "ability_haste": return "minecraft:clock";
            case "bonus_xp": return "minecraft:experience_bottle";
            default: return "minecraft:paper";
        }
    }

    private static String formatStatDisplayName(String stat) {
        switch (stat) {
            case "max_health": return "Max Health";
            case "attack_damage": return "Attack Damage";
            case "attack_speed": return "Attack Speed";
            case "armor": return "Armor";
            case "magic_resist": return "Magic Resist";
            case "movement_speed": return "Movement Speed";
            case "ability_power": return "Ability Power";
            case "crit_chance": return "Crit Chance";
            case "life_steal": return "Life Steal";
            case "omnivamp": return "Omnivamp";
            case "health_regen": return "HP Regen";
            case "ability_haste": return "Ability Haste";
            case "bonus_xp": return "Bonus XP";
            default: return stat;
        }
    }

    private static String formatStatDisplayValue(String stat, double value) {
        if (stat.equals("life_steal") || stat.equals("omnivamp")) {
            double pct = value * 100.0;
            String valStr = String.format("%.2f", pct);
            if (valStr.endsWith(".00")) {
                valStr = valStr.substring(0, valStr.length() - 3);
            } else if (valStr.endsWith("0")) {
                valStr = valStr.substring(0, valStr.length() - 1);
            }
            return valStr + "%";
        }
        if (stat.equals("movement_speed") || stat.equals("crit_chance") || stat.equals("bonus_xp")) {
            double pct = value * 100.0;
            String valStr = String.format("%.0f", pct);
            return valStr + "%";
        }
        if (value == (long) value) {
            return String.valueOf((long) value);
        } else {
            String valStr = String.format("%.2f", value);
            if (valStr.endsWith(".00")) {
                valStr = valStr.substring(0, valStr.length() - 3);
            } else if (valStr.endsWith("0")) {
                valStr = valStr.substring(0, valStr.length() - 1);
            }
            return valStr;
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GameplayEvents {

        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            // Critical Strike
            if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide()) {
                if (event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
                    player.getCapability(net.com.cardengine.capability.PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                        double critChance = upgrade.getStats().getOrDefault("attribute_card_extension:crit_chance", 0.0);
                        if (critChance > 0.0 && player.getRandom().nextFloat() < critChance) {
                            // Double the damage
                            float originalDamage = event.getAmount();
                            float doubleDamage = originalDamage * 2.0f;
                            event.setAmount(doubleDamage);
                            player.sendSystemMessage(Component.literal("§c§lCRITICAL STRIKE! §6Dealt " + String.format("%.1f", doubleDamage) + " damage!"));
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide()) {
                player.getCapability(net.com.cardengine.capability.PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                    // Melee Life Steal
                    if (event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
                        double lifeSteal = upgrade.getStats().getOrDefault("attribute_card_extension:life_steal", 0.0);
                        if (lifeSteal > 0.0) {
                            float healAmount = (float) (event.getAmount() * lifeSteal);
                            player.heal(healAmount);
                        }
                    }

                    // Omnivamp (works on all damage sources from player)
                    double omnivamp = upgrade.getStats().getOrDefault("attribute_card_extension:omnivamp", 0.0);
                    if (omnivamp > 0.0) {
                        float healAmount = (float) (event.getAmount() * omnivamp);
                        player.heal(healAmount);
                    }

                    // Ability Power damage boost
                    if (event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC)) {
                        double ap = upgrade.getStats().getOrDefault("attribute_card_extension:ability_power", 0.0);
                        if (ap > 0.0) {
                            float bonusFactor = (float) (1.0 + (ap / 100.0));
                            event.setAmount(event.getAmount() * bonusFactor);
                        }
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
                Player player = event.player;
                if (player.tickCount % 100 == 0) { // Every 5 seconds (100 ticks)
                    player.getCapability(net.com.cardengine.capability.PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                        double regen = upgrade.getStats().getOrDefault("attribute_card_extension:health_regen", 0.0);
                        if (regen > 0.0) {
                            player.heal((float) regen);
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerXpChange(net.minecraftforge.event.entity.player.PlayerXpEvent.XpChange event) {
            if (event.isCanceled()) return;
            Player player = event.getEntity();
            if (!player.level().isClientSide()) {
                player.getCapability(net.com.cardengine.capability.PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                    double bonusXp = upgrade.getStats().getOrDefault("attribute_card_extension:bonus_xp", 0.0);
                    if (bonusXp > 0.0) {
                        int amount = event.getAmount();
                        if (amount > 0) {
                            int extra = (int) Math.round(amount * bonusXp);
                            if (extra > 0) {
                                event.setAmount(amount + extra);
                            }
                        }
                    }
                });
            }
        }
    }
}
