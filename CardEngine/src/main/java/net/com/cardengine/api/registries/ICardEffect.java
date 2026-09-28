package net.com.cardengine.api.registries;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ICardEffect {
    void apply(ServerPlayer player, double value);
}
