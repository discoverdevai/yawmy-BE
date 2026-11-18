package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.HealthEntryRequest;
import com.discoverdev.nms.child.dto.HealthEntryResponse;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.User;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * A child's allergy, immunization and medical-note lists (Jira YC-231..233).
 *
 * The child also keeps one text field per list, which the current apps read and write.
 * The two are kept in step: each entry is one line of that text. Writing the text replaces
 * the list; changing the list rewrites the text. Children whose text predates the lists get
 * their entries the first time the list is read.
 */
@Service
public class ChildHealthService {

    /** The text field holds at most 2000 characters, so the joined list has to fit. */
    private static final int LEGACY_MAX = 2000;

    private final ChildHealthEntryRepository entries;
    private final ChildRepository children;
    private final TeacherAccessService teacherAccess;

    public ChildHealthService(
        ChildHealthEntryRepository entries,
        ChildRepository children,
        TeacherAccessService teacherAccess
    ) {
        this.entries = entries;
        this.children = children;
        this.teacherAccess = teacherAccess;
    }

    // ---- admin ----

    @Transactional
    public List<HealthEntryResponse> list(UUID childId, HealthEntryType type) {
        return listFor(loadForAdmin(childId), type);
    }

    @Transactional
    public HealthEntryResponse add(UUID childId, HealthEntryType type, HealthEntryRequest req) {
        Child child = loadForAdmin(childId);
        listFor(child, type); // make sure older text is already split into entries
        ChildHealthEntry e = entries.save(ChildHealthEntry.builder()
            .child(child).type(type).text(req.text().trim()).recordedOn(req.recordedOn()).build());
        syncText(child, type);
        return HealthEntryResponse.of(e);
    }

    @Transactional
    public HealthEntryResponse update(UUID childId, HealthEntryType type, UUID entryId, HealthEntryRequest req) {
        Child child = loadForAdmin(childId);
        ChildHealthEntry e = loadEntry(child, type, entryId);
        e.setText(req.text().trim());
        e.setRecordedOn(req.recordedOn());
        entries.save(e);
        syncText(child, type);
        return HealthEntryResponse.of(e);
    }

    @Transactional
    public void delete(UUID childId, HealthEntryType type, UUID entryId) {
        Child child = loadForAdmin(childId);
        entries.delete(loadEntry(child, type, entryId));
        entries.flush();
        syncText(child, type);
    }

    // ---- teacher (read only) ----

    @Transactional
    public List<HealthEntryResponse> listForTeacher(UUID childId, HealthEntryType type, User teacher) {
        Child child = children.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (!teacherAccess.canAccessChild(teacher, child)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return listFor(child, type);
    }

    // ---- keeping the old text field in step ----

    /** The old text field was written (current apps): rebuild the list from its lines. */
    @Transactional
    public void replaceFromText(Child child, HealthEntryType type, String text) {
        entries.deleteByChildIdAndType(child.getId(), type);
        entries.flush();
        List<String> lines = text == null ? List.of() : Arrays.stream(text.split("\\R"))
            .map(String::trim).filter(l -> !l.isEmpty()).toList();
        for (String line : lines) {
            entries.save(ChildHealthEntry.builder()
                .child(child).type(type).text(line.length() > 500 ? line.substring(0, 500) : line).build());
        }
    }

    private List<HealthEntryResponse> listFor(Child child, HealthEntryType type) {
        List<ChildHealthEntry> list = entries.findByChildIdAndTypeOrderByCreatedAtAscIdAsc(child.getId(), type);
        String text = type.legacyText(child);
        if (list.isEmpty() && text != null && !text.isBlank()) {
            replaceFromText(child, type, text);
            list = entries.findByChildIdAndTypeOrderByCreatedAtAscIdAsc(child.getId(), type);
        }
        return list.stream().map(HealthEntryResponse::of).toList();
    }

    private void syncText(Child child, HealthEntryType type) {
        String joined = entries.findByChildIdAndTypeOrderByCreatedAtAscIdAsc(child.getId(), type).stream()
            .map(ChildHealthEntry::getText).collect(Collectors.joining("\n"));
        if (joined.length() > LEGACY_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "This list is too long; keep the entries under " + LEGACY_MAX + " characters in total");
        }
        type.setLegacyText(child, joined.isEmpty() ? null : joined);
        children.save(child);
    }

    private Child loadForAdmin(UUID childId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        return children.findById(childId)
            .filter(c -> c.getNursery() != null && nurseryId.equals(c.getNursery().getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
    }

    private ChildHealthEntry loadEntry(Child child, HealthEntryType type, UUID entryId) {
        return entries.findById(entryId)
            .filter(e -> e.getChild().getId().equals(child.getId()) && e.getType() == type)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
    }
}
