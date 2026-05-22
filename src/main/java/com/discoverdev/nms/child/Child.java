package com.discoverdev.nms.child;

import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.Room;
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
import java.time.LocalDate;
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
    name = "children",
    indexes = {
        @Index(name = "idx_children_nursery", columnList = "nursery_id"),
        @Index(name = "idx_children_room", columnList = "room_id"),
        @Index(name = "idx_children_nursery_status", columnList = "nursery_id, enrollment_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Child {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nursery_id", nullable = false)
    private Nursery nursery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Column(name = "photo_key", length = 255)
    private String photoKey;

    @Column(name = "allergies", length = 2000)
    private String allergies;

    @Column(name = "medical_notes", length = 2000)
    private String medicalNotes;

    @Column(name = "immunizations", length = 2000)
    private String immunizations;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_status", nullable = false, length = 16)
    @Builder.Default
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.ENROLLED;

    @Column(name = "unenrolled_at")
    private Instant unenrolledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
