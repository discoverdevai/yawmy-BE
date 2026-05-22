package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ChildResponse;
import com.discoverdev.nms.child.dto.CreateChildRequest;
import com.discoverdev.nms.child.dto.ReassignRoomRequest;
import com.discoverdev.nms.child.dto.UpdateChildRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/children")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChildController {

    private final AdminChildService service;

    public AdminChildController(AdminChildService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChildResponse create(@Valid @RequestBody CreateChildRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<ChildResponse> list(
        @RequestParam(name = "roomId", required = false) UUID roomId,
        @RequestParam(name = "includeUnenrolled", defaultValue = "false") boolean includeUnenrolled
    ) {
        return service.list(roomId, includeUnenrolled);
    }

    @GetMapping("/{id}")
    public ChildResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public ChildResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateChildRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/room")
    public ChildResponse reassignRoom(@PathVariable UUID id, @RequestBody ReassignRoomRequest req) {
        return service.reassignRoom(id, req);
    }

    @PostMapping("/{id}/unenroll")
    public ChildResponse unenroll(@PathVariable UUID id) {
        return service.unenroll(id);
    }

    @PostMapping("/{id}/restore")
    public ChildResponse restore(@PathVariable UUID id) {
        return service.restore(id);
    }
}
