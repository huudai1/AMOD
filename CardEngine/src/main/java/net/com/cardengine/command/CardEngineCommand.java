package net.com.cardengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.com.cardengine.CardDefinition;
import net.com.cardengine.CardManager;
import net.com.cardengine.api.registries.CardRegistry;
import net.com.cardengine.capability.CardEngineStorage;
import net.com.cardengine.capability.PlayerUpgradeProvider;
import net.com.cardengine.cooldown.CooldownManager;
import net.com.cardengine.network.ClientboundOpenSelectionScreenPacket;
import net.com.cardengine.network.ModPackets;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CardEngineCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cardengine")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("draw")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> draw(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("xp")
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                .executes(context -> addXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), DoubleArgumentType.getDouble(context, "amount")))))))
                .then(Commands.literal("stats")
                        .executes(context -> showStats(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("reload")
                        .executes(context -> reloadConfigs(context.getSource())))
                // ── Cooldown Reset ─────────────────────────────────────────────
                .then(Commands.literal("cooldown")
                        .then(Commands.literal("reset")
                                .executes(context -> resetCooldown(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException()), null))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> resetCooldown(context.getSource(), EntityArgument.getPlayers(context, "targets"), null))
                                        .then(Commands.argument("skillId", StringArgumentType.string())
                                                .suggests((ctx, builder) -> {
                                                    for (String key : CardRegistry.ACTIVE_SKILLS.keySet()) {
                                                        builder.suggest(key);
                                                        if (key.startsWith("skillcard_extension:")) {
                                                            builder.suggest(key.substring("skillcard_extension:".length()));
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> resetCooldown(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "skillId")))
                                        )
                                )
                        )
                )
                // ── Quick Shorthand: /cardengine resetcd ───────────────────────
                .then(Commands.literal("resetcd")
                        .executes(context -> resetCooldown(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException()), null))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> resetCooldown(context.getSource(), EntityArgument.getPlayers(context, "targets"), null))
                                .then(Commands.argument("skillId", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            for (String key : CardRegistry.ACTIVE_SKILLS.keySet()) {
                                                builder.suggest(key);
                                                if (key.startsWith("skillcard_extension:")) {
                                                    builder.suggest(key.substring("skillcard_extension:".length()));
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> resetCooldown(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "skillId")))
                                )
                        )
                )
                // ── Passive Skills (Equip / Remove) ───────────────────────────
                .then(Commands.literal("passive")
                        .then(Commands.literal("equip")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("cardId", StringArgumentType.string())
                                                .suggests((ctx, builder) -> {
                                                    for (String key : CardRegistry.PASSIVE_SKILLS.keySet()) {
                                                        builder.suggest(key);
                                                        if (key.startsWith("skillcard_extension:")) {
                                                            builder.suggest(key.substring("skillcard_extension:".length()));
                                                        }
                                                    }
                                                    for (CardDefinition def : CardManager.CARDS.values()) {
                                                        if (CardManager.isPassiveSkillCard(def)) {
                                                            builder.suggest(def.getId());
                                                            if (def.getId().startsWith("skillcard_extension:")) {
                                                                builder.suggest(def.getId().substring("skillcard_extension:".length()));
                                                            }
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> equipPassive(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "cardId"), 1))
                                                .then(Commands.argument("starLevel", IntegerArgumentType.integer(1, 5))
                                                        .executes(context -> equipPassive(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "cardId"), IntegerArgumentType.getInteger(context, "starLevel")))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .executes(context -> removePassive(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> removePassive(context.getSource(), EntityArgument.getPlayers(context, "targets")))
                                )
                        )
                )
        );
    }

    private static int draw(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                upgrade.setPendingDraws(upgrade.getPendingDraws() + 1);
                upgrade.triggerPendingDraw(player);
                source.sendSuccess(() -> Component.literal("Enqueued forced card draw for " + player.getName().getString()), true);
            });
        }
        return targets.size();
    }

    private static int addXp(CommandSourceStack source, Collection<ServerPlayer> targets, double amount) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                upgrade.addExperience(amount, player);
                source.sendSuccess(() -> Component.literal("Added " + amount + " Card Engine XP to " + player.getName().getString()), true);
            });
        }
        return targets.size();
    }

    private static int showStats(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                source.sendSuccess(() -> Component.literal("=== Card Engine Stats for " + player.getName().getString() + " ==="), false);
                source.sendSuccess(() -> Component.literal("Level: " + upgrade.getLevel()), false);
                source.sendSuccess(() -> Component.literal(String.format("Experience: %.1f / %.1f", upgrade.getExperience(), upgrade.getXpNeeded())), false);
            });
        }
        return targets.size();
    }

    private static int reloadConfigs(CommandSourceStack source) {
        CardManager.loadCards();
        source.sendSuccess(() -> Component.literal("Reloaded card JSON definitions."), true);
        return 1;
    }

    private static int resetCooldown(CommandSourceStack source, Collection<ServerPlayer> targets, String rawSkillId) {
        String skillId = rawSkillId;
        if (skillId != null && !skillId.isEmpty()) {
            if (!CardRegistry.ACTIVE_SKILLS.containsKey(skillId) && CardRegistry.ACTIVE_SKILLS.containsKey("skillcard_extension:" + skillId)) {
                skillId = "skillcard_extension:" + skillId;
            }
        }
        final String finalSkillId = skillId;

        for (ServerPlayer player : targets) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                if (finalSkillId == null || finalSkillId.isEmpty()) {
                    CooldownManager.reset(player);
                    source.sendSuccess(() -> Component.literal("Reset all skill cooldowns for " + player.getName().getString()), true);
                } else {
                    CooldownManager.reset(player, finalSkillId);
                    source.sendSuccess(() -> Component.literal("Reset cooldown of " + finalSkillId + " for " + player.getName().getString()), true);
                }
            });
        }
        return targets.size();
    }

    private static int equipPassive(CommandSourceStack source, Collection<ServerPlayer> targets, String rawCardId, int starLevel) {
        String cardId = rawCardId;
        if (!CardManager.CARDS.containsKey(cardId) && !CardRegistry.PASSIVE_SKILLS.containsKey(cardId)) {
            if (CardManager.CARDS.containsKey("skillcard_extension:" + cardId)) {
                cardId = "skillcard_extension:" + cardId;
            } else if (CardRegistry.CARD_TO_SKILL.containsKey("skillcard_extension:" + cardId)) {
                cardId = "skillcard_extension:" + cardId;
            } else if (CardRegistry.SKILL_TO_CARD.containsKey(cardId)) {
                cardId = CardRegistry.SKILL_TO_CARD.get(cardId);
            } else if (CardRegistry.SKILL_TO_CARD.containsKey("skillcard_extension:" + cardId)) {
                cardId = CardRegistry.SKILL_TO_CARD.get("skillcard_extension:" + cardId);
            }
        }

        final String finalCardId = cardId;
        final int finalStar = Math.max(1, Math.min(5, starLevel));

        for (ServerPlayer player : targets) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                String old = upgrade.getPassiveSkillSlot();
                if (!old.isEmpty()) {
                    upgrade.getChosenCards().remove(old);
                    upgrade.removePassiveSkill(player);
                }

                upgrade.getChosenCards().put(finalCardId, finalStar);
                upgrade.equipPassiveSkill(finalCardId, player);
                CardEngineStorage.save(player, upgrade);
                upgrade.sync(player);

                source.sendSuccess(() -> Component.literal("Equipped passive " + finalCardId + " (" + finalStar + "★) to " + player.getName().getString()), true);
            });
        }
        return targets.size();
    }

    private static int removePassive(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                String old = upgrade.getPassiveSkillSlot();
                if (!old.isEmpty()) {
                    upgrade.getChosenCards().remove(old);
                    upgrade.removePassiveSkill(player);
                    CardEngineStorage.save(player, upgrade);
                    upgrade.sync(player);
                    source.sendSuccess(() -> Component.literal("Removed passive skill from " + player.getName().getString()), true);
                } else {
                    source.sendFailure(Component.literal(player.getName().getString() + " does not have an equipped passive skill."));
                }
            });
        }
        return targets.size();
    }
}
