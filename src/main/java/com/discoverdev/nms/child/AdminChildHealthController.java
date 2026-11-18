package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.HealthEntryRequest;
import com.discoverdev.nms.child.dto.HealthEntryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * A child's allergies, immunization records and medical notes as lists of entries
 * (Jira YC-231, YC-232, YC-233): /api/admin/children/{childId}/{allergies|immunizations|medical-notes}.
 */
@RestController
@RequestMapping("/api/admin/children/{childId}/{list:allergies|immunizations|medical-notes}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChildHealthController {

    private final ChildHealthService service;

    public AdminChildHealthController(ChildHealthService service) {
        this.service = service;
    }

    @GetMapping
    public List<HealthEntryResponse> list(@PathVariable UUID childId, @PathVariable String list) {
        return service.list(childId, HealthEntryType.fromPath(list));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HealthEntryResponse add(
        @PathVariable UUID childId, @PathVariable String list, @Valid @RequestBody HealthEntryRequest req
    ) {
        return service.add(childId, HealthEntryType.fromPath(list), req);
    }

    @PatchMapping("/{entryId}")
    public HealthEntryResponse update(
        @PathVariable UUID childId, @PathVariable String list, @PathVariable UUID entryId,
        @Valid @RequestBody HealthEntryRequest req
    ) {
        return service.update(childId, HealthEntryType.fromPath(list), entryId, req);
    }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID childId, @PathVariable String list, @PathVariable UUID entryId) {
        service.delete(childId, HealthEntryType.fromPath(list), entryId);
    }
}
