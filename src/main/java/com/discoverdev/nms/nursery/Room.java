package com.discoverdev.nms.nursery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "rooms",
    indexes = {
        @Index(name = "idx_rooms_nursery", columnList = "nursery_id"),
        @Index(name = "idx_rooms_nursery_archived", columnList = "nursery_id, archived_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nursery_id", nullable = false)
    private Nursery nursery;

    @Column(nullable = false, length = 100)
    private String name;

    // Max children allowed in the room. Defaults to 20 when not specified.
    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 20;

    // Uploaded room image (file served via GET /api/files/rooms/{id}/image).
    @Column(name = "image_key", length = 300)
    private String imageKey;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}