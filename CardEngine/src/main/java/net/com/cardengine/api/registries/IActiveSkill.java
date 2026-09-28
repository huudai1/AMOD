package net.com.cardengine.api.registries;

import net.minecraft.server.level.ServerPlayer;

/**
 * Interface cho kỹ năng chủ động (Active Skill) trong CardEngine.
 * Hỗ trợ vòng đời đầy đủ: onEquip và onUnequip khi người chơi lắp/tháo thẻ ở Slot 1 hoặc Slot 2.
 */
@FunctionalInterface
public interface IActiveSkill {

    /**
     * Kích hoạt kỹ năng khi người chơi nhấn phím thi triển (R cho Slot 1, X cho Slot 2).
     * @param player người chơi thi triển
     */
    void trigger(ServerPlayer player);

    /**
     * Được gọi khi kỹ năng được trang bị vào Slot 1 hoặc Slot 2.
     * @param player người chơi trang bị
     * @param slot vị trí slot (1 hoặc 2)
     */
    default void onEquip(ServerPlayer player, int slot) {}

    /**
     * Được gọi khi kỹ năng bị tháo ra hoặc bị ghi đè bởi kỹ năng khác.
     * Dùng để dọn dẹp các trạng thái dở dang, hủy minions, gỡ modifiers hoặc tasks.
     * @param player người chơi tháo kỹ năng
     * @param slot vị trí slot (1 hoặc 2)
     */
    default void onUnequip(ServerPlayer player, int slot) {}
}
