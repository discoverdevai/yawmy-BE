package com.discoverdev.nms.incident;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.user.User;
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
    name = "incident_reports",
    indexes = {
        @Index(name = "idx_ir_nursery", columnList = "nursery_id"),
        @Index(name = "idx_ir_child_incident", columnList = "child_id, incident_at"),
        @Index(name = "idx_ir_room_incident", columnList = "room_id, incident_at"),
        @Index(name = "idx_ir_incident_at", columnList = "incident_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentReport {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nursery_id", nullable = false)
    private Nursery nursery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "incident_at", nullable = false)
    private Instant incidentAt;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "action_taken", length = 2000)
    private String actionTaken;

    @Column(name = "injury_area", length = 100)
    private String injuryArea;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
