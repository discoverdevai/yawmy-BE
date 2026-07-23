package com.discoverdev.nms.schedule;

import com.discoverdev.nms.schedule.dto.CreateSessionRequest;
import com.discoverdev.nms.schedule.dto.SessionResponse;
import com.discoverdev.nms.schedule.dto.UpdateSessionRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/admin/rooms/{roomId}/sessions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomSessionController {

    private final AdminRoomSessionService service;

    public AdminRoomSessionController(AdminRoomSessionService service) {
        this.service = service;
    }

    /** Sessions for the room on a given day (?date=YYYY-MM-DD). */
    @GetMapping
    public List<SessionResponse> list(
        @PathVariable UUID roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return service.list(roomId, date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(
        @PathVariable UUID roomId,
        @Valid @RequestBody CreateSessionRequest req
    ) {
        return service.create(roomId, req);
    }

    @PatchMapping("/{sessionId}")
    public SessionResponse update(
        @PathVariable UUID roomId,
        @PathVariable UUID sessionId,
        @Valid @RequestBody UpdateSessionRequest req
    ) {
        return service.update(roomId, sessionId, req);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID roomId, @PathVariable UUID sessionId) {
        service.delete(roomId, sessionId);
    }
}
