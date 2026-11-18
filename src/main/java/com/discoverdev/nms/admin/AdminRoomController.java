package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.ArchiveRoomRequest;
import com.discoverdev.nms.admin.dto.CreateRoomRequest;
import com.discoverdev.nms.admin.dto.RoomResponse;
import com.discoverdev.nms.admin.dto.UpdateRoomRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin/rooms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final AdminRoomService adminRoomService;
    private final RoomDeletionService roomDeletionService;

    public AdminRoomController(AdminRoomService adminRoomService, RoomDeletionService roomDeletionService) {
        this.adminRoomService = adminRoomService;
        this.roomDeletionService = roomDeletionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest req) {
        return adminRoomService.create(req);
    }

    @GetMapping
    public List<RoomResponse> list(
        @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "search", required = false) String search
    ) {
        // status=active|archived (Jira YC-84) wins; otherwise includeArchived keeps its old meaning.
        if (status != null && !status.equalsIgnoreCase("active") && !status.equalsIgnoreCase("archived")) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "status must be 'active' or 'archived'");
        }
        String effective = status != null ? status.toLowerCase() : (includeArchived ? null : "active");
        return adminRoomService.list(effective, search);
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable UUID id) {
        return adminRoomService.get(id);
    }

    @PatchMapping("/{id}")
    public RoomResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoomRequest req) {
        return adminRoomService.update(id, req);
    }

    /**
     * Plain DELETE archives the room (what the admin app uses today). With permanent=true it is
     * Jira's Delete Room (YC-90): only a room that never had children or activity, else 409.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<RoomResponse> archive(
        @PathVariable UUID id,
        @RequestParam(name = "permanent", defaultValue = "false") boolean permanent
    ) {
        if (permanent) {
            roomDeletionService.delete(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(adminRoomService.archive(id));
    }

    /**
     * Archive a room that still has children: reassign every child to another
     * room and archive the source room in one atomic call.
     */
    @PostMapping("/{id}/archive")
    public RoomResponse archiveWithReassignment(
        @PathVariable UUID id,
        @RequestBody ArchiveRoomRequest req
    ) {
        return adminRoomService.archiveWithReassignment(id, req);
    }

    @PostMapping("/{id}/restore")
    public RoomResponse restore(@PathVariable UUID id) {
        return adminRoomService.restore(id);
    }
}
