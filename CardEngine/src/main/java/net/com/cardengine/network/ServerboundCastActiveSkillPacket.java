package net.com.cardengine.network;

import net.com.cardengine.api.registries.CardRegistry;
import net.com.cardengine.api.registries.IActiveSkill;
import net.com.cardengine.capability.CardEngineStorage;
import net.com.cardengine.capability.PlayerUpgradeProvider;
import net.com.cardengine.cooldown.CooldownManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundCastActiveSkillPacket {
    private final int slot;

    public ServerboundCastActiveSkillPacket(int slot) {
        this.slot = slot;
    }

    public static ServerboundCastActiveSkillPacket decode(FriendlyByteBuf buf) {
        return new ServerboundCastActiveSkillPacket(buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slot);
    }

    public static void handle(ServerboundCastActiveSkillPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                // Block any action while already casting
                if (upgrade.isCasting()) return;

                String skillId = (packet.slot == 1)
                        ? upgrade.getActiveSkillSlot1()
                        : upgrade.getActiveSkillSlot2();

                if (skillId == null || skillId.isEmpty()) return;

                IActiveSkill skill = CardRegistry.ACTIVE_SKILLS.get(skillId);
                if (skill == null) return;

                if (CooldownManager.isOnCooldown(player, skillId)) return;

                // Determine cast time for this skill
                int castTimeTicks = CardRegistry.BASE_CAST_TIMES.getOrDefault(skillId, 0);

                if (castTimeTicks > 0) {
                    // === Cast-time skill: start casting — the tick loop fires it later ===
                    boolean started = upgrade.startCasting(skillId, packet.slot, castTimeTicks, player);
                    if (!started) return; // already casting (redundant guard)
                } else {
                    // === Instant skill: trigger right now ===
                    skill.trigger(player);

                    // Calculate and start cooldown (apply star-level scaling + CDR)
                    int skillLevel = upgrade.getSkillLevel(skillId);
                    int baseCooldown = CardRegistry.BASE_COOLDOWNS.getOrDefault(skillId, 0);
                    double levelDownScale = ServerboundSelectCardPacket.getDownScaleMultiplier(skillLevel);
                    double cdr = upgrade.getStats().getOrDefault("card_engine:cooldown_reduction", 0.0);
                    double cdrMultiplier = 1.0 - Math.min(0.9, Math.max(0.0, cdr));
                    int realCooldown = (int) Math.max(1, Math.round(baseCooldown * levelDownScale * cdrMultiplier));

                    CooldownManager.start(player, skillId, realCooldown);
                }

                CardEngineStorage.save(player, upgrade);
                upgrade.sync(player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
