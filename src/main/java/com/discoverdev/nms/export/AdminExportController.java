package com.discoverdev.nms.export;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/export")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExportController {

    private static final String XLSX_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final AdminExportService service;

    public AdminExportController(AdminExportService service) {
        this.service = service;
    }

    @GetMapping("/children")
    public ResponseEntity<byte[]> children(
        @RequestParam(value = "childIds", required = false) List<UUID> childIds,
        @RequestParam(value = "roomIds", required = false) List<UUID> roomIds
    ) {
        byte[] data = service.childrenXlsx(childIds, roomIds);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(XLSX_TYPE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=child-records.xlsx")
            .body(data);
    }

    @GetMapping("/attendance")
    public ResponseEntity<byte[]> attendance(
        @RequestParam(value = "childIds", required = false) List<UUID> childIds,
        @RequestParam(value = "roomIds", required = false) List<UUID> roomIds,
        @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(value = "statuses", required = false) List<String> statuses
    ) {
        byte[] data = service.attendanceXlsx(childIds, roomIds, from, to, statuses);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(XLSX_TYPE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-logs.xlsx")
            .body(data);
    }

    @GetMapping("/incidents")
    public ResponseEntity<byte[]> incidents(
        @RequestParam(value = "childIds", required = false) List<UUID> childIds,
        @RequestParam(value = "roomIds", required = false) List<UUID> roomIds,
        @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] data = service.incidentsXlsx(childIds, roomIds, from, to);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(XLSX_TYPE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incident-log.xlsx")
            .body(data);
    }
}
