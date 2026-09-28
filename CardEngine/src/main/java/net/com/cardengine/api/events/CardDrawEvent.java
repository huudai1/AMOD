package net.com.cardengine.api.events;

import net.com.cardengine.CardDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public class CardDrawEvent extends Event {
    private final ServerPlayer player;
    private final List<CardDefinition> pool;
    private int amount;

    public CardDrawEvent(ServerPlayer player, List<CardDefinition> pool, int amount) {
        this.player = player;
        this.pool = pool;
        this.amount = amount;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public List<CardDefinition> getPool() {
        return pool;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
