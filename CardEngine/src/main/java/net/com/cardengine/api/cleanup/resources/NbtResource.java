package net.com.cardengine.api.cleanup.resources;

import net.com.cardengine.api.cleanup.ICardResource;
import net.minecraft.world.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Quản lý vòng đời NBT tag được đánh dấu trên Entity hoặc Player.
 * Khi dọn dẹp, NBT key sẽ được gỡ bỏ khỏi PersistentData của entity.
 */
public class NbtResource implements ICardResource {

    private final UUID playerUuid;
    private final String cardOrSkillId;
    private final UUID targetEntityUuid;
    private final WeakReference<Entity> targetEntityRef;
    private final String nbtKey;
    private final long createdAtTick;
    private final int ttlTicks;

    public NbtResource(UUID playerUuid, String cardOrSkillId, Entity targetEntity, String nbtKey, long createdAtTick, int ttlTicks) {
        this.playerUuid = playerUuid;
        this.cardOrSkillId = (cardOrSkillId != null) ? cardOrSkillId : "";
        this.targetEntityUuid = (targetEntity != null) ? targetEntity.getUUID() : null;
        this.targetEntityRef = (targetEntity != null) ? new WeakReference<>(targetEntity) : new WeakReference<>(null);
        this.nbtKey = nbtKey;
        this.createdAtTick = createdAtTick;
        this.ttlTicks = ttlTicks;
    }

    public UUID getTargetEntityUuid() {
        return targetEntityUuid;
    }

    public String getNbtKey() {
        return nbtKey;
    }

    @Override
    public void cleanup() {
        Entity entity = targetEntityRef.get();
        if (entity != null && nbtKey != null && !nbtKey.isEmpty()) {
            entity.getPersistentData().remove(nbtKey);
        }
    }

    @Override
    public boolean isValid() {
        Entity entity = targetEntityRef.get();
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
