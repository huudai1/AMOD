package net.com.cardengine.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModPackets {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation("card_engine", "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ClientboundSyncStatsPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundSyncStatsPacket::decode)
                .encoder(ClientboundSyncStatsPacket::encode)
                .consumerNetworkThread(ClientboundSyncStatsPacket::handle)
                .add();

        net.messageBuilder(ClientboundOpenSelectionScreenPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundOpenSelectionScreenPacket::decode)
                .encoder(ClientboundOpenSelectionScreenPacket::encode)
                .consumerNetworkThread(ClientboundOpenSelectionScreenPacket::handle)
                .add();

        net.messageBuilder(ServerboundSelectCardPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundSelectCardPacket::decode)
                .encoder(ServerboundSelectCardPacket::encode)
                .consumerNetworkThread(ServerboundSelectCardPacket::handle)
                .add();

        net.messageBuilder(ServerboundRequestOpenSelectionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundRequestOpenSelectionPacket::decode)
                .encoder(ServerboundRequestOpenSelectionPacket::encode)
                .consumerNetworkThread(ServerboundRequestOpenSelectionPacket::handle)
                .add();

        net.messageBuilder(ServerboundRerollCardsPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundRerollCardsPacket::decode)
                .encoder(ServerboundRerollCardsPacket::encode)
                .consumerNetworkThread(ServerboundRerollCardsPacket::handle)
                .add();

        net.messageBuilder(ServerboundCastActiveSkillPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundCastActiveSkillPacket::decode)
                .encoder(ServerboundCastActiveSkillPacket::encode)
                .consumerNetworkThread(ServerboundCastActiveSkillPacket::handle)
                .add();

        net.messageBuilder(ClientboundSyncCastingPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundSyncCastingPacket::decode)
                .encoder(ClientboundSyncCastingPacket::encode)
                .consumerNetworkThread(ClientboundSyncCastingPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
