package net.com.cardengine.api.registries;

import net.minecraft.server.level.ServerPlayer;

public interface IPassiveSkill {
    /**
     * Called when the player equips this passive skill into their passive slot.
     * @param player the player equipping the skill
     * @param starLevel current star level (1 to 5) of the card
     */
    default void onEquip(ServerPlayer player, int starLevel) {}

    /**
     * Called when this passive skill is unequipped/replaced from the passive slot.
     * Use this to clean up attribute modifiers, active buffs, or tracked states.
     * @param player the player unequipping the skill
     */
    default void onUnequip(ServerPlayer player) {}

    /**
     * Called periodically on server player tick while this passive skill is equipped.
     * @param player the player with this passive equipped
     * @param starLevel current star level (1 to 5)
     */
    default void onTick(ServerPlayer player, int starLevel) {}

    /**
     * Called when the player upgrades this passive card to a higher star level while already equipped.
     * @param player the player
     * @param newStarLevel new star level (e.g. 2, 3, 4, 5)
     */
    default void onUpgrade(ServerPlayer player, int newStarLevel) {}
}
