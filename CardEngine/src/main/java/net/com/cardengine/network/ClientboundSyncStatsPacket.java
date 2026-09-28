package net.com.cardengine.network;

import net.com.cardengine.capability.PlayerUpgradeProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class ClientboundSyncStatsPacket {
    private final int level;
    private final double experience;
    private final CompoundTag stats;
    private final int pendingChoicesCount;
    private final int rollCount;

    public ClientboundSyncStatsPacket(int level, double experience, CompoundTag stats, int pendingChoicesCount, int rollCount) {
        this.level = level;
        this.experience = experience;
        this.stats = stats;
        this.pendingChoicesCount = pendingChoicesCount;
        this.rollCount = rollCount;
    }

    public ClientboundSyncStatsPacket(net.minecraft.world.entity.player.Player player) {
        var upgrade = player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).orElse(null);
        this.level = upgrade != null ? upgrade.getLevel() : 1;
        this.experience = upgrade != null ? upgrade.getExperience() : 0.0;
        this.pendingChoicesCount = upgrade != null ? upgrade.getPendingDraws() + (upgrade.getCurrentChoices() != null ? 1 : 0) : 0;
        this.rollCount = upgrade != null ? upgrade.getRollCount() : 0;

        CompoundTag cardEngineNbt = new CompoundTag();
        if (upgrade != null) {
            for (Map.Entry<String, Double> entry : upgrade.getStats().entrySet()) {
                cardEngineNbt.putDouble(entry.getKey(), entry.getValue());
            }

            cardEngineNbt.putString("active_skill_slot_1", upgrade.getActiveSkillSlot1());
            cardEngineNbt.putString("active_skill_slot_2", upgrade.getActiveSkillSlot2());
            cardEngineNbt.putString("passive_skill_slot", upgrade.getPassiveSkillSlot());

            CompoundTag cdTag = new CompoundTag();
            for (Map.Entry<String, net.com.cardengine.cooldown.CooldownState> entry : upgrade.getCooldowns().entrySet()) {
                net.com.cardengine.cooldown.CooldownState state = entry.getValue();
                CompoundTag stateTag = new CompoundTag();
                stateTag.putInt("remaining", state.getRemainingTicks());
                stateTag.putInt("max", state.getMaxTicks());
                cdTag.put(entry.getKey(), stateTag);
            }
            cardEngineNbt.put("cooldowns", cdTag);

            CompoundTag chosenTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : upgrade.getChosenCards().entrySet()) {
                chosenTag.putInt(entry.getKey(), entry.getValue());
            }
            cardEngineNbt.put("chosen_cards", chosenTag);
        }
        this.stats = cardEngineNbt;
    }

    public static ClientboundSyncStatsPacket decode(FriendlyByteBuf buf) {
        return new ClientboundSyncStatsPacket(buf.readInt(), buf.readDouble(), buf.readNbt(), buf.readInt(), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeDouble(experience);
        buf.writeNbt(stats);
        buf.writeInt(pendingChoicesCount);
        buf.writeInt(rollCount);
    }

    public static void handle(ClientboundSyncStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.com.cardengine.client.ClientStatsHandler.updateStats(packet.level, packet.experience, packet.stats, packet.pendingChoicesCount, packet.rollCount);
        });
        ctx.get().setPacketHandled(true);
    }
}
