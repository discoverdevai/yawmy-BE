package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.user.StaffRole;

/** A staff-role option for the "add member" dropdown: {value} sent back, {label} shown. */
public record StaffRoleOption(String value, String label) {
    public static StaffRoleOption of(StaffRole r) {
        return new StaffRoleOption(r.name(), r.getLabel());
    }
}
