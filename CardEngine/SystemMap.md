# System Map - Card Engine Reference Manual

Tài liệu tham chiếu kiến trúc và hướng dẫn tích hợp dành cho Lập trình viên và AI Assistants về thư viện nền tảng **Card Engine** (`card_engine`).

---

## 1. Codebase File Map

### Entrypoint & Lifecycle
- [CardEngineMod.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/CardEngineMod.java): Mod lifecycle, networking channel, phím tắt (L, K, R, X), overlays GUI, và event bus (Clone, Respawn, LogIn/Out, DimensionChange, EntityLeaveLevel, LivingDeath, ServerStopped, ServerTick TTL sweep).

### Core Data & Card Management
- [CardDefinition.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/CardDefinition.java): Data class cho thẻ bài (id, icon, rarity, effects, description, level/stars).
- [EffectDefinition.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/EffectDefinition.java): Định nghĩa effect (`type`, `value`, `params`).
- [CardRarity.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/CardRarity.java): Enum độ hiếm (`COMMON`, `UNCOMMON`, `RARE`, `EPIC`, `LEGENDARY`, `MYTHIC`, `SPECIAL`).
- [CardManager.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/CardManager.java): Load cấu hình JSON từ `config/card_engine/cards/`, sinh file default nếu thiếu, weighted card drawing pool (theo rarity weight), kiểm tra `maxStack` / `requiredCards` prerequisite, lọc passive card đã trang bị, `drawSingleReplacementCard` cho reroll, `isPassiveSkillCard` phân loại thẻ.

### Registries & Public API
- [CardRegistry.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/CardRegistry.java): Trung tâm đăng ký stats, active skills, custom card engines, default cards:
  - `registerActiveSkill(skillId, cdTicks, logic)`: Đăng ký kỹ năng chủ động (nhận lambda hoặc instance của `IActiveSkill`).
  - `registerASkillCard(cardId, skillId)`: Đăng ký thẻ bài gắn active skill.
  - `registerPassiveSkill(cardId, skill)`: Đăng ký kỹ năng bị động (`IPassiveSkill`).
  - `registerPSkillCard(cardId, mechName)`: Đăng ký thẻ bài passive skill.
  - `registerStatsCard(cardId, statKey)`: Đăng ký thẻ tăng chỉ số thường.
  - `linkSkillAndCard(skillId, cardId)`: Liên kết 2 chiều giữa skill ID và card ID.
- [IActiveSkill.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/IActiveSkill.java): Giao diện kỹ năng chủ động với vòng đời hoàn chỉnh:
  - `void trigger(ServerPlayer player)`: Thi triển skill khi bấm phím.
  - `default void onEquip(ServerPlayer player, int slot)`: Kích hoạt khi trang bị vào Slot 1 hoặc Slot 2.
  - `default void onUnequip(ServerPlayer player, int slot)`: Kích hoạt khi tháo hoặc bị ghi đè khỏi slot.
- [IPassiveSkill.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/IPassiveSkill.java): Vòng đời kỹ năng bị động:
  - `void onEquip(ServerPlayer player, int starLevel)`: Trang bị passive — gắn attribute modifier, khởi tạo buff.
  - `void onUnequip(ServerPlayer player)`: Tháo passive — gỡ modifier. Engine tự gọi `CardResourceManager.cleanupBySkill()` sau đó.
  - `default void onTick(ServerPlayer player, int starLevel)`: Chạy logic kiểm tra điều kiện mỗi 10 ticks.
  - `default void onUpgrade(ServerPlayer player, int newStarLevel)`: Cập nhật lại sức mạnh buff khi nâng sao (1★ → 5★).
- [CardEngine.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/CardEngine.java): Base class cho dynamic mechanisms (`apply(ServerPlayer)`).
- [CardParam.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/CardParam.java), [CardParamUp.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/CardParamUp.java), [CardParamDown.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/CardParamDown.java): Annotations scale theo cấp sao thẻ bài.

### Automatic Clean Up & Resource Management (`net.com.cardengine.api.cleanup`)
- [CardResourceManager.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/cleanup/CardResourceManager.java): Bộ điều phối trung tâm quản lý tài nguyên tạm do Card/Skill sinh ra:
  - `bindEntity(player, skillId, entity, [ttlTicks])`: Ràng buộc summoned mob / projectile với skill.
  - `bindEntityNbt(targetEntity, nbtKey, skillId)`: Ràng buộc NBT tag đánh dấu trên mục tiêu.
  - `bindState(player, skillId, collectionOrMap)`: Ràng buộc List / Set / Map lưu trạng thái tạm thời.
  - `bindTask(player, skillId, runnable, [ttlTicks])`: Đăng ký callback Runnable dọn dẹp tùy biến.
  - `cleanupBySkill(player, skillId)`: Tự động dọn sạch mọi tài nguyên khi skill bị tháo ra.
  - `cleanupByPlayer(playerUuid)`: Tự động dọn khi player logout hoặc đổi dimension.
  - `cleanupByEntity(entityUuid)`: Tự động dọn khi target entity chết hoặc despawn.
  - `cleanupAll()`: Tự động dọn sạch khi Server stop / World unload.
  - `tickTtl(currentTick)`: Quét tự động mỗi 5s giải phóng các task/tài nguyên quá hạn TTL.
- [ICardResource.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/cleanup/ICardResource.java): Interface cho tài nguyên vòng đời.
- [EntityResource.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/cleanup/resources/EntityResource.java): Thu hồi Entity qua `entity.discard()`.
- [NbtResource.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/cleanup/resources/NbtResource.java): Xóa NBT key qua `entity.getPersistentData().remove(key)`.
- [CollectionResource.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/cleanup/resources/CollectionResource.java): Dọn dẹp dữ liệu qua `.clear()`.
- [CustomTaskResource.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/cleanup/resources/CustomTaskResource.java): Chạy `Runnable` tùy biến.

### Capability & Cooldowns
- [PlayerUpgradeData.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/capability/PlayerUpgradeData.java): Dữ liệu nâng cấp: level, exp, rollCount, stats map, `chosenCards`, `activeSkillSlot1`, `activeSkillSlot2`, và `passiveSkillSlot`.
  - `equipSkillToSlot(skillId, slot, player)`: **Tự động** gọi `oldSkill.onUnequip(player, slot)` → `CardResourceManager.cleanupBySkill(player, oldSkillId)` → lưu `newSkillId` → gọi `newSkill.onEquip(player, slot)`.
  - `unequipSkillSlot(slot, player)`: Tháo skill khỏi slot, kích hoạt dọn dẹp an toàn.
  - `equipPassiveSkill(cardId, player)`: **Tự động** gọi `oldSkill.onUnequip(player)` → `CardResourceManager.cleanupBySkill(player, oldCardId)` → gỡ thẻ cũ khỏi `chosenCards` → gọi `newSkill.onEquip(player, starLevel)`.
- [PlayerUpgradeProvider.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/capability/PlayerUpgradeProvider.java): Capability provider gắn vào ServerPlayer.
- [CardEngineStorage.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/capability/CardEngineStorage.java): Persistence JSON lưu theo world: `world/data/card_engine/[uuid].json`.
- [CooldownState.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/cooldown/CooldownState.java): State lưu tick đếm ngược hồi chiêu.
- [CooldownManager.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/cooldown/CooldownManager.java): Server-side cooldown checks & activations.

### Network Packets
- [ModPackets.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/network/ModPackets.java): Kênh SimpleChannel đăng ký gói tin.
- [ClientboundSyncStatsPacket.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/network/ClientboundSyncStatsPacket.java): Sync stats, level, xp, rolls, cooldowns, active slots và `passive_skill_slot` về client.
- [ClientboundOpenSelectionScreenPacket.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/network/ClientboundOpenSelectionScreenPacket.java): Mở UI chọn thẻ trên client.
- [ServerboundCastActiveSkillPacket.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/network/ServerboundCastActiveSkillPacket.java): Bấm phím R (slot 1) hoặc X (slot 2) để kích hoạt active skill kèm CDR calculation.
- [ServerboundSelectCardPacket.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/network/ServerboundSelectCardPacket.java): Gửi lựa chọn thẻ (`targetSlot = 0` auto, `1`/`2` replace active, `3` replace passive).
- [ServerboundRerollCardsPacket.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/network/ServerboundRerollCardsPacket.java): Reroll 1 slot thẻ cụ thể.

### Client GUI & Rendering
- [ModKeyBindings.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/client/ModKeyBindings.java): Phím tắt: `K` (chọn thẻ), `L` (bảng stats), `R` (slot 1), `X` (slot 2).
- [CardHUDOverlay.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/client/CardHUDOverlay.java): HUD overlay bên phải hotbar (2 ô active skill kèm đếm ngược cooldown) và bên trái hotbar (hàng chờ rút bài).
- [CardSelectionScreen.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/client/CardSelectionScreen.java): UI 3 thẻ rút bài, hiệu ứng kính mờ, nút Roll, và dialog xác nhận thay thế Active Slot (1/2) hoặc Passive Slot (3).
- [CardStatsScreen.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/client/CardStatsScreen.java): Giao diện 2 tab (phím L): Tab 0: `STATS`, Tab 1: `UPGRADES` (Active Slots, Passive Slot, Passive Upgrades list kèm tooltip).
- [ClientStatsHandler.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/client/ClientStatsHandler.java): Cache phía client nhận từ `ClientboundSyncStatsPacket`.

---

## 2. Active Skill & Cooldown Flow

1. **Đăng ký**: Gọi `CardRegistry.registerActiveSkill(skillId, cdTicks, skillInstance)` và `CardRegistry.registerASkillCard(cardId, skillId)`.
2. **Trang bị & Vòng đời (Slot 1 & Slot 2)**:
   - Khi chọn thẻ active skill: nếu còn slot trống (slot 1, sau đó slot 2), tự động trang bị.
   - Nếu cả 2 slot đầy: `CardSelectionScreen` hiển thị dialog hỏi thay thế Slot 1 hay Slot 2.
   - Khi chọn slot thay thế:
     1. Gọi `oldSkill.onUnequip(player, slot)` cho kỹ năng cũ.
     2. Tự động gọi `CardResourceManager.cleanupBySkill(player, oldSkillId)` giải phóng toàn bộ tài nguyên skill cũ.
     3. Lưu `newSkillId` vào slot tương ứng (`activeSkillSlot1` hoặc `activeSkillSlot2`).
     4. Gọi `newSkill.onEquip(player, slot)` cho kỹ năng mới.
3. **Thi triển (Phím R / X)**:
   - Gửi `ServerboundCastActiveSkillPacket(slotIndex)`.
   - Server tính: $\text{Cooldown} = \text{BaseCD} \times (1.0 - \text{CDR})$, giới hạn CDR tối đa 90%.
   - Kích hoạt logic qua `IActiveSkill.trigger(player)`.

---

## 3. Passive Slot System

- **Giới hạn**: Chỉ được trang bị **tối đa 1 Passive Skill** tại 1 thời điểm.
- **Phân loại**: Thẻ passive skill là thẻ không có effect thuộc `ACTIVE_SKILLS` và có ít nhất 1 effect không thuộc `REGISTERED_STATS`.
- **Trang bị & Thay thế**:
  - Khi chưa có passive nào: chọn thẻ sẽ tự động lưu vào `passiveSkillSlot`, kích hoạt `newSkill.onEquip(player, starLevel)`.
  - Khi đã có passive: `CardSelectionScreen` hiện dialog cảnh báo:
    - `"⚠ PASSIVE SLOT OCCUPIED"`
    - Nút `[ Confirm Replace ]` gửi `ServerboundSelectCardPacket(cardId, 3)`.
    - Server kích hoạt `oldSkill.onUnequip(player)`, dọn dẹp qua `CardResourceManager.cleanupBySkill(player, oldCardId)`, gỡ bỏ thẻ passive cũ khỏi `chosenCards`, và nạp passive mới (`newSkill.onEquip`).
- **Hiển thị (Phím L)**:
  - Tab `UPGRADES` hiển thị ô Passive Slot ở giữa mục Active Skills và Passive Upgrades.

---

## 4. Scaling Formula Cheatsheet

| Cấp sao (Star Level) | `@CardParamUp` Multiplier | `@CardParamDown` Multiplier |
| :---: | :---: | :---: |
| **1★** | `1.00` | `1.00` |
| **2★** | `1.15` | `0.85` |
| **3★** | `1.35` | `0.65` |
| **4★** | `1.55` | `0.45` |
| **5★** | `1.70` | `0.30` |

---

## 5. Hướng Dẫn Chuẩn Dành Cho AI Khi Viết/Refactor Thẻ Kỹ Năng

Khi các AI trợ lý khác tiến hành viết mới hoặc viết lại (refactor) thẻ kỹ năng trong các extension (như `SkillCardExtension`), **BẮT BUỘC tuân thủ các quy tắc sau**:

### Quy Tắc Vàng (KHÔNG vi phạm):
1. **KHÔNG tự duy trì các `static Map<UUID, ...>` vô hạn mà không có cơ chế dọn dẹp**.
2. **KHÔNG lưu cứng reference trực tiếp tới `Entity` hay `ServerLevel` trong static collection** (sẽ gây rò rỉ chunk và world). Dùng `UUID` hoặc `WeakReference`.
3. **KHÔNG tự viết `@SubscribeEvent` cho `PlayerLoggedOutEvent`, `PlayerChangedDimensionEvent`, `EntityLeaveLevelEvent`, `LivingDeathEvent`, `ServerStoppedEvent` trong addon**. CardEngine đã xử lý toàn bộ:
   - `PlayerLoggedOutEvent` → `CardResourceManager.cleanupByPlayer(uuid)`
   - `PlayerChangedDimensionEvent` → `CardResourceManager.cleanupByPlayer(uuid)`
   - `EntityLeaveLevelEvent` / `LivingDeathEvent` → `CardResourceManager.cleanupByEntity(uuid)`
   - `ServerStoppedEvent` → `CardResourceManager.cleanupAll()`
   - `ServerTickEvent` mỗi 100 ticks (5s) → `CardResourceManager.tickTtl(tick)` quét TTL hết hạn
4. **Luôn sử dụng `CardResourceManager.bind...()` ngay khi sinh ra tài nguyên tạm**.
5. **Khi tháo thẻ (`onUnequip`)**: CardEngine **tự động** gọi `CardResourceManager.cleanupBySkill(player, skillId)` sau `onUnequip` — dev chỉ cần dọn local cache nếu có, không cần gọi cleanup thủ công.

---

### Mẫu 1: Active Skill đơn giản (Không có tài nguyên tạm kéo dài)
Dùng lambda ngắn gọn:
```java
CardRegistry.registerActiveSkill("mymod:dash", 100, player -> {
    Vec3 look = player.getLookAngle();
    player.setDeltaMovement(look.x * 1.5, 0.2, look.z * 1.5);
    player.hurtMarked = true;
});
CardRegistry.registerASkillCard("mymod:dash_card", "mymod:dash");
```

---

### Mẫu 2: Active Skill phức tạp (Có Summoned Entity / Hẹn giờ / Trạng thái đệm)
Tạo class riêng implements [IActiveSkill.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/IActiveSkill.java):

```java
package com.myextension.skills.active;

import net.com.cardengine.api.cleanup.CardResourceManager;
import net.com.cardengine.api.registries.IActiveSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MySummonOrDetonationSkill implements IActiveSkill {

    public static final String SKILL_ID = "mymod:my_skill";
    
    // Nếu có state theo dõi cục bộ:
    private final List<CustomBombData> activeBombs = new CopyOnWriteArrayList<>();

    @Override
    public void trigger(ServerPlayer player) {
        // 1. Ràng buộc list trạng thái với CardEngine (tự clear khi tháo thẻ):
        CardResourceManager.bindState(player, SKILL_ID, activeBombs);

        // 2. Nếu triệu hồi Entity (tự discard khi tháo thẻ hoặc chết):
        Entity minion = spawnMinion(player);
        CardResourceManager.bindEntity(player, SKILL_ID, minion);

        // 3. Nếu đánh dấu NBT lên quái (tự gỡ NBT khi quái chết hoặc tháo thẻ):
        CardResourceManager.bindEntityNbt(targetMob, "my_custom_tag", SKILL_ID);

        // 4. Nếu có task hẹn giờ hoặc TTL:
        CardResourceManager.bindTask(player, SKILL_ID, () -> {
            // Task cleanup riêng nếu cần
        }, 400); // 400 ticks = 20 giây TTL
    }

    @Override
    public void onEquip(ServerPlayer player, int slot) {
        // Khởi tạo các modifier, HUD cache nếu cần
    }

    @Override
    public void onUnequip(ServerPlayer player, int slot) {
        // CardEngine sẽ tự động kích hoạt CardResourceManager.cleanupBySkill()!
        // Dev chỉ cần xử lý logic phụ trợ đặc thù (nếu có):
        activeBombs.clear();
    }
}

// Đăng ký trong mod setup:
CardRegistry.registerActiveSkill(MySummonOrDetonationSkill.SKILL_ID, 200, new MySummonOrDetonationSkill());
CardRegistry.registerASkillCard("mymod:my_skill_card", MySummonOrDetonationSkill.SKILL_ID);
```

---

### Mẫu 3: Passive Skill có trạng thái đếm hoặc hiệu ứng
Tạo class implements [IPassiveSkill.java](file:///d:/AMOD/CardEngine/src/main/java/net/com/cardengine/api/registries/IPassiveSkill.java):

```java
package com.myextension.skills.passive;

import net.com.cardengine.api.cleanup.CardResourceManager;
import net.com.cardengine.api.registries.IPassiveSkill;
import net.minecraft.server.level.ServerPlayer;

public class MyBuffPassiveSkill implements IPassiveSkill {

    public static final String CARD_ID = "mymod:my_passive_card";

    @Override
    public void onEquip(ServerPlayer player, int starLevel) {
        // Gắn attribute modifier hoặc kích hoạt buff
    }

    @Override
    public void onTick(ServerPlayer player, int starLevel) {
        // Chạy logic kiểm tra điều kiện mỗi 10 ticks
    }

    @Override
    public void onUnequip(ServerPlayer player) {
        // Gỡ bỏ attribute modifier
        // CardEngine tự động gọi CardResourceManager.cleanupBySkill(player, CARD_ID)
    }

    @Override
    public void onUpgrade(ServerPlayer player, int newStarLevel) {
        // Cập nhật lại sức mạnh buff theo cấp sao mới (1★ -> 5★)
    }
}

// Đăng ký:
CardRegistry.registerPassiveSkill(MyBuffPassiveSkill.CARD_ID, new MyBuffPassiveSkill());
CardRegistry.registerPSkillCard(MyBuffPassiveSkill.CARD_ID, "mymod:my_passive_mech");
```
