package net.com.cardengine;

import com.mojang.logging.LogUtils;
import net.com.cardengine.api.registries.CardRegistry;
import net.com.cardengine.api.registries.CardEngine;
import net.com.cardengine.capability.CardEngineStorage;
import net.com.cardengine.capability.PlayerUpgradeData;
import net.com.cardengine.capability.PlayerUpgradeProvider;
import net.com.cardengine.client.CardHUDOverlay;
import net.com.cardengine.command.CardEngineCommand;
import net.com.cardengine.network.ClientboundOpenSelectionScreenPacket;
import net.com.cardengine.network.ModPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.com.cardengine.api.cleanup.CardResourceManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

@Mod(CardEngineMod.MODID)
public class CardEngineMod {
    public static final String MODID = "card_engine";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CardEngineMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Card Engine Mod common setup...");

        // Register Network
        ModPackets.register();
    }

    public static void applyPlayerModifiers(ServerPlayer player) {
        player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
            for (Map.Entry<String, net.minecraft.world.entity.ai.attributes.Attribute> entry : CardRegistry.ATTRIBUTE_STATS.entrySet()) {
                String statKey = entry.getKey();
                net.minecraft.world.entity.ai.attributes.Attribute attribute = entry.getValue();
                double value = upgrade.getStats().getOrDefault(statKey, 0.0);

                var attr = player.getAttribute(attribute);
                if (attr != null) {
                    java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(statKey.getBytes());
                    attr.removeModifier(uuid);
                    if (value > 0) {
                        attr.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                uuid,
                                "Card Engine Upgrade: " + statKey,
                                value,
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                        ));
                    }
                }
            }
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerUpgradeData.class);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).isPresent()) {
                event.addCapability(PlayerUpgradeProvider.IDENTIFIER, new PlayerUpgradeProvider());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
            newPlayer.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(newStore -> {
                CardEngineStorage.load(newPlayer, newStore);
                applyPlayerModifiers(newPlayer);
                newStore.sync(newPlayer);
            });
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                CardEngineStorage.load(player, upgrade);
                applyPlayerModifiers(player);
                upgrade.sync(player);
            });
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                CardEngineStorage.load(player, upgrade);
                applyPlayerModifiers(player);
                upgrade.sync(player);
                
                // Heal player to new max health on respawn
                player.setHealth(player.getMaxHealth());

                if (upgrade.getCurrentChoices() != null && !upgrade.getCurrentChoices().isEmpty()) {
                    List<CardDefinition> drawn = upgrade.getCurrentChoices().stream()
                            .map(CardManager.CARDS::get)
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    if (!drawn.isEmpty()) {
                        ModPackets.sendToPlayer(new ClientboundOpenSelectionScreenPacket(drawn), player);
                    }
                } else {
                    upgrade.triggerPendingDraw(player);
                }
            });
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                CardEngineStorage.load(player, upgrade);
                applyPlayerModifiers(player);
                upgrade.sync(player);

                if (upgrade.getCurrentChoices() != null && !upgrade.getCurrentChoices().isEmpty()) {
                    List<CardDefinition> drawn = upgrade.getCurrentChoices().stream()
                            .map(CardManager.CARDS::get)
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    if (!drawn.isEmpty()) {
                        ModPackets.sendToPlayer(new ClientboundOpenSelectionScreenPacket(drawn), player);
                    }
                } else {
                    upgrade.triggerPendingDraw(player);
                }
            });

            // Dọn dẹp minions / task vị trí cũ khi player đổi dimension
            CardResourceManager.cleanupByPlayer(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                CardEngineStorage.save(player, upgrade);
            });
            // Dọn dẹp toàn bộ tài nguyên tạm của player khi đăng xuất
            CardResourceManager.cleanupByPlayer(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            CardResourceManager.cleanupByEntity(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            CardResourceManager.cleanupByEntity(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        CardResourceManager.cleanupAll();
    }

    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            long tick = event.getServer().getTickCount();
            if (tick % 100 == 0) { // Mỗi 5s (100 ticks) quét dọn TTL một lần
                CardResourceManager.tickTtl(tick);
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CardEngineCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        CardManager.loadCards();
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void onPlayerXpChange(net.minecraftforge.event.entity.player.PlayerXpEvent.XpChange event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            int amount = event.getAmount();
            if (amount > 0) {
                player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                    upgrade.addExperience(amount, player);
                });
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.side.isServer()) {
            if (event.player instanceof ServerPlayer player) {
                player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                    upgrade.tickCooldowns(player);
                });
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Card Engine Mod Client setup...");
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(new ResourceLocation("minecraft", "hotbar"), "card_hud", CardHUDOverlay.HUD_OVERLAY);
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            net.com.cardengine.client.ModKeyBindings.register(event);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                net.com.cardengine.client.ClientStatsHandler.tickClientCooldown();
            }
        }

        @SubscribeEvent
        public static void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen == null && mc.player != null) {
                if (net.com.cardengine.client.ModKeyBindings.TOGGLE_SELECTION_KEY.consumeClick()) {
                    net.com.cardengine.network.ModPackets.sendToServer(new net.com.cardengine.network.ServerboundRequestOpenSelectionPacket());
                } else if (net.com.cardengine.client.ModKeyBindings.TOGGLE_STATS_KEY.consumeClick()) {
                    mc.setScreen(new net.com.cardengine.client.CardStatsScreen());
                } else if (net.com.cardengine.client.ModKeyBindings.ACTIVE_SKILL_1_KEY.consumeClick()) {
                    tryCastSkill(1);
                } else if (net.com.cardengine.client.ModKeyBindings.ACTIVE_SKILL_2_KEY.consumeClick()) {
                    tryCastSkill(2);
                }
            }
        }

        private static void tryCastSkill(int slot) {
            String skillId = (slot == 1) ? net.com.cardengine.client.ClientStatsHandler.getActiveSkillSlot1()
                                         : net.com.cardengine.client.ClientStatsHandler.getActiveSkillSlot2();
            if (skillId != null && !skillId.isEmpty()) {
                if (!net.com.cardengine.cooldown.CooldownManager.isOnCooldown(net.minecraft.client.Minecraft.getInstance().player, skillId)) {
                    net.com.cardengine.network.ModPackets.sendToServer(new net.com.cardengine.network.ServerboundCastActiveSkillPacket(slot));
                }
            }
        }
    }
}
