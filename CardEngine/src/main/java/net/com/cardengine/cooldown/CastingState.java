package net.com.cardengine.cooldown;

public class CastingState {
    private final String skillId;
    private final int slot;
    private final int totalTicks;
    private int remainingTicks;

    public CastingState(String skillId, int slot, int totalTicks) {
        this.skillId = skillId != null ? skillId : "";
        this.slot = slot;
        this.totalTicks = Math.max(1, totalTicks);
        this.remainingTicks = this.totalTicks;
    }

    public CastingState(String skillId, int slot, int totalTicks, int remainingTicks) {
        this.skillId = skillId != null ? skillId : "";
        this.slot = slot;
        this.totalTicks = Math.max(1, totalTicks);
        this.remainingTicks = Math.max(0, remainingTicks);
    }

    public String getSkillId() {
        return skillId;
    }

    public int getSlot() {
        return slot;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public boolean isFinished() {
        return remainingTicks <= 0;
    }

    public float getProgress() {
        if (totalTicks <= 0) return 1.0F;
        return 1.0F - ((float) remainingTicks / (float) totalTicks);
    }
}
