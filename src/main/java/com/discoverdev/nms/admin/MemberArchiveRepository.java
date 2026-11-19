package com.discoverdev.nms.admin;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberArchiveRepository extends JpaRepository<MemberArchive, UUID> {
}
