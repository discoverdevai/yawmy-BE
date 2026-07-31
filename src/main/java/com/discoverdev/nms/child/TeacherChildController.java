package com.discoverdev.nms.child;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.child.dto.ChildResponse;
import com.discoverdev.nms.child.dto.EmergencyContactResponse;
import com.discoverdev.nms.child.dto.PickupResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of a child for the teacher app's child profile.
 *
 * The same records the admin app manages, but teachers only read them, and only
 * for children in a room they are assigned to — enforced per request rather
 * than by role alone, so a teacher cannot reach a child in another room.
 */
@RestController
@RequestMapping("/api/teacher/children/{childId}")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherChildController {

    private final TeacherChildService service;

    public TeacherChildController(TeacherChildService service) {
        this.service = service;
    }

    /** Name, photo, date of birth, room, and the medical fields. */
    @GetMapping
    public ChildResponse get(
        @PathVariable UUID childId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.getChild(childId, principal.getUser());
    }

    /** Who to call in an emergency, oldest first. */
    @GetMapping("/emergency-contacts")
    public List<EmergencyContactResponse> emergencyContacts(
        @PathVariable UUID childId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.emergencyContacts(childId, principal.getUser());
    }

    /** Who is allowed to collect the child, oldest first. */
    @GetMapping("/pickups")
    public List<PickupResponse> pickups(
        @PathVariable UUID childId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.pickups(childId, principal.getUser());
    }
}
