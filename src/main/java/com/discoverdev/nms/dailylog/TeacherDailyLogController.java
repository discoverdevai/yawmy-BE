package com.discoverdev.nms.dailylog;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.dailylog.dto.ChildLogStatusResponse;
import com.discoverdev.nms.dailylog.dto.CreateLogEntryRequest;
import com.discoverdev.nms.dailylog.dto.DailyLogResponse;
import com.discoverdev.nms.dailylog.dto.DailyLogSummaryResponse;
import com.discoverdev.nms.dailylog.dto.LogEntryResponse;
import com.discoverdev.nms.dailylog.dto.PublishRequest;
import com.discoverdev.nms.dailylog.dto.UpdateLogEntryRequest;
import com.discoverdev.nms.dailylog.dto.UpsertDailyLogRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherDailyLogController {

    private final TeacherDailyLogService service;

    public TeacherDailyLogController(TeacherDailyLogService service) {
        this.service = service;
    }

    /**
     * Save a child's whole log for one date — what the app's "Save as Draft"
     * and "Publish" buttons call. Replaces the day's entries, so a save that
     * failed on poor wifi can be retried as-is.
     */
    @PutMapping("/children/{childId}/daily-logs/{date}")
    public DailyLogResponse upsertLog(
        @PathVariable UUID childId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Valid @RequestBody UpsertDailyLogRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.upsertLog(childId, date, req, principal.getUser());
    }

    @PostMapping("/children/{childId}/log-entries")
    @ResponseStatus(HttpStatus.CREATED)
    public LogEntryResponse addEntry(
        @PathVariable UUID childId,
        @Valid @RequestBody CreateLogEntryRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.addEntry(childId, req, principal.getUser());
    }

    @PatchMapping("/log-entries/{entryId}")
    public LogEntryResponse updateEntry(
        @PathVariable UUID entryId,
        @Valid @RequestBody UpdateLogEntryRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.updateEntry(entryId, req, principal.getUser());
    }

    @DeleteMapping("/log-entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(@PathVariable UUID entryId, @AuthenticationPrincipal AppUserDetails principal) {
        service.deleteEntry(entryId, principal.getUser());
    }

    @PostMapping("/log-entries/{entryId}/publish")
    public LogEntryResponse publishEntry(
        @PathVariable UUID entryId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.publishEntry(entryId, principal.getUser());
    }

    @PostMapping("/log-entries/{entryId}/unpublish")
    public LogEntryResponse unpublishEntry(
        @PathVariable UUID entryId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.unpublishEntry(entryId, principal.getUser());
    }

    @PostMapping("/daily-logs/{logId}/publish")
    public DailyLogResponse publishLog(
        @PathVariable UUID logId,
        @RequestBody(required = false) PublishRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.publishLog(logId, req, principal.getUser());
    }

    @GetMapping("/daily-logs/{logId}")
    public DailyLogResponse getLog(@PathVariable UUID logId, @AuthenticationPrincipal AppUserDetails principal) {
        return service.getById(logId, principal.getUser());
    }

    /**
     * Every enrolled child in the room with their log state for the date —
     * NOT_STARTED / DRAFT / COMPLETED. Backs the Logs screen list.
     */
    @GetMapping("/rooms/{roomId}/daily-logs/status")
    public List<ChildLogStatusResponse> roomLogStatus(
        @PathVariable UUID roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.roomLogStatus(roomId, date, principal.getUser());
    }

    @GetMapping("/children/{childId}/daily-logs")
    public ResponseEntity<DailyLogResponse> getLogForChild(
        @PathVariable UUID childId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.getByChildAndDate(childId, date, principal.getUser())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/rooms/{roomId}/daily-logs")
    public List<DailyLogSummaryResponse> roomFeed(
        @PathVariable UUID roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listRoomFeed(roomId, date, principal.getUser());
    }
}
