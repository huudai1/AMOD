package net.com.cardengine.api.cleanup.resources;

import net.com.cardengine.api.cleanup.ICardResource;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Quản lý vòng đời của Collection (List/Set) hoặc Map lưu trữ trạng thái tạm thời của Skill/Card.
 * Khi dọn dẹp, sẽ tự động gọi .clear().
 */
public class CollectionResource implements ICardResource {

    private final UUID playerUuid;
    private final String cardOrSkillId;
    private final Collection<?> collection;
    private final Map<?, ?> map;
    private final long createdAtTick;
    private final int ttlTicks;

    public CollectionResource(UUID playerUuid, String cardOrSkillId, Collection<?> collection, long createdAtTick, int ttlTicks) {
        this.playerUuid = playerUuid;
        this.cardOrSkillId = (cardOrSkillId != null) ? cardOrSkillId : "";
        this.collection = collection;
        this.map = null;
        this.createdAtTick = createdAtTick;
        this.ttlTicks = ttlTicks;
    }

    public CollectionResource(UUID playerUuid, String cardOrSkillId, Map<?, ?> map, long createdAtTick, int ttlTicks) {
        this.playerUuid = playerUuid;
        this.cardOrSkillId = (cardOrSkillId != null) ? cardOrSkillId : "";
        this.collection = null;
        this.map = map;
        this.createdAtTick = createdAtTick;
        this.ttlTicks = ttlTicks;
    }

    @Override
    public void cleanup() {
        if (collection != null) {
            collection.clear();
        }
        if (map != null) {
            map.clear();
        }
    }

    @Override
    public boolean isValid() {
        if (collection != null) {
            return !collection.isEmpty();
        }
        if (map != null) {
            return !map.isEmpty();
        }
        return false;
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
