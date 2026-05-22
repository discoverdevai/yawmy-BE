package com.discoverdev.nms.notification;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationAudienceService {

    private final GuardianLinkRepository guardianLinkRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;

    public NotificationAudienceService(
        GuardianLinkRepository guardianLinkRepository,
        ChildRepository childRepository,
        UserRepository userRepository
    ) {
        this.guardianLinkRepository = guardianLinkRepository;
        this.childRepository = childRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> guardiansOfChild(UUID childId, boolean enrolledOnly) {
        return guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(childId).stream()
            .filter(link -> !enrolledOnly || link.getChild().getEnrollmentStatus() == EnrollmentStatus.ENROLLED)
            .map(GuardianLink::getGuardian)
            .filter(User::isEnabled)
            .collect(distinctById());
    }

    @Transactional(readOnly = true)
    public List<User> parentsInRoom(UUID nurseryId, UUID roomId) {
        List<Child> children = childRepository
            .findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                nurseryId, roomId, EnrollmentStatus.ENROLLED);
        return children.stream()
            .flatMap(c -> guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(c.getId()).stream())
            .map(GuardianLink::getGuardian)
            .filter(User::isEnabled)
            .collect(distinctById());
    }

    @Transactional(readOnly = true)
    public List<User> adminsOfNursery(UUID nurseryId) {
        return userRepository.findByNurseryIdAndRoleAndEnabledTrue(nurseryId, Role.ADMIN);
    }

    private static java.util.stream.Collector<User, ?, List<User>> distinctById() {
        return java.util.stream.Collectors.collectingAndThen(
            java.util.stream.Collectors.toMap(
                User::getId,
                u -> u,
                (a, b) -> a,
                LinkedHashMap::new),
            m -> List.copyOf(m.values()));
    }
}
