package com.discoverdev.nms.user;

/**
 * Job title for a staff member (all are system role TEACHER). Exposed to the app
 * via GET /api/admin/staff/roles so the "add member" dropdown is driven by the backend.
 */
public enum StaffRole {
    LEAD_TEACHER("Lead Teacher"),
    ASSISTANT_TEACHER("Assistant Teacher"),
    CLASSROOM_TEACHER("Classroom Teacher"),
    SUBJECT_TEACHER("Subject Teacher"),
    NURSERY_SUPERVISOR("Nursery Supervisor"),
    TRAINEE_TEACHER("Trainee Teacher");

    private final String label;

    StaffRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
