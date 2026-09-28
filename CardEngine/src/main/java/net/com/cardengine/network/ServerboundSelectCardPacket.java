package net.com.cardengine.network;

import net.com.cardengine.CardDefinition;
import net.com.cardengine.CardManager;
import net.com.cardengine.EffectDefinition;
import net.com.cardengine.api.events.CardSelectEvent;
import net.com.cardengine.api.registries.CardRegistry;
import net.com.cardengine.api.registries.CardEngine;
import net.com.cardengine.api.registries.CardParam;
import net.com.cardengine.api.registries.CardParamUp;
import net.com.cardengine.api.registries.CardParamDown;
import net.com.cardengine.capability.PlayerUpgradeProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.function.Supplier;

public class ServerboundSelectCardPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String cardId;
    private final int targetSlot;

    public ServerboundSelectCardPacket(String cardId) {
        this(cardId, 0);
    }

    public ServerboundSelectCardPacket(String cardId, int targetSlot) {
        this.cardId = cardId;
        this.targetSlot = targetSlot;
    }

    public static ServerboundSelectCardPacket decode(FriendlyByteBuf buf) {
        return new ServerboundSelectCardPacket(buf.readUtf(256), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(cardId, 256);
        buf.writeInt(targetSlot);
    }

    public static void handle(ServerboundSelectCardPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                if (upgrade.getCurrentChoices() == null || !upgrade.getCurrentChoices().contains(packet.cardId)) {
                    LOGGER.warn("Player {} tried to select a card {} that wasn't offered to them!", player.getName().getString(), packet.cardId);
                    return;
                }

                CardDefinition card = CardManager.CARDS.get(packet.cardId);
                if (card == null) {
                    LOGGER.warn("Player {} selected card {} which doesn't exist!", player.getName().getString(), packet.cardId);
                    return;
                }

                CardSelectEvent event = new CardSelectEvent(player, card);
                MinecraftForge.EVENT_BUS.post(event);

                if (!event.isCanceled()) {
                    // ── Passive skill card enforcement ─────────────────────────────
                    if (isPassiveSkillCard(card)) {
                        String oldPassive = upgrade.getPassiveSkillSlot();
                        boolean slotOccupied = !oldPassive.isEmpty();
                        boolean upgradeInPlace = slotOccupied && oldPassive.equals(card.getId());
                        boolean replacing = slotOccupied && !upgradeInPlace;

                        if (replacing && packet.targetSlot != 3) {
                            // Client should have shown confirmation first — block request
                            LOGGER.warn("Player {} tried to overwrite passive slot without confirmation! (card={})",
                                    player.getName().getString(), packet.cardId);
                            return;
                        }

                        if (upgradeInPlace) {
                            // Same passive, upgrading star level — fire onUpgrade lifecycle
                            upgrade.upgradePassiveSkill(player);
                            // Already handled chosenCards in upgradePassiveSkill; skip normal apply
                            upgrade.setCurrentChoices(null);
                            net.com.cardengine.capability.CardEngineStorage.save(player, upgrade);
                            upgrade.sync(player);
                            LOGGER.info("Player {} upgraded passive skill: {}", player.getName().getString(), card.getId());
                            upgrade.triggerPendingDraw(player);
                            return;
                        }

                        if (replacing) {
                            // Confirmed replacement — remove old passive from chosenCards entirely
                            upgrade.getChosenCards().remove(oldPassive);
                        }

                        // Set star to 1 then equip (lifecycle: onUnequip old, onEquip new)
                        upgrade.getChosenCards().put(card.getId(), 1);
                        upgrade.equipPassiveSkill(card.getId(), player);

                        // Clear choices, save, sync, and move on
                        upgrade.setCurrentChoices(null);
                        net.com.cardengine.capability.CardEngineStorage.save(player, upgrade);
                        upgrade.sync(player);
                        LOGGER.info("Player {} equipped passive skill: {}", player.getName().getString(), card.getId());
                        upgrade.triggerPendingDraw(player);
                        return;
                    }

                    for (EffectDefinition effect : card.getEffects()) {
                        String type = effect.getType();
                        
                        // Auto-equip if the effect type is registered as an Active Skill
                        if (CardRegistry.ACTIVE_SKILLS.containsKey(type)) {
                            upgrade.equipSkillToSlot(type, packet.targetSlot, player);
                        }

                        boolean handled = false;

                        // 1. Check class-based CardEngines (inject @CardParam parameters)
                        if (CardRegistry.ENGINE_CLASSES.containsKey(type)) {
                            Class<? extends CardEngine> clazz = CardRegistry.ENGINE_CLASSES.get(type);
                            try {
                                CardEngine instance = clazz.getDeclaredConstructor().newInstance();
                                int targetLevel = upgrade.getChosenCards().getOrDefault(card.getId(), 0) + 1;
                                
                                // Populate fields with @CardParam
                                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                                    boolean hasParam = false;
                                    double val = 0.0;
                                    
                                    if (field.isAnnotationPresent(CardParam.class)) {
                                        CardParam param = field.getAnnotation(CardParam.class);
                                        val = param.value();
                                        hasParam = true;
                                    } else if (field.isAnnotationPresent(CardParamUp.class)) {
                                        hasParam = true;
                                    } else if (field.isAnnotationPresent(CardParamDown.class)) {
                                        hasParam = true;
                                    }
                                    
                                    if (hasParam) {
                                        field.setAccessible(true);
                                        String fieldName = field.getName();
                                        
                                        if (effect.getParams() != null && effect.getParams().containsKey(fieldName)) {
                                            val = effect.getParams().get(fieldName);
                                        } else if (fieldName.equals("value")) {
                                            val = effect.getValue();
                                        }
                                        
                                        // Apply Up/Down scaling if marked
                                        if (field.isAnnotationPresent(CardParamUp.class)) {
                                            val = val * getUpScaleMultiplier(targetLevel);
                                        } else if (field.isAnnotationPresent(CardParamDown.class)) {
                                            val = val * getDownScaleMultiplier(targetLevel);
                                        }
                                        
                                        // Set depending on field type
                                        if (field.getType() == double.class || field.getType() == Double.class) {
                                            field.set(instance, val);
                                        } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                            field.set(instance, (int) val);
                                        } else if (field.getType() == float.class || field.getType() == Float.class) {
                                            field.set(instance, (float) val);
                                        }
                                    }
                                }
                                
                                instance.apply(player);
                                handled = true;
                            } catch (Exception e) {
                                LOGGER.error("Error instantiating class-based CardEngine for type: {}", type, e);
                            }
                        }

                        // 2. Check lambda-based CardEngines
                        if (!handled && CardRegistry.CARD_ENGINES.containsKey(type)) {
                            CardEngine handler = CardRegistry.CARD_ENGINES.get(type);
                            try {
                                handler.apply(player);
                                handled = true;
                            } catch (Exception e) {
                                LOGGER.error("Error applying lambda CardEngine: {}", type, e);
                            }
                        }

                        // 3. Fallback automated stat/attribute modification logic
                        if (!handled && CardRegistry.REGISTERED_STATS.contains(type)) {
                            final String statType = type;
                            final double val = effect.getValue();
                            try {
                                double current = upgrade.getStats().getOrDefault(statType, 0.0);
                                upgrade.getStats().put(statType, current + val);
                                
                                // Auto-apply attribute mapping if registered to standard Minecraft Attribute
                                if (CardRegistry.ATTRIBUTE_STATS.containsKey(statType)) {
                                    net.com.cardengine.CardEngineMod.applyPlayerModifiers(player);
                                    if (CardRegistry.ATTRIBUTE_STATS.get(statType) == net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) {
                                        player.heal((float) val);
                                    }
                                }
                                
                                // Print clean system message to notify user of their upgrade
                                String displayName = formatDisplayName(statType);
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a" + displayName + " increased by +" + formatValue(val)));
                            } catch (Exception e) {
                                LOGGER.error("Error applying automated stat modification for type: {}", statType, e);
                            }
                        }
                    }

                    // Increment chosen cards count
                    upgrade.getChosenCards().put(card.getId(), upgrade.getChosenCards().getOrDefault(card.getId(), 0) + 1);

                    upgrade.setCurrentChoices(null);
                    net.com.cardengine.capability.CardEngineStorage.save(player, upgrade);
                    upgrade.sync(player);
                    LOGGER.info("Player {} successfully selected card: {}", player.getName().getString(), card.getId());

                    // Trigger next pending draw in the queue
                    upgrade.triggerPendingDraw(player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
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

    /**
     * Returns true if this card is a passive skill card:
     * - has at least one effect NOT in REGISTERED_STATS
     * - has NO effect in ACTIVE_SKILLS
     */
    public static boolean isPassiveSkillCard(CardDefinition card) {
        return net.com.cardengine.CardManager.isPassiveSkillCard(card);
    }

    public static double getUpScaleMultiplier(int level) {
        if (level <= 1) return 1.0;
        if (level == 2) return 1.15;
        if (level == 3) return 1.35;
        if (level == 4) return 1.55;
        return 1.70;
    }

    public static double getDownScaleMultiplier(int level) {
        if (level <= 1) return 1.0;
        if (level == 2) return 0.85;
        if (level == 3) return 0.65;
        if (level == 4) return 0.45;
        return 0.30;
    }
}
