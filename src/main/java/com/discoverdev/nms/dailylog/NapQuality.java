package com.discoverdev.nms.dailylog;

/** How the nap went. Answers "How was the nap?". */
public enum NapQuality {
    SLEPT_WELL("Slept Well"),
    LIGHT_SLEEP("Light Sleep"),
    RESTLESS("Restless"),
    LITTLE("Little");

    private final String label;

    NapQuality(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
