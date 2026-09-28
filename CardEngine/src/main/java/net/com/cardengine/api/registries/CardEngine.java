package net.com.cardengine.api.registries;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface CardEngine {
    void apply(ServerPlayer player);
}
