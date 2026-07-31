package com.discoverdev.nms.dailylog;

/**
 * How the child seemed — a MOOD entry records exactly one of these.
 *
 * The label is what the teacher and parent apps show; storing the constant
 * rather than the words means either app can be reworded without the two
 * drifting apart, and without touching stored data.
 */
public enum MoodOption {
    HAPPY("Happy"),
    UPSET("Upset"),
    PLAYFUL("Playful"),
    FEELING_UNWELL("Feeling Unwell");

    private final String label;

    MoodOption(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
