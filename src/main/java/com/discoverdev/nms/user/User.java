package com.discoverdev.nms.user;

import com.discoverdev.nms.nursery.Nursery;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_role", columnList = "role"),
        @Index(name = "idx_users_nursery", columnList = "nursery_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nursery_id")
    private Nursery nursery;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    // Job title for TEACHER staff (Lead Teacher, Assistant Teacher, …). Null for others.
    @Enumerated(EnumType.STRING)
    @Column(name = "staff_role", length = 32)
    private StaffRole staffRole;

    // Uploaded profile image (served via GET /api/files/users/{id}/image).
    @Column(name = "image_key", length = 300)
    private String imageKey;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    @Builder.Default
    private Locale locale = Locale.EN;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // Soft-delete marker: when set, the teacher is removed from the nursery
    // (hidden from all lists and unassigned from every room).
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
