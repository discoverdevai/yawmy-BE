package com.discoverdev.nms.dailylog;

/** How much of a meal the child ate. Answers "How much was eaten?". */
public enum MealAmount {
    ALL("All"),
    MOST("Most"),
    HALF("Half"),
    LITTLE("Little"),
    NONE("None");

    private final String label;

    MealAmount(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
