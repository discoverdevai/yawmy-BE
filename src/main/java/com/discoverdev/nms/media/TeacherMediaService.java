package com.discoverdev.nms.media;

import com.discoverdev.nms.attendance.AttendanceDates;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.media.dto.BulkUploadResponse;
import com.discoverdev.nms.media.dto.MediaItemResponse;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherMediaService {

    static final Duration TEACHER_DELETE_WINDOW = Duration.ofHours(24);

    private final MediaWriter writer;
    private final MediaItemRepository repository;
    private final ChildRepository childRepository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;
    private final TeacherAccessService teacherAccess;

    public TeacherMediaService(
        MediaWriter writer,
        MediaItemRepository repository,
        ChildRepository childRepository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        TeacherAccessService teacherAccess
    ) {
        this.writer = writer;
        this.repository = repository;
        this.childRepository = childRepository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.teacherAccess = teacherAccess;
    }

    @Transactional
    public BulkUploadResponse uploadForChild(
        UUID childId, List<MultipartFile> files, String caption, User teacher
    ) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (!teacherAccess.canAccessChild(teacher, child)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return writer.writeForChild(child.getNursery(), child, files, caption, teacher);
    }

    @Transactional
    public BulkUploadResponse uploadForRoom(
        UUID roomId, List<MultipartFile> files, String caption, User teacher
    ) {
        if (!teacherAccess.canAccessRoom(teacher, roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        UUID nurseryId = teacher.getNursery().getId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        Room room = roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (room.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot upload to an archived room");
        }
        return writer.writeForRoom(nursery, room, files, caption, teacher);
    }

    @Transactional
    public void delete(UUID mediaId, User teacher) {
        MediaItem item = repository.findByIdAndDeletedAtIsNull(mediaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        if (!teacher.getNursery().getId().equals(item.getNursery().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        if (!item.getUploader().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Teachers can only delete their own uploads");
        }
        Instant cutoff = item.getUploadedAt().plus(TEACHER_DELETE_WINDOW);
        if (Instant.now().isAfter(cutoff)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Teacher delete window (24 h) has elapsed — ask an admin");
        }
        writer.softDelete(item);
    }

    @Transactional(readOnly = true)
    public List<MediaItemResponse> listForChild(UUID childId, LocalDate from, LocalDate to, User teacher) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (!teacherAccess.canAccessChild(teacher, child)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        Nursery nursery = child.getNursery();
        List<MediaItem> items = (from != null && to != null)
            ? repository.findByChildIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
                child.getId(),
                AttendanceDates.startOfDay(nursery, from),
                AttendanceDates.endOfRangeInclusive(nursery, to))
            : repository.findByChildIdAndDeletedAtIsNullOrderByUploadedAtDesc(child.getId());
        return items.stream().map(MediaItemResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<MediaItemResponse> listForRoom(UUID roomId, LocalDate date, User teacher) {
        if (!teacherAccess.canAccessRoom(teacher, roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        UUID nurseryId = teacher.getNursery().getId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        List<MediaItem> items = (date != null)
            ? repository.findByRoomIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
                roomId,
                AttendanceDates.startOfDay(nursery, date),
                AttendanceDates.startOfNextDay(nursery, date))
            : repository.findByRoomIdAndDeletedAtIsNullOrderByUploadedAtDesc(roomId);
        return items.stream().map(MediaItemResponse::of).toList();
    }
}
