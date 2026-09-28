# System Map - Skill Card Extension Reference Manual

Tài liệu tham chiếu thiết kế và hướng dẫn tích hợp dành cho Lập trình viên và AI Assistants về mod mở rộng **Skill Card Extension** (`skillcard_extension`).

> **[!IMPORTANT]**
> Moi quy tac ve **Lifecycle, Cleanup, CardResourceManager, IActiveSkill/IPassiveSkill** deu nam trong **[CardEngine/SystemMap.md](file:///d:/AMOD/CardEngine/SystemMap.md) — Section 5** — doc do truoc khi viet/sua bat ky skill nao.

---

## 1. Codebase File Map

- [SkillCardMod.java](file:///d:/AMOD/SkillCardExtension/src/main/java/com/skillcard/extension/SkillCardMod.java): Mod entry point, khoi tao `PotionStackHandler`, dang ky 28 ky nang vao `commonSetup`, lang nghe server tick (SkillStateManager, TrackingArrow, DelayedDetonation, DeferredRetribution, ThunderField, StormCall, MeteorShower, MeteorStrike), va client tick (Light Breeze glide).
- [PotionStackHandler.java](file:///d:/AMOD/SkillCardExtension/src/main/java/com/skillcard/extension/PotionStackHandler.java): Reflection handler mo rong `maxStackSize = 16` cho moi loai binh thuoc (Potion, Splash, Lingering).
- [SkillCardCommand.java](file:///d:/AMOD/SkillCardExtension/src/main/java/com/skillcard/extension/SkillCardCommand.java): Lenh `/skillcard <skillId> <slot>` (OP level 2) de trang bi truc tiep skill vao slot 1/2 dung cho testing; autocomplete danh sach skill tu `CardRegistry.ACTIVE_SKILLS`.
- [SkillBase.java](file:///d:/AMOD/SkillCardExtension/src/main/java/com/skillcard/extension/skills/SkillBase.java): Lop co so cung cap cac helper dang ky:
  - `registerSkill(skillId, cdTicks, castTimeTicks, logic)`: Dang ky active skill.
  - `registerCard(cardId, skillId, icon, desc...)`: Dang ky the bai gan active skill.
  - `registerPassiveCard(cardId, icon, desc...)`: Dang ky the bai passive skill doc lap (khong cooldown, khong active keybind).
  - `hasSkill(player, skillId)` / `hasCard(player, cardId)`: Kiem tra nguoi choi da so huu ky nang/the bai.
  - `getSkillLevel(player, skillId)`: Lay cap sao (1 - 5*).
  - `getUpScale(player, skillId)` / `getDownScale(player, skillId)`: Lay multiplier scale theo cap sao.
- [SkillStateManager.java](file:///d:/AMOD/SkillCardExtension/src/main/java/com/skillcard/extension/skills/SkillStateManager.java): Luu trang thai tam thoi **ngan han giua cac tick server** dang `Map<UUID, Long>` (expiry timestamp ms) va `Map<UUID, UUID>` (target lock). **Chi dung cho trang thai logic thuan** (thoi han hieu ung, lock muc tieu) — **KHONG** dung de luu Entity reference hay NBT (dung `CardResourceManager` cho nhung thu do). `tickCleanup()` duoc goi moi server tick tu `SkillCardMod`.
- Subpackages:
  - `com.skillcard.extension.skills.active`: 18 ky nang chu dong.
  - `com.skillcard.extension.skills.passive`: 10 ky nang bi dong.

---

## 2. Danh Sach 28 Ky Nang (All Skills)

### Active Skills (18 Ky Nang - Package `skills.active`)
1. `TailwindSkill` (`skillcard_extension:tailwind`): Luot nhanh ve phia truoc. CD: 5s.
2. `UpdraftSkill` (`skillcard_extension:updraft`): Phong len khong trung. CD: 6s.
3. `AdrenalineRushSkill` (`skillcard_extension:adrenaline_rush`): Tang toc do di chuyen dot bien trong 2s (toc do va thoi gian tang theo sao). CD: 10s.
4. `SonicBoomSkill` (`skillcard_extension:sonic_boom`): Song am Warden xuyen giap 18 block + Darkness. CD: 15s.
5. `RapidBarrageSkill` (`skillcard_extension:rapid_barrage`): Keo cung nhanh, xac suat mua cau lua no. CD: 60s.
6. `ShadowVeilSkill` (`skillcard_extension:shadow_veil`): Tang hinh hoan toan va xoa aggro quai vat. CD: 10s.
7. `HeavyDrawSkill` (`skillcard_extension:heavy_draw`): Ten dam xuyen tuyet doi + x3 sat thuong. CD: 30s.
8. `ScatterShotSkill` (`skillcard_extension:scatter_shot`): Ban chum 5 mui ten hinh quat. CD: 30s.
9. `IronBastionSkill` (`skillcard_extension:iron_bastion`): Giam manh sat thuong nhan vao (Resistance V). CD: 30s.
10. `TrackingArrowSkill` (`skillcard_extension:tracking_arrow`): Danh dau va tu dong uon luon ten duoi theo muc tieu. CD: 30s.
11. `DelayedDetonationSkill` (`skillcard_extension:delayed_detonation`): Luu vet ban trung va kich no dong loat khi het gio. CD: 30s.
12. `BeneficialPotionSkill` (`skillcard_extension:beneficial_potion`): Nem 1-5 lo thuoc co loi theo cap sao. CD: 15s.
13. `HarmfulPotionSkill` (`skillcard_extension:harmful_potion`): Nem 1-5 lo thuoc co hai theo cap sao. CD: 15s.
14. `DeferredRetributionSkill` (`skillcard_extension:deferred_retribution`): Khac Dau An Tu Than, don sat thuong 2-10s roi no 1.25x. CD: 20s.
15. `ThunderFieldSkill` (`skillcard_extension:thunder_field`): Vong tron tich tu 5s quanh than, giang set toan bo quai vat/muc tieu trong vung. CD: 5 phut.
16. `StormCallSkill` (`skillcard_extension:storm_call`): Trieu hoi mua bao toan the gioi trong 5 phut; cu 10s giang set muc tieu dang danh hoac quai gan nhat trong 60s. CD: 20 phut.
17. `ParrySkill` (`skillcard_extension:parry`): The thu gio kiem trong 0.5s; do don thanh cong se do luc chop nhoang, hat kiem kem tieng de & tieng khien, phan don 100% chi mang, hat vang quai va hoi chieu ngay lap tuc (do hut CD 5s). CD: 5s.
18. `MeteorStrikeSkill` (`skillcard_extension:meteor_strike`): Khoa vi tri dung tao Vong Tron Ma Thuat Do ruc dem nguoc 5s; sau 5s khoi Magma khong lo xoay tit lao am am tu troi cao cam thang xuong dat; gay no pha block, rung chan camera, boc chay dien rong va sat thuong AoE cuc lon (30 - 51 DMG). CD: 30s.

### Passive Skills (10 Ky Nang - Package `skills.passive`)
19. `LightBreezeSkill` (`skillcard_extension:light_breeze`): Giu SPACE tren khong trung de luot nhe va mien sat thuong roi.
20. `PoisonMasterySkill` (`skillcard_extension:poison_mastery`): Tu dong nang cap cac loai potion sang tier cao hon (Poison/Harming/Slowness/Weakness -> Strong/Long).
21. `DamageStaggerSkill` (`skillcard_extension:damage_stagger`): Chuyen hoa sat thuong nhan vao thanh be Stagger rut dan qua Wither II.
22. `FleshOverSteelSkill` (`skillcard_extension:flesh_over_steel`): Triet tieu giap ve 0, chuyen doi thanh mau toi da khong lo.
23. `OverhealTransfusionSkill` (`skillcard_extension:overheal_transfusion`): Giam 50% mau toi da, chuyen moi luong hoi mau du thanh mau vang.
24. `EnergyShieldSkill` (`skillcard_extension:energy_shield`): Triet tieu giap, tao khien mau vang tu sac lien tuc.
25. `StormConduitSkill` (`skillcard_extension:storm_conduit`): Khi troi Mua hoac Mua Bao: +50% sat thuong set do nguoi choi gay ra; set danh gay them Thieu Dot 3s va No Nhe khong pha block.
26. `FatmanSkill` (`skillcard_extension:fatman`): Khi di chuyen va cham ke dich gay sat thuong va cham & hat lui; ban than nhan 100% phan phe (giam theo sao); moi sat thuong khac giam 50% (giam phat theo sao).
27. `RhythmStrikerSkill` (`skillcard_extension:rhythm_striker`): Giam 80% moi nguon sat thuong nhan vao. Sau don danh, dem cho 5.0s; danh trung trong 1.0s tiep theo tang +50% DMG (toi da 10 stack = +500% DMG). Tai stack 10: toan bo don danh chuyen thanh Sat Thuong Chuan (True Damage - xuyen giap, khang). Danh voi khi dang cho 5s bi tru 1 stack.
28. `MeteorShowerSkill` (`skillcard_extension:meteor_shower`): Khi ban ten, co ty le (3% - 15% ngay, 6% - 30% dem theo sao) kich hoat Mua Sao Bang Neon gom 5 thien thach roi cheo xuong khu vuc va cham, gay sat thuong AoE dien rong va no chum hat Neon ruc ro.

---

## 3. Huong Dan Them Ky Nang Moi (Guide for AI & Dev)

Xem toan bo quy tac lifecycle, cleanup, mau code chuan tai **[CardEngine/SystemMap.md Section 5](file:///d:/AMOD/CardEngine/SystemMap.md)**.

### Ky Nang Chu Dong -- Lambda (Don Gian, khong co tai nguyen tam)
```java
public class MyActiveSkill extends SkillBase {
    public static final String SKILL_ID = "skillcard_extension:my_skill";
    public static final String CARD_ID  = "skillcard_extension:my_skill_card";

    @CardParamDown
    public static int COOLDOWN_TICKS = 300; // 15s

    public static void register() {
        registerSkill(SKILL_ID, COOLDOWN_TICKS, MyActiveSkill::trigger);
        registerCard(CARD_ID, SKILL_ID, "minecraft:iron_sword",
                "Mu ta tinh nang ngan gon.",
                "Cap sao tang hieu luc.");
    }

    private static void trigger(ServerPlayer player) {
        // Logic thuc thi ky nang
    }
}
```

### Ky Nang Chu Dong -- Class (Phuc Tap, co Entity / Trang thai / TTL)
Khi skill tao Entity, danh dau NBT, hoac can `onEquip`/`onUnequip` => implements IActiveSkill (xem mau day du o CardEngine/SystemMap.md Mau 2).

### Ky Nang Bi Dong (Passive Skill)
```java
public class MyPassiveSkill extends SkillBase {
    public static final String CARD_ID = "skillcard_extension:my_passive_card";

    public static void register() {
        registerPassiveCard(CARD_ID, "minecraft:nether_star",
                "My Passive (Passive)",
                "Mo ta hieu ung bi dong.");
    }
}
```
Xem mau `IPassiveSkill` day du voi `onEquip`, `onTick`, `onUnequip`, `onUpgrade` tai **[CardEngine/SystemMap.md Mau 3](file:///d:/AMOD/CardEngine/SystemMap.md)**.

