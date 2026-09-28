package net.com.cardengine;

public class EffectDefinition {
    private String type;
    private double value;
    private java.util.Map<String, Double> params = new java.util.HashMap<>();

    public EffectDefinition() {}

    public EffectDefinition(String type, double value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public java.util.Map<String, Double> getParams() {
        return params;
    }

    public void setParams(java.util.Map<String, Double> params) {
        this.params = params;
    }
}
