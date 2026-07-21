package com.discoverdev.nms.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * capacity defaults to 20 when omitted. teacherId (optional) assigns a teacher
 * to the room on creation — pick one from GET /api/admin/teachers.
 */
public record CreateRoomRequest(
    @NotBlank @Size(max = 100) String name,
    @Min(1) Integer capacity,
    UUID teacherId
) {}
