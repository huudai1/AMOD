package net.com.cardengine;

public enum CardType {
    STAT("stat"),
    ACTIVE_SKILL("active_skill"),
    PASSIVE_SKILL("passive_skill");

    private final String id;

    CardType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static CardType fromString(String str) {
        if (str == null || str.isEmpty()) {
            return STAT;
        }
        String normalized = str.trim().toLowerCase();
        for (CardType type : values()) {
            if (type.id.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        if (normalized.contains("passive")) return PASSIVE_SKILL;
        if (normalized.contains("active") || normalized.contains("skill")) return ACTIVE_SKILL;
        return STAT;
    }
}
