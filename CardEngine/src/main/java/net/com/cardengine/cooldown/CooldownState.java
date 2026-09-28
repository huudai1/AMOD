package net.com.cardengine.cooldown;

public class CooldownState {
    private int remainingTicks;
    private int maxTicks;

    public CooldownState(int ticks) {
        this.remainingTicks = ticks;
        this.maxTicks = ticks;
    }

    public CooldownState(int remainingTicks, int maxTicks) {
        this.remainingTicks = remainingTicks;
        this.maxTicks = maxTicks;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void setRemainingTicks(int ticks) {
        this.remainingTicks = ticks;
    }

    public int getMaxTicks() {
        return maxTicks;
    }

    public void setMaxTicks(int ticks) {
        this.maxTicks = ticks;
    }

    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }
}
