package net.com.cardengine.api.cleanup;

import java.util.UUID;

/**
 * Interface đại diện cho một tài nguyên (Entity, NBT, Collection, Task)
 * được sinh ra từ Card hoặc Skill và cần dọn dẹp khi hết vòng đời.
 */
public interface ICardResource {

    /**
     * Thực thi logic giải phóng / dọn dẹp tài nguyên.
     */
    void cleanup();

    /**
     * Kiểm tra tài nguyên có còn hợp lệ hay không.
     * @return true nếu tài nguyên vẫn còn tồn tại trong thế giới/bộ nhớ.
     */
    default boolean isValid() {
        return true;
    }

    /**
     * UUID của người chơi sở hữu tài nguyên này (hoặc null nếu gắn thuần theo entity).
     */
    UUID getPlayerUuid();

    /**
     * Card ID hoặc Skill ID sinh ra tài nguyên này.
     */
    String getCardOrSkillId();

    /**
     * Thời điểm (tick) tài nguyên được đăng ký.
     */
    default long getCreatedAtTick() {
        return 0L;
    }

    /**
     * Thời gian sống tối đa tính theo ticks (-1 là vô hạn cho đến khi có sự kiện dọn dẹp).
     */
    default int getTtlTicks() {
        return -1;
    }

    /**
     * Kiểm tra xem tài nguyên đã quá hạn Time-To-Live chưa.
     */
    default boolean isExpired(long currentTick) {
        return getTtlTicks() > 0 && (currentTick - getCreatedAtTick()) >= getTtlTicks();
    }
}
