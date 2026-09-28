package net.com.cardengine.api.events;

import net.com.cardengine.CardDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class CardSelectEvent extends Event {
    private final ServerPlayer player;
    private final CardDefinition selectedCard;

    public CardSelectEvent(ServerPlayer player, CardDefinition selectedCard) {
        this.player = player;
        this.selectedCard = selectedCard;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public CardDefinition getSelectedCard() {
        return selectedCard;
    }
}
