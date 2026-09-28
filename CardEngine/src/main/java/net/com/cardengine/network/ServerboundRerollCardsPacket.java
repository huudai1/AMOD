package net.com.cardengine.network;

import net.com.cardengine.CardDefinition;
import net.com.cardengine.CardManager;
import net.com.cardengine.capability.CardEngineStorage;
import net.com.cardengine.capability.PlayerUpgradeProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ServerboundRerollCardsPacket {
    private final int index;

    public ServerboundRerollCardsPacket(int index) {
        this.index = index;
    }

    public static ServerboundRerollCardsPacket decode(FriendlyByteBuf buf) {
        return new ServerboundRerollCardsPacket(buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(index);
    }

    public static void handle(ServerboundRerollCardsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).ifPresent(upgrade -> {
                List<String> currentChoices = upgrade.getCurrentChoices();
                if (currentChoices != null && currentChoices.size() > packet.index && packet.index >= 0) {
                    if (upgrade.getRollCount() > 0) {
                        upgrade.setRollCount(upgrade.getRollCount() - 1);
                        
                        // Exclude all current choices from being re-drawn to prevent duplicates
                        CardDefinition replacement = CardManager.drawSingleReplacementCard(player, currentChoices);
                        if (replacement != null) {
                            List<String> newChoices = new ArrayList<>(currentChoices);
                            newChoices.set(packet.index, replacement.getId());
                            upgrade.setCurrentChoices(newChoices);
                            
                            // Load updated CardDefinition list to send back to client
                            List<CardDefinition> updatedCards = new ArrayList<>();
                            for (String id : newChoices) {
                                CardDefinition cardDef = CardManager.CARDS.get(id);
                                if (cardDef != null) {
                                    updatedCards.add(cardDef);
                                }
                            }
                            
                            ModPackets.sendToPlayer(new ClientboundOpenSelectionScreenPacket(updatedCards), player);
                            CardEngineStorage.save(player, upgrade);
                            upgrade.sync(player);
                        }
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
