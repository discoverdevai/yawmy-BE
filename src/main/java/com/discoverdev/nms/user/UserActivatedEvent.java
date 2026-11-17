package com.discoverdev.nms.user;

import java.util.UUID;

/** Published when an invited user sets their password and their account becomes active. */
public record UserActivatedEvent(UUID userId, UUID nurseryId) {}
