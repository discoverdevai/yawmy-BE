package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.CreateRoomRequest;
import com.discoverdev.nms.admin.dto.RoomResponse;
import com.discoverdev.nms.admin.dto.UpdateRoomRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rooms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    public AdminRoomController(AdminRoomService adminRoomService) {
        this.adminRoomService = adminRoomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest req) {
        return adminRoomService.create(req);
    }

    @GetMapping
    public List<RoomResponse> list(
        @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived
    ) {
        return adminRoomService.list(includeArchived);
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable UUID id) {
        return adminRoomService.get(id);
    }

    @PatchMapping("/{id}")
    public RoomResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoomRequest req) {
        return adminRoomService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public RoomResponse archive(@PathVariable UUID id) {
        return adminRoomService.archive(id);
    }

    @PostMapping("/{id}/restore")
    public RoomResponse restore(@PathVariable UUID id) {
        return adminRoomService.restore(id);
    }
}
