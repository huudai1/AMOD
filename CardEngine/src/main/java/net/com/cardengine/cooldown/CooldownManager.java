package net.com.cardengine.cooldown;

import net.com.cardengine.capability.PlayerUpgradeProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

public class CooldownManager {
    public static boolean start(ServerPlayer player, String key, int ticks) {
        var opt = player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).resolve();
        if (opt.isPresent()) {
            var upgrade = opt.get();
            if (!upgrade.getCooldowns().containsKey(key)) {
                upgrade.getCooldowns().put(key, new CooldownState(ticks));
                upgrade.sync(player);
                return true;
            }
        }
        return false;
    }

    public static boolean isOnCooldown(Player player, String key) {
        var opt = player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).resolve();
        if (opt.isPresent()) {
            var upgrade = opt.get();
            CooldownState state = upgrade.getCooldowns().get(key);
            return state != null && !state.isExpired();
        }
        return false;
    }

    public static int getRemainingTicks(Player player, String key) {
        var opt = player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).resolve();
        if (opt.isPresent()) {
            var upgrade = opt.get();
            CooldownState state = upgrade.getCooldowns().get(key);
            return state != null ? state.getRemainingTicks() : 0;
        }
        return 0;
    }

    public static int getMaxTicks(Player player, String key) {
        var opt = player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).resolve();
        if (opt.isPresent()) {
            var upgrade = opt.get();
            CooldownState state = upgrade.getCooldowns().get(key);
            return state != null ? state.getMaxTicks() : 0;
        }
        return 0;
    }

    public static void reset(ServerPlayer player) {
        var opt = player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).resolve();
        if (opt.isPresent()) {
            var upgrade = opt.get();
            upgrade.getCooldowns().clear();
            upgrade.sync(player);
        }
    }

    public static void reset(ServerPlayer player, String key) {
        var opt = player.getCapability(PlayerUpgradeProvider.PLAYER_UPGRADE).resolve();
        if (opt.isPresent()) {
            var upgrade = opt.get();
            upgrade.getCooldowns().remove(key);
            upgrade.sync(player);
        }
    }
}
