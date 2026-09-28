package net.com.cardengine.api.cleanup;

import net.com.cardengine.api.cleanup.resources.CollectionResource;
import net.com.cardengine.api.cleanup.resources.CustomTaskResource;
import net.com.cardengine.api.cleanup.resources.EntityResource;
import net.com.cardengine.api.cleanup.resources.NbtResource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry và bộ điều phối trung tâm quản lý vòng đời tài nguyên sinh ra từ Card và Skill.
 * Cung cấp các API 1 dòng code để đăng ký và tự động thu hồi tài nguyên (Entity, NBT, State, Task).
 */
public final class CardResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CardResourceManager.class);

    // Key: playerUuid:skillId -> List of resources
    private static final Map<String, List<ICardResource>> BY_PLAYER_SKILL = new ConcurrentHashMap<>();

    // Key: entityUuid -> List of resources (Entity itself or NBT bound to it)
    private static final Map<UUID, List<ICardResource>> BY_ENTITY = new ConcurrentHashMap<>();

    // Tất cả tài nguyên đang hoạt động (dùng cho TTL sweep và cleanup all khi stop server)
    private static final Set<ICardResource> ALL_ACTIVE = ConcurrentHashMap.newKeySet();

    private CardResourceManager() {}

    private static String makeKey(UUID playerUuid, String skillId) {
        return (playerUuid != null ? playerUuid.toString() : "global") + ":" + (skillId != null ? skillId : "");
    }

    /**
     * Đăng ký một tài nguyên vào hệ thống quản lý.
     */
    public static void registerResource(ICardResource resource) {
        if (resource == null) return;
        ALL_ACTIVE.add(resource);

        if (resource.getPlayerUuid() != null || (resource.getCardOrSkillId() != null && !resource.getCardOrSkillId().isEmpty())) {
            String key = makeKey(resource.getPlayerUuid(), resource.getCardOrSkillId());
            BY_PLAYER_SKILL.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(resource);
        }

        if (resource instanceof EntityResource er && er.getEntityUuid() != null) {
            BY_ENTITY.computeIfAbsent(er.getEntityUuid(), k -> new CopyOnWriteArrayList<>()).add(resource);
        } else if (resource instanceof NbtResource nr && nr.getTargetEntityUuid() != null) {
            BY_ENTITY.computeIfAbsent(nr.getTargetEntityUuid(), k -> new CopyOnWriteArrayList<>()).add(resource);
        }
    }

    // ==========================================
    // CÁC HÀM TIỆN LỢI ĐĂNG KÝ (BINDING APIS)
    // ==========================================

    /**
     * Ràng buộc một Entity được triệu hồi với Skill của Người chơi.
     */
    public static void bindEntity(ServerPlayer player, String skillId, Entity entity) {
        bindEntity(player != null ? player.getUUID() : null, skillId, entity, -1, player != null ? player.tickCount : 0);
    }

    /**
     * Ràng buộc một Entity được triệu hồi có giới hạn thời gian sống (TTL).
     */
    public static void bindEntity(ServerPlayer player, String skillId, Entity entity, int ttlTicks) {
        bindEntity(player != null ? player.getUUID() : null, skillId, entity, ttlTicks, player != null ? player.tickCount : 0);
    }

    public static void bindEntity(UUID playerUuid, String skillId, Entity entity, int ttlTicks, long createdAtTick) {
        if (entity == null) return;
        registerResource(new EntityResource(playerUuid, skillId, entity, createdAtTick, ttlTicks));
    }

    /**
     * Đánh dấu và ràng buộc một NBT key trên Target Entity với Skill/Card.
     */
    public static void bindEntityNbt(Entity targetEntity, String nbtKey, String skillId) {
        bindEntityNbt(null, targetEntity, nbtKey, skillId, -1, 0);
    }

    /**
     * Đánh dấu và ràng buộc một NBT key có Player sở hữu và có thời gian sống (TTL).
     */
    public static void bindEntityNbt(ServerPlayer player, Entity targetEntity, String nbtKey, String skillId, int ttlTicks) {
        bindEntityNbt(player != null ? player.getUUID() : null, targetEntity, nbtKey, skillId, ttlTicks, player != null ? player.tickCount : 0);
    }

    public static void bindEntityNbt(UUID playerUuid, Entity targetEntity, String nbtKey, String skillId, int ttlTicks, long createdAtTick) {
        if (targetEntity == null || nbtKey == null || nbtKey.isEmpty()) return;
        registerResource(new NbtResource(playerUuid, skillId, targetEntity, nbtKey, createdAtTick, ttlTicks));
    }

    /**
     * Ràng buộc một Collection (List, Set) lưu trạng thái tạm với Skill của Người chơi.
     */
    public static void bindState(ServerPlayer player, String skillId, Collection<?> collection) {
        if (collection == null) return;
        registerResource(new CollectionResource(player != null ? player.getUUID() : null, skillId, collection, player != null ? player.tickCount : 0, -1));
    }

    /**
     * Ràng buộc một Map lưu trạng thái tạm với Skill của Người chơi.
     */
    public static void bindState(ServerPlayer player, String skillId, Map<?, ?> map) {
        if (map == null) return;
        registerResource(new CollectionResource(player != null ? player.getUUID() : null, skillId, map, player != null ? player.tickCount : 0, -1));
    }

    /**
     * Ràng buộc một Task dọn dẹp tùy chỉnh (Runnable callback) với Skill của Người chơi.
     */
    public static void bindTask(ServerPlayer player, String skillId, Runnable cleanupTask) {
        bindTask(player != null ? player.getUUID() : null, skillId, cleanupTask, -1, player != null ? player.tickCount : 0);
    }

    /**
     * Ràng buộc một Task dọn dẹp có thời hạn TTL.
     */
    public static void bindTask(ServerPlayer player, String skillId, Runnable cleanupTask, int ttlTicks) {
        bindTask(player != null ? player.getUUID() : null, skillId, cleanupTask, ttlTicks, player != null ? player.tickCount : 0);
    }

    public static void bindTask(UUID playerUuid, String skillId, Runnable cleanupTask, int ttlTicks, long createdAtTick) {
        if (cleanupTask == null) return;
        registerResource(new CustomTaskResource(playerUuid, skillId, cleanupTask, createdAtTick, ttlTicks));
    }

    // ==========================================
    // CÁC HÀM DỌN DẸP THEO SỰ KIỆN (CLEANUP APIS)
    // ==========================================

    /**
     * Dọn dẹp toàn bộ tài nguyên thuộc về một Skill cụ thể của Người chơi (ví dụ khi tháo thẻ).
     */
    public static void cleanupBySkill(ServerPlayer player, String skillId) {
        if (player == null) return;
        cleanupBySkill(player.getUUID(), skillId);
    }

    public static void cleanupBySkill(UUID playerUuid, String skillId) {
        if (playerUuid == null || skillId == null || skillId.isEmpty()) return;
        String key = makeKey(playerUuid, skillId);
        List<ICardResource> resources = BY_PLAYER_SKILL.remove(key);
        if (resources != null) {
            for (ICardResource res : resources) {
                cleanupSafely(res);
                ALL_ACTIVE.remove(res);
            }
        }
    }

    /**
     * Dọn dẹp toàn bộ tài nguyên của một Người chơi (ví dụ khi logout hoặc đổi dimension).
     */
    public static void cleanupByPlayer(ServerPlayer player) {
        if (player == null) return;
        cleanupByPlayer(player.getUUID());
    }

    public static void cleanupByPlayer(UUID playerUuid) {
        if (playerUuid == null) return;
        String prefix = playerUuid.toString() + ":";
        Iterator<Map.Entry<String, List<ICardResource>>> it = BY_PLAYER_SKILL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<ICardResource>> entry = it.next();
            if (entry.getKey().startsWith(prefix)) {
                it.remove();
                for (ICardResource res : entry.getValue()) {
                    cleanupSafely(res);
                    ALL_ACTIVE.remove(res);
                }
            }
        }
    }

    /**
     * Dọn dẹp tài nguyên gắn với một Entity (khi entity chết, despawn hoặc chunk unload).
     */
    public static void cleanupByEntity(UUID entityUuid) {
        if (entityUuid == null) return;
        List<ICardResource> resources = BY_ENTITY.remove(entityUuid);
        if (resources != null) {
            for (ICardResource res : resources) {
                cleanupSafely(res);
                ALL_ACTIVE.remove(res);
            }
        }
    }

    /**
     * Quét và dọn dẹp các tài nguyên quá hạn Time-To-Live (gọi định kỳ mỗi vài giây).
     */
    public static void tickTtl(long currentTick) {
        if (ALL_ACTIVE.isEmpty()) return;
        Iterator<ICardResource> it = ALL_ACTIVE.iterator();
        while (it.hasNext()) {
            ICardResource res = it.next();
            if (res.isExpired(currentTick) || !res.isValid()) {
                cleanupSafely(res);
                it.remove();
            }
        }
    }

    /**
     * Dọn sạch toàn bộ tài nguyên (khi Server dừng hoặc World unload).
     */
    public static void cleanupAll() {
        LOGGER.info("Cleaning up all Card Engine tracked resources...");
        for (ICardResource res : ALL_ACTIVE) {
            cleanupSafely(res);
        }
        ALL_ACTIVE.clear();
        BY_PLAYER_SKILL.clear();
        BY_ENTITY.clear();
    }

    private static void cleanupSafely(ICardResource resource) {
        if (resource == null) return;
        try {
            resource.cleanup();
        } catch (Exception e) {
            LOGGER.error("Failed to cleanup resource for skill {}: {}", resource.getCardOrSkillId(), e.getMessage(), e);
        }
    }
}
