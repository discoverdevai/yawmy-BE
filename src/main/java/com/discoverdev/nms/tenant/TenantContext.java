package com.discoverdev.nms.tenant;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.user.User;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantContext {

    private TenantContext() {}

    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return details.getUser();
    }

    public static UUID currentNurseryId() {
        Nursery nursery = currentUser().getNursery();
        if (nursery == null) {
            throw new IllegalStateException("Authenticated user is not attached to a nursery");
        }
        return nursery.getId();
    }
}