package com.discoverdev.nms.media;

import com.discoverdev.nms.attendance.AttendanceDates;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.media.dto.MediaItemResponse;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParentMediaService {

    private final MediaItemRepository repository;
    private final GuardianLinkRepository guardianLinkRepository;

    public ParentMediaService(
        MediaItemRepository repository, GuardianLinkRepository guardianLinkRepository
    ) {
        this.repository = repository;
        this.guardianLinkRepository = guardianLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<MediaItemResponse> listForChild(
        UUID childId, LocalDate date, LocalDate from, LocalDate to, User parent
    ) {
        Child child = requireLinkedEnrolledChild(childId, parent);
        Nursery nursery = child.getNursery();
        Instant rangeStart;
        Instant rangeEnd;
        if (date != null) {
            rangeStart = AttendanceDates.startOfDay(nursery, date);
            rangeEnd = AttendanceDates.startOfNextDay(nursery, date);
        } else if (from != null && to != null) {
            rangeStart = AttendanceDates.startOfDay(nursery, from);
            rangeEnd = AttendanceDates.endOfRangeInclusive(nursery, to);
        } else {
            rangeStart = null;
            rangeEnd = null;
        }

        List<MediaItem> childMedia = rangeStart != null
            ? repository.findByChildIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
                child.getId(), rangeStart, rangeEnd)
            : repository.findByChildIdAndDeletedAtIsNullOrderByUploadedAtDesc(child.getId());

        List<MediaItem> roomMedia = List.of();
        if (child.getRoom() != null) {
            roomMedia = rangeStart != null
                ? repository.findByRoomIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
                    child.getRoom().getId(), rangeStart, rangeEnd)
                : repository.findByRoomIdAndDeletedAtIsNullOrderByUploadedAtDesc(child.getRoom().getId());
        }

        List<MediaItem> merged = new ArrayList<>(childMedia.size() + roomMedia.size());
        merged.addAll(childMedia);
        merged.addAll(roomMedia);
        merged.sort(Comparator.comparing(MediaItem::getUploadedAt).reversed());
        return merged.stream().map(MediaItemResponse::of).toList();
    }

    private Child requireLinkedEnrolledChild(UUID childId, User parent) {
        GuardianLink link = guardianLinkRepository.findByChildIdAndGuardianId(childId, parent.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        Child child = link.getChild();
        if (child.getEnrollmentStatus() != EnrollmentStatus.ENROLLED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return child;
    }
}
