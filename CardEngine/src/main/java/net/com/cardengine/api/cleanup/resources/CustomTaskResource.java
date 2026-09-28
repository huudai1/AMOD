package net.com.cardengine.api.cleanup.resources;

import net.com.cardengine.api.cleanup.ICardResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Quản lý vòng đời thông qua callback Runnable tùy chỉnh.
 * Cho phép nhà phát triển chạy code dọn dẹp logic riêng khi thẻ bị tháo/hủy.
 */
public class CustomTaskResource implements ICardResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTaskResource.class);

    private final UUID playerUuid;
    private final String cardOrSkillId;
    private final Runnable cleanupAction;
    private final long createdAtTick;
    private final int ttlTicks;
    private boolean executed = false;

    public CustomTaskResource(UUID playerUuid, String cardOrSkillId, Runnable cleanupAction, long createdAtTick, int ttlTicks) {
        this.playerUuid = playerUuid;
        this.cardOrSkillId = (cardOrSkillId != null) ? cardOrSkillId : "";
        this.cleanupAction = cleanupAction;
        this.createdAtTick = createdAtTick;
        this.ttlTicks = ttlTicks;
    }

    @Override
    public synchronized void cleanup() {
        if (!executed && cleanupAction != null) {
            executed = true;
            try {
                cleanupAction.run();
            } catch (Exception e) {
                LOGGER.error("Error executing custom cleanup task for skill {}: {}", cardOrSkillId, e.getMessage(), e);
            }
        }
    }

    @Override
    public synchronized boolean isValid() {
        return !executed;
    }

    @Override
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @Override
    public String getCardOrSkillId() {
        return cardOrSkillId;
    }

    @Override
    public long getCreatedAtTick() {
        return createdAtTick;
    }

    @Override
    public int getTtlTicks() {
        return ttlTicks;
    }
}
