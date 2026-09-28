package net.com.cardengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSyncCastingPacket {
    private final boolean casting;
    private final String skillId;
    private final int slot;
    private final int totalTicks;
    private final int remainingTicks;
    private final boolean interrupted;

    public ClientboundSyncCastingPacket(boolean casting, String skillId, int slot, int totalTicks, int remainingTicks, boolean interrupted) {
        this.casting = casting;
        this.skillId = skillId != null ? skillId : "";
        this.slot = slot;
        this.totalTicks = totalTicks;
        this.remainingTicks = remainingTicks;
        this.interrupted = interrupted;
    }

    public static ClientboundSyncCastingPacket decode(FriendlyByteBuf buf) {
        return new ClientboundSyncCastingPacket(
                buf.readBoolean(),
                buf.readUtf(256),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(casting);
        buf.writeUtf(skillId, 256);
        buf.writeInt(slot);
        buf.writeInt(totalTicks);
        buf.writeInt(remainingTicks);
        buf.writeBoolean(interrupted);
    }

    public static void handle(ClientboundSyncCastingPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.com.cardengine.client.ClientStatsHandler.updateCastingState(
                    packet.casting,
                    packet.skillId,
                    packet.slot,
                    packet.totalTicks,
                    packet.remainingTicks,
                    packet.interrupted
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
