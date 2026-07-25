package com.discoverdev.nms.config;

import com.discoverdev.nms.user.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-app invite link bases: linkBaseUrl → admin, teacherLinkBaseUrl → teacher,
 * parentLinkBaseUrl → parent.
 */
@ConfigurationProperties(prefix = "app.invite")
public record InviteProperties(
    long ttlHours,
    String linkBaseUrl,
    String teacherLinkBaseUrl,
    String parentLinkBaseUrl
) {

    /**
     * The invite link for a role, with the token appended.
     *
     * Every invite email must build its link through here. Each app registers its
     * own deep-link scheme, so a teacher who is emailed the admin app's link has
     * no way to open it — which is exactly what happens when a call site picks
     * the base URL by hand and picks the wrong one.
     */
    public String linkFor(Role role, String token) {
        String base = switch (role) {
            case TEACHER -> teacherLinkBaseUrl;
            case PARENT -> parentLinkBaseUrl;
            default -> linkBaseUrl;
        };
        String sep = base != null && base.contains("?") ? "&" : "?";
        return base + sep + "token=" + token;
    }
}
