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
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminMediaService {

    private final MediaWriter writer;
    private final MediaItemRepository repository;
    private final ChildRepository childRepository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;

    public AdminMediaService(
        MediaWriter writer,
        MediaItemRepository repository,
        ChildRepository childRepository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository
    ) {
        this.writer = writer;
        this.repository = repository;
        this.childRepository = childRepository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
    }

    @Transactional
    public BulkUploadResponse uploadForChild(
        UUID childId, List<MultipartFile> files, String caption, User admin
    ) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Child child = childRepository.findByIdAndNurseryId(childId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        return writer.writeForChild(child.getNursery(), child, files, caption, admin);
    }

    @Transactional
    public BulkUploadResponse uploadForRoom(
        UUID roomId, List<MultipartFile> files, String caption, User admin
    ) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        Room room = roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (room.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot upload to an archived room");
        }
        return writer.writeForRoom(nursery, room, files, caption, admin);
    }

    /** Upload announcement attachments — nursery-scoped, not tied to a child or room. */
    @Transactional
    public BulkUploadResponse uploadForAnnouncement(List<MultipartFile> files, String caption, User admin) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        return writer.writeForNursery(nursery, files, caption, admin);
    }

    @Transactional
    public void delete(UUID mediaId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        MediaItem item = repository.findByIdAndNurseryIdAndDeletedAtIsNull(mediaId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        writer.softDelete(item);
    }

    @Transactional(readOnly = true)
    public List<MediaItemResponse> listForChild(UUID childId, LocalDate from, LocalDate to) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Child child = childRepository.findByIdAndNurseryId(childId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
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
    public List<MediaItemResponse> listForRoom(UUID roomId, LocalDate date) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        Room room = roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        List<MediaItem> items = (date != null)
            ? repository.findByRoomIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
                room.getId(),
                AttendanceDates.startOfDay(nursery, date),
                AttendanceDates.startOfNextDay(nursery, date))
            : repository.findByRoomIdAndDeletedAtIsNullOrderByUploadedAtDesc(room.getId());
        return items.stream().map(MediaItemResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<MediaItemResponse> listNurseryWide(LocalDate from, LocalDate to) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        List<MediaItem> items = (from != null && to != null)
            ? repository.findByNurseryIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
                nurseryId,
                AttendanceDates.startOfDay(nursery, from),
                AttendanceDates.endOfRangeInclusive(nursery, to))
            : repository.findByNurseryIdAndDeletedAtIsNullOrderByUploadedAtDesc(nurseryId);
        return items.stream().map(MediaItemResponse::of).toList();
    }
}
