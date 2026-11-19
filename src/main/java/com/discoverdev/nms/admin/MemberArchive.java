package com.discoverdev.nms.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A staff member or parent the nursery owner archived (Jira YC-118). Archiving is reversible
 * (YC-119 Restore): the account stays, sign-in is blocked while this row exists.
 */
@Entity
@Table(name = "member_archives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberArchive {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "archived_at", nullable = false)
    private Instant archivedAt;

    /** Whether the account was active (had set a password) when archived — restored as it was. */
    @Column(name = "was_active", nullable = false)
    private boolean wasActive;
}
