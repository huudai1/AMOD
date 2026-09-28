package net.com.cardengine;

public enum CardRarity {
    COMMON("common", 50.0, 0x9E9E9E),
    UNCOMMON("uncommon", 25.0, 0x4CAF50),
    RARE("rare", 13.0, 0x2196F3),
    EPIC("epic", 7.0, 0x9C27B0),
    LEGENDARY("legendary", 3.5, 0xFF9800),
    MYTHIC("mythic", 1.0, 0xF44336),
    SPECIAL("special", 0.5, -1);

    private final String name;
    private final double defaultWeight;
    private final int defaultColor;

    CardRarity(String name, double defaultWeight, int defaultColor) {
        this.name = name;
        this.defaultWeight = defaultWeight;
        this.defaultColor = defaultColor;
    }

    public String getName() {
        return name;
    }

    public double getDefaultWeight() {
        return defaultWeight;
    }

    public int getColor() {
        if (this == SPECIAL) {
            // Calculate rainbow color dynamically using time
            long time = System.currentTimeMillis();
            float hue = (time % 4000) / 4000.0f; // cycle every 4 seconds
            return java.awt.Color.HSBtoRGB(hue, 0.8f, 0.9f) & 0xFFFFFF;
        }
        return defaultColor;
    }

    public static CardRarity byName(String name) {
        for (CardRarity rarity : values()) {
            if (rarity.name.equalsIgnoreCase(name)) {
                return rarity;
            }
        }
        return COMMON;
    }
}
