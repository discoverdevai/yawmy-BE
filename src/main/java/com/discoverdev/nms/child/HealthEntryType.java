package com.discoverdev.nms.child;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The three per-child health lists (Jira YC-231 allergies, YC-232 immunizations, YC-233 medical notes). */
public enum HealthEntryType {
    ALLERGY("allergies"),
    IMMUNIZATION("immunizations"),
    MEDICAL_NOTE("medical-notes");

    private final String path;

    HealthEntryType(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static HealthEntryType fromPath(String path) {
        for (HealthEntryType t : values()) {
            if (t.path.equals(path)) return t;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown list: " + path);
    }

    /** The older single-text field on the child that mirrors this list (one entry per line). */
    String legacyText(Child c) {
        return switch (this) {
            case ALLERGY -> c.getAllergies();
            case IMMUNIZATION -> c.getImmunizations();
            case MEDICAL_NOTE -> c.getMedicalNotes();
        };
    }

    void setLegacyText(Child c, String text) {
        switch (this) {
            case ALLERGY -> c.setAllergies(text);
            case IMMUNIZATION -> c.setImmunizations(text);
            case MEDICAL_NOTE -> c.setMedicalNotes(text);
        }
    }
}
