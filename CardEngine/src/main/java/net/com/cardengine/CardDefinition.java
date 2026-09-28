package net.com.cardengine;

import java.util.ArrayList;
import java.util.List;

public class CardDefinition {
    private String id;
    private String icon;
    private String rarity;
    private String required_class = "all";
    private List<String> description = new ArrayList<>();
    private List<EffectDefinition> effects = new ArrayList<>();
    private String custom_frame; // Path to custom frame texture if any (null by default)
    private String border_color; // Custom border hex color override (null by default)
    private int level = 1;
    private int max_stack = 5;
    private List<String> required_cards = new ArrayList<>();
    private String card_type = "stat";

    public CardDefinition() {}

    public CardDefinition(String id, String icon, String rarity, String required_class, List<String> description, List<EffectDefinition> effects) {
        this.id = id;
        this.icon = icon;
        this.rarity = rarity;
        this.required_class = required_class;
        this.description = description;
        this.effects = effects;
    }

    public CardDefinition(String id, String icon, String rarity, String required_class, List<String> description, List<EffectDefinition> effects, String custom_frame, String border_color) {
        this(id, icon, rarity, required_class, description, effects);
        this.custom_frame = custom_frame;
        this.border_color = border_color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getRequiredClass() {
        return required_class != null ? required_class : "all";
    }

    public void setRequiredClass(String required_class) {
        this.required_class = required_class;
    }

    public List<String> getDescription() {
        return description != null ? description : new ArrayList<>();
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public List<EffectDefinition> getEffects() {
        return effects != null ? effects : new ArrayList<>();
    }

    public void setEffects(List<EffectDefinition> effects) {
        this.effects = effects;
    }

    public String getCustomFrame() {
        return custom_frame;
    }

    public void setCustomFrame(String custom_frame) {
        this.custom_frame = custom_frame;
    }

    public String getBorderColor() {
        return border_color;
    }

    public void setBorderColor(String border_color) {
        this.border_color = border_color;
    }

    public CardRarity getRarityEnum() {
        return CardRarity.byName(rarity);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getMaxStack() {
        return max_stack;
    }

    public void setMaxStack(int max_stack) {
        this.max_stack = max_stack;
    }

    public List<String> getRequiredCards() {
        return required_cards != null ? required_cards : new ArrayList<>();
    }

    public void setRequiredCards(List<String> required_cards) {
        this.required_cards = required_cards;
    }

    public String getCardType() {
        return card_type != null ? card_type : "stat";
    }

    public void setCardType(String card_type) {
        this.card_type = card_type;
    }

    public CardType getCardTypeEnum() {
        return CardType.fromString(card_type);
    }
}
