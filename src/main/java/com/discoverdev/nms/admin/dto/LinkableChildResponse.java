package com.discoverdev.nms.admin.dto;

import java.util.UUID;

/** A child that can be linked to a parent (no guardian of the parent's role yet). */
public record LinkableChildResponse(
    UUID id,
    String fullName,
    String roomName,
    String photoUrl
) {}
