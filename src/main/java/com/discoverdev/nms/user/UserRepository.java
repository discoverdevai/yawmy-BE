package com.discoverdev.nms.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRole(Role role);

    long countByNurseryIdAndRole(UUID nurseryId, Role role);

    List<User> findByNurseryIdAndRoleAndEnabledTrue(UUID nurseryId, Role role);

    List<User> findByNurseryIdAndRoleAndEnabledTrueOrderByFullNameAsc(UUID nurseryId, Role role);

    // All users of a role in the nursery (enabled + pending), for the staff list.
    List<User> findByNurseryIdAndRoleOrderByFullNameAsc(UUID nurseryId, Role role);
}
