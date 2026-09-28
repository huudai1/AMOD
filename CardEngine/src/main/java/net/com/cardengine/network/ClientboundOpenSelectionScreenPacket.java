package net.com.cardengine.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.com.cardengine.CardDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundOpenSelectionScreenPacket {
    private static final Gson GSON = new Gson();
    private final List<CardDefinition> cards;

    public ClientboundOpenSelectionScreenPacket(List<CardDefinition> cards) {
        this.cards = cards;
    }

    public static ClientboundOpenSelectionScreenPacket decode(FriendlyByteBuf buf) {
        String json = buf.readUtf(32767);
        List<CardDefinition> list = GSON.fromJson(json, new TypeToken<List<CardDefinition>>(){}.getType());
        return new ClientboundOpenSelectionScreenPacket(list != null ? list : new ArrayList<>());
    }

    public void encode(FriendlyByteBuf buf) {
        String json = GSON.toJson(cards);
        buf.writeUtf(json, 32767);
    }

    public static void handle(ClientboundOpenSelectionScreenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.com.cardengine.client.ClientStatsHandler.openSelectionScreen(packet.cards);
        });
        ctx.get().setPacketHandled(true);
    }
}
