package com.discoverdev.nms.announcement;

import com.discoverdev.nms.announcement.dto.AnnouncementResponse;
import com.discoverdev.nms.announcement.dto.AnnouncementSeenRow;
import com.discoverdev.nms.announcement.dto.CreateAnnouncementRequest;
import com.discoverdev.nms.announcement.dto.UpdateAnnouncementRequest;
import com.discoverdev.nms.media.MediaItem;
import com.discoverdev.nms.media.MediaItemRepository;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TenantContext;
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
public class AdminAnnouncementService {

    private final AnnouncementRepository repository;
    private final AnnouncementSeenRepository seenRepository;
    private final MediaItemRepository mediaRepository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;
    private final ApplicationEventPublisher events;

    public AdminAnnouncementService(
        AnnouncementRepository repository,
        AnnouncementSeenRepository seenRepository,
        MediaItemRepository mediaRepository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.seenRepository = seenRepository;
        this.mediaRepository = mediaRepository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.events = events;
    }

    @Transactional
    public AnnouncementResponse create(CreateAnnouncementRequest req, User author) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        Room room = null;
        if (req.scope() == AnnouncementScope.ROOM) {
            if (req.roomId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "roomId is required for ROOM-scoped announcements");
            }
            room = roomRepository.findByIdAndNurseryId(req.roomId(), nurseryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
            if (room.getArchivedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is archived");
            }
        }
        List<MediaItem> attachments = resolveAttachments(req.attachmentMediaIds(), nurseryId);
        Announcement saved = repository.save(Announcement.builder()
            .nursery(nursery)
            .author(author)
            .scope(req.scope())
            .room(room)
            .title(req.title())
            .body(req.body())
            .announcementDate(req.announcementDate())
            .announcementTime(req.announcementTime())
            .links(req.links() != null ? new ArrayList<>(req.links()) : new ArrayList<>())
            .attachments(attachments)
            .build());
        events.publishEvent(new AnnouncementPublishedEvent(
            nurseryId, saved.getId(), saved.getScope(),
            room != null ? room.getId() : null, author.getId()
        ));
        return toResponse(saved, null);
    }

    @Transactional
    public AnnouncementResponse update(UUID id, UpdateAnnouncementRequest req) {
        Announcement ann = loadOwned(id);
        if (req.title() != null) ann.setTitle(req.title());
        if (req.body() != null) ann.setBody(req.body());
        if (req.announcementDate() != null) ann.setAnnouncementDate(req.announcementDate());
        if (req.announcementTime() != null) ann.setAnnouncementTime(req.announcementTime());
        // Change share-with (scope / target room).
        if (req.scope() != null) {
            if (req.scope() == AnnouncementScope.ROOM) {
                UUID roomId = req.roomId() != null ? req.roomId()
                    : (ann.getRoom() != null ? ann.getRoom().getId() : null);
                if (roomId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "roomId is required for ROOM-scoped announcements");
                }
                Room room = roomRepository.findByIdAndNurseryId(roomId, ann.getNursery().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
                if (room.getArchivedAt() != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is archived");
                }
                ann.setScope(AnnouncementScope.ROOM);
                ann.setRoom(room);
            } else {
                ann.setScope(AnnouncementScope.NURSERY);
                ann.setRoom(null);
            }
        } else if (req.roomId() != null && ann.getScope() == AnnouncementScope.ROOM) {
            // Same scope, just retarget to a different room.
            Room room = roomRepository.findByIdAndNurseryId(req.roomId(), ann.getNursery().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
            if (room.getArchivedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is archived");
            }
            ann.setRoom(room);
        }
        if (req.links() != null) ann.setLinks(new ArrayList<>(req.links()));
        if (req.attachmentMediaIds() != null) {
            ann.setAttachments(resolveAttachments(req.attachmentMediaIds(), ann.getNursery().getId()));
        }
        return toResponse(repository.save(ann), null);
    }

    @Transactional
    public void delete(UUID id) {
        Announcement ann = loadOwned(id);
        repository.delete(ann);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list(UUID roomId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        List<Announcement> items = (roomId != null)
            ? repository.findByNurseryIdAndRoomIdOrderByCreatedAtDesc(nurseryId, roomId)
            : repository.findByNurseryIdOrderByCreatedAtDesc(nurseryId);
        return items.stream().map(a -> toResponse(a, null)).toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse get(UUID id) {
        return toResponse(loadOwned(id), null);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementSeenRow> seenBy(UUID id) {
        Announcement ann = loadOwned(id);
        return seenRepository.findByAnnouncementId(ann.getId()).stream()
            .map(s -> new AnnouncementSeenRow(
                s.getUser().getId(),
                s.getUser().getFullName(),
                s.getUser().getEmail(),
                s.getUser().getRole().name(),
                s.getSeenAt()
            ))
            .toList();
    }

    Announcement loadOwned(UUID id) {
        UUID nurseryId = TenantContext.currentNurseryId();
        return repository.findByIdAndNurseryId(id, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
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

    private AnnouncementResponse toResponse(Announcement a, UUID viewerUserId) {
        long count = seenRepository.countByAnnouncementId(a.getId());
        return AnnouncementResponse.of(a, count, false, null);
    }
}
