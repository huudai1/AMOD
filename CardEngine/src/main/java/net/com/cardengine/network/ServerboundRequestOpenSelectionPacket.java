package net.com.cardengine.network;

import net.com.cardengine.CardDefinition;
import net.com.cardengine.CardManager;
import net.com.cardengine.capability.PlayerUpgradeProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ServerboundRequestOpenSelectionPacket {
    public ServerboundRequestOpenSelectionPacket() {}

    public static ServerboundRequestOpenSelectionPacket decode(FriendlyByteBuf buf) {
        return new ServerboundRequestOpenSelectionPacket();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ServerboundRequestOpenSelectionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                List<String> choices = upgrade.getCurrentChoices();
                if (choices != null && !choices.isEmpty()) {
                    List<CardDefinition> cardList = new ArrayList<>();
                    for (String id : choices) {
                        CardDefinition card = CardManager.CARDS.get(id);
                        if (card != null) {
                            cardList.add(card);
                        }
                    }
                    if (!cardList.isEmpty()) {
                        ModPackets.sendToPlayer(new ClientboundOpenSelectionScreenPacket(cardList), player);
                        return;
                    }
                }
                player.sendSystemMessage(Component.literal("§cYou do not have any pending card selections!"));
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
