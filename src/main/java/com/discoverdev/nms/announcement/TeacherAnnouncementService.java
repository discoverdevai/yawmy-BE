package com.discoverdev.nms.announcement;

import com.discoverdev.nms.announcement.dto.AnnouncementResponse;
import com.discoverdev.nms.announcement.dto.CreateAnnouncementRequest;
import com.discoverdev.nms.announcement.dto.UpdateAnnouncementRequest;
import com.discoverdev.nms.media.MediaItem;
import com.discoverdev.nms.media.MediaItemRepository;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherAnnouncementService {

    private final AnnouncementRepository repository;
    private final AnnouncementSeenRepository seenRepository;
    private final MediaItemRepository mediaRepository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;
    private final TeacherAccessService teacherAccess;
    private final ApplicationEventPublisher events;

    public TeacherAnnouncementService(
        AnnouncementRepository repository,
        AnnouncementSeenRepository seenRepository,
        MediaItemRepository mediaRepository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        TeacherAccessService teacherAccess,
        ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.seenRepository = seenRepository;
        this.mediaRepository = mediaRepository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.teacherAccess = teacherAccess;
        this.events = events;
    }

    @Transactional
    public AnnouncementResponse create(CreateAnnouncementRequest req, User teacher) {
        if (req.scope() != AnnouncementScope.ROOM || req.roomId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Teachers can only post ROOM-scoped announcements with a roomId");
        }
        if (!teacherAccess.canAccessRoom(teacher, req.roomId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        UUID nurseryId = teacher.getNursery().getId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        Room room = roomRepository.findByIdAndNurseryId(req.roomId(), nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (room.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is archived");
        }
        List<MediaItem> attachments = resolveAttachments(req.attachmentMediaIds(), nurseryId);
        Announcement saved = repository.save(Announcement.builder()
            .nursery(nursery)
            .author(teacher)
            .scope(AnnouncementScope.ROOM)
            .room(room)
            .title(req.title())
            .body(req.body())
            .links(req.links() != null ? new ArrayList<>(req.links()) : new ArrayList<>())
            .attachments(attachments)
            .build());
        events.publishEvent(new AnnouncementPublishedEvent(
            nurseryId, saved.getId(), saved.getScope(), room.getId(), teacher.getId()
        ));
        return AnnouncementResponse.of(saved, 0, false, null);
    }

    @Transactional
    public AnnouncementResponse update(UUID id, UpdateAnnouncementRequest req, User teacher) {
        Announcement ann = loadAccessible(id, teacher);
        if (!ann.getAuthor().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the author can edit this announcement");
        }
        if (req.title() != null) ann.setTitle(req.title());
        if (req.body() != null) ann.setBody(req.body());
        if (req.links() != null) ann.setLinks(new ArrayList<>(req.links()));
        if (req.attachmentMediaIds() != null) {
            ann.setAttachments(resolveAttachments(req.attachmentMediaIds(), ann.getNursery().getId()));
        }
        long count = seenRepository.countByAnnouncementId(ann.getId());
        return AnnouncementResponse.of(repository.save(ann), count, false, null);
    }

    @Transactional
    public void delete(UUID id, User teacher) {
        Announcement ann = loadAccessible(id, teacher);
        if (!ann.getAuthor().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the author can delete this announcement");
        }
        repository.delete(ann);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listForRoom(UUID roomId, User teacher) {
        if (!teacherAccess.canAccessRoom(teacher, roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        UUID nurseryId = teacher.getNursery().getId();
        return repository.findByNurseryIdAndRoomIdOrderByCreatedAtDesc(nurseryId, roomId).stream()
            .map(a -> {
                long count = seenRepository.countByAnnouncementId(a.getId());
                return AnnouncementResponse.of(a, count, false, null);
            })
            .toList();
    }

    private Announcement loadAccessible(UUID id, User teacher) {
        UUID nurseryId = teacher.getNursery().getId();
        Announcement ann = repository.findByIdAndNurseryId(id, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
        if (ann.getScope() != AnnouncementScope.ROOM || ann.getRoom() == null
            || !teacherAccess.canAccessRoom(teacher, ann.getRoom().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found");
        }
        return ann;
    }

    private List<MediaItem> resolveAttachments(List<UUID> ids, UUID nurseryId) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        List<MediaItem> items = new ArrayList<>();
        for (UUID mediaId : ids) {
            MediaItem m = mediaRepository.findByIdAndNurseryIdAndDeletedAtIsNull(mediaId, nurseryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Attachment not found: " + mediaId));
            items.add(m);
        }
        return items;
    }
}
