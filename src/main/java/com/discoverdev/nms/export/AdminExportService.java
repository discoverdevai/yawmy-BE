package com.discoverdev.nms.export;

import com.discoverdev.nms.attendance.AttendanceDates;
import com.discoverdev.nms.attendance.AttendanceEvent;
import com.discoverdev.nms.attendance.AttendanceEventRepository;
import com.discoverdev.nms.attendance.AttendanceEventType;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.incident.IncidentReport;
import com.discoverdev.nms.incident.IncidentReportRepository;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.tenant.TenantContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminExportService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final ChildRepository childRepository;
    private final NurseryRepository nurseryRepository;
    private final AttendanceEventRepository eventRepository;
    private final IncidentReportRepository incidentRepository;

    public AdminExportService(
        ChildRepository childRepository,
        NurseryRepository nurseryRepository,
        AttendanceEventRepository eventRepository,
        IncidentReportRepository incidentRepository
    ) {
        this.childRepository = childRepository;
        this.nurseryRepository = nurseryRepository;
        this.eventRepository = eventRepository;
        this.incidentRepository = incidentRepository;
    }

    /**
     * Build an XLSX of child records for the current nursery.
     * If childIds is non-empty, exactly those enrolled children are exported;
     * otherwise if roomIds is non-empty, all enrolled children in those rooms;
     * otherwise all enrolled children.
     */
    @Transactional(readOnly = true)
    public byte[] childrenXlsx(List<UUID> childIds, List<UUID> roomIds) {
        UUID nurseryId = TenantContext.currentNurseryId();
        List<Child> children = childRepository
            .findByNurseryIdAndEnrollmentStatusOrderByFullNameAsc(nurseryId, EnrollmentStatus.ENROLLED);

        if (childIds != null && !childIds.isEmpty()) {
            Set<UUID> ids = new HashSet<>(childIds);
            children = children.stream().filter(c -> ids.contains(c.getId())).toList();
        } else if (roomIds != null && !roomIds.isEmpty()) {
            Set<UUID> rids = new HashSet<>(roomIds);
            children = children.stream()
                .filter(c -> c.getRoom() != null && rids.contains(c.getRoom().getId()))
                .toList();
        }

        String[] headers = {"Child ID", "Full Name", "Date of Birth", "Age (Months)", "Room"};
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Child Records");

            CellStyle headStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            int r = 1;
            LocalDate today = LocalDate.now();
            for (Child c : children) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(shortId(c.getId()));
                row.createCell(1).setCellValue(c.getFullName() != null ? c.getFullName() : "");
                LocalDate dob = c.getDob();
                row.createCell(2).setCellValue(dob != null ? dob.toString() : "");
                row.createCell(3).setCellValue(dob != null ? ChronoUnit.MONTHS.between(dob, today) : 0);
                row.createCell(4).setCellValue(c.getRoom() != null ? c.getRoom().getName() : "—");
            }

            // Fixed column widths (avoids AWT font metrics on headless servers).
            int[] widths = {14, 30, 16, 14, 24};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build export", e);
        }
    }

    /**
     * Build an XLSX of attendance logs for the current nursery over a date range.
     * Children resolve like the child export (childIds → roomIds → all enrolled).
     * One row per child per day in the range; statuses ("PRESENT"/"ABSENT") filters
     * which rows are kept (empty = both).
     */
    @Transactional(readOnly = true)
    public byte[] attendanceXlsx(
        List<UUID> childIds, List<UUID> roomIds, LocalDate from, LocalDate to, List<String> statuses
    ) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));

        List<Child> children = childRepository
            .findByNurseryIdAndEnrollmentStatusOrderByFullNameAsc(nurseryId, EnrollmentStatus.ENROLLED);
        if (childIds != null && !childIds.isEmpty()) {
            Set<UUID> ids = new HashSet<>(childIds);
            children = children.stream().filter(c -> ids.contains(c.getId())).toList();
        } else if (roomIds != null && !roomIds.isEmpty()) {
            Set<UUID> rids = new HashSet<>(roomIds);
            children = children.stream()
                .filter(c -> c.getRoom() != null && rids.contains(c.getRoom().getId()))
                .toList();
        }

        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start;
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        List<AttendanceEvent> events = eventRepository.findByNurseryIdAndRecordedAtBetween(
            nurseryId, AttendanceDates.startOfDay(nursery, start), AttendanceDates.endOfRangeInclusive(nursery, end));
        // childId -> (localDate -> events)
        Map<UUID, Map<LocalDate, List<AttendanceEvent>>> byChildDate = new HashMap<>();
        for (AttendanceEvent e : events) {
            LocalDate d = AttendanceDates.localDateOf(nursery, e.getRecordedAt());
            byChildDate
                .computeIfAbsent(e.getChild().getId(), k -> new LinkedHashMap<>())
                .computeIfAbsent(d, k -> new ArrayList<>())
                .add(e);
        }

        boolean noStatusFilter = statuses == null || statuses.isEmpty();
        boolean wantPresent = noStatusFilter || statuses.contains("PRESENT");
        boolean wantAbsent = noStatusFilter || statuses.contains("ABSENT");

        String[] headers = {"Child", "Room", "Date", "Status", "Check In", "Check Out"};
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Attendance Logs");

            CellStyle headStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            int r = 1;
            for (Child c : children) {
                Map<LocalDate, List<AttendanceEvent>> days = byChildDate.getOrDefault(c.getId(), Map.of());
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    List<AttendanceEvent> dayEvents = days.getOrDefault(d, List.of());
                    Instant in = null;
                    Instant out = null;
                    boolean arrived = false;
                    for (AttendanceEvent e : dayEvents) {
                        if (e.getType() == AttendanceEventType.CHECK_IN) {
                            in = e.getRecordedAt();
                            arrived = true;
                        } else if (e.getType() == AttendanceEventType.CHECK_OUT) {
                            out = e.getRecordedAt();
                        }
                    }
                    if (arrived && !wantPresent) continue;
                    if (!arrived && !wantAbsent) continue;

                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(c.getFullName() != null ? c.getFullName() : "");
                    row.createCell(1).setCellValue(c.getRoom() != null ? c.getRoom().getName() : "—");
                    row.createCell(2).setCellValue(d.toString());
                    row.createCell(3).setCellValue(arrived ? "Present" : "Absent");
                    row.createCell(4).setCellValue(in != null ? TIME_FMT.format(localTime(nursery, in)) : "--");
                    row.createCell(5).setCellValue(out != null ? TIME_FMT.format(localTime(nursery, out)) : "--");
                }
            }

            int[] widths = {26, 22, 14, 12, 12, 12};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build export", e);
        }
    }

    /**
     * Build an XLSX of incident reports for the current nursery. Children resolve
     * like the other exports (childIds → roomIds → all). One row per incident,
     * optionally limited to a date range.
     */
    @Transactional(readOnly = true)
    public byte[] incidentsXlsx(List<UUID> childIds, List<UUID> roomIds, LocalDate from, LocalDate to) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));

        List<IncidentReport> incidents;
        if (from != null || to != null) {
            LocalDate start = from != null ? from : LocalDate.now();
            LocalDate end = to != null ? to : start;
            if (end.isBefore(start)) {
                LocalDate tmp = start;
                start = end;
                end = tmp;
            }
            incidents = incidentRepository.findByNurseryIdAndIncidentAtBetweenOrderByIncidentAtDesc(
                nurseryId, AttendanceDates.startOfDay(nursery, start), AttendanceDates.endOfRangeInclusive(nursery, end));
        } else {
            incidents = incidentRepository.findByNurseryIdOrderByIncidentAtDesc(nurseryId);
        }

        if (childIds != null && !childIds.isEmpty()) {
            Set<UUID> ids = new HashSet<>(childIds);
            incidents = incidents.stream().filter(i -> ids.contains(i.getChild().getId())).toList();
        } else if (roomIds != null && !roomIds.isEmpty()) {
            Set<UUID> rids = new HashSet<>(roomIds);
            incidents = incidents.stream()
                .filter(i -> i.getRoom() != null && rids.contains(i.getRoom().getId()))
                .toList();
        }

        String[] headers = {"Child", "Room", "Date", "Time", "Incident Type", "Description", "Action Taken", "Affected Area"};
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Incident Log");

            CellStyle headStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            int r = 1;
            for (IncidentReport in : incidents) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(in.getChild().getFullName() != null ? in.getChild().getFullName() : "");
                row.createCell(1).setCellValue(in.getRoom() != null ? in.getRoom().getName() : "—");
                Instant at = in.getIncidentAt();
                row.createCell(2).setCellValue(at != null ? AttendanceDates.localDateOf(nursery, at).toString() : "");
                row.createCell(3).setCellValue(at != null ? TIME_FMT.format(localTime(nursery, at)) : "");
                row.createCell(4).setCellValue(in.getIncidentType() != null ? in.getIncidentType() : "");
                row.createCell(5).setCellValue(in.getDescription() != null ? in.getDescription() : "");
                row.createCell(6).setCellValue(in.getActionTaken() != null ? in.getActionTaken() : "");
                row.createCell(7).setCellValue(in.getInjuryArea() != null ? in.getInjuryArea() : "");
            }

            int[] widths = {26, 22, 14, 12, 20, 40, 30, 20};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build export", e);
        }
    }

    private static LocalTime localTime(Nursery nursery, Instant instant) {
        return AttendanceDates.localTimeOf(nursery, instant);
    }

    private static String shortId(UUID id) {
        return id.toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
