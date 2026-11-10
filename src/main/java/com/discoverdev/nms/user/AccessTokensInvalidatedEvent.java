package com.discoverdev.nms.user;

import java.util.UUID;

/** Published after a user's access-token version changes. */
public record AccessTokensInvalidatedEvent(UUID userId) {}
