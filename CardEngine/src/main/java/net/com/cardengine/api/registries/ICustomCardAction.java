package net.com.cardengine.api.registries;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ICustomCardAction {
    void apply(ServerPlayer player);
}
