package net.com.cardengine.api.cleanup.resources;

import net.com.cardengine.api.cleanup.ICardResource;
import net.minecraft.world.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Quản lý vòng đời của Summoned Entity / Projectile sinh ra từ Card hoặc Skill.
 * Khi được dọn dẹp, entity sẽ được gọi .discard() để loại bỏ khỏi thế giới an toàn.
 */
public class EntityResource implements ICardResource {

    private final UUID playerUuid;
    private final String cardOrSkillId;
    private final UUID entityUuid;
    private final WeakReference<Entity> entityRef;
    private final long createdAtTick;
    private final int ttlTicks;

    public EntityResource(UUID playerUuid, String cardOrSkillId, Entity entity, long createdAtTick, int ttlTicks) {
        this.playerUuid = playerUuid;
        this.cardOrSkillId = (cardOrSkillId != null) ? cardOrSkillId : "";
        this.entityUuid = (entity != null) ? entity.getUUID() : null;
        this.entityRef = (entity != null) ? new WeakReference<>(entity) : new WeakReference<>(null);
        this.createdAtTick = createdAtTick;
        this.ttlTicks = ttlTicks;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    @Override
    public void cleanup() {
        Entity entity = entityRef.get();
        if (entity != null && entity.isAlive()) {
            entity.discard();
        }
    }

    @Override
    public boolean isValid() {
        Entity entity = entityRef.get();
        return entity != null && entity.isAlive() && !entity.isRemoved();
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
