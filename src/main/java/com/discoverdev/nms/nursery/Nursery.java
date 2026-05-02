package com.discoverdev.nms.nursery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "nurseries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nursery {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 64)
    @Builder.Default
    private String timezone = "Africa/Cairo";

    @Column(name = "hours_start")
    private LocalTime hoursStart;

    @Column(name = "hours_end")
    private LocalTime hoursEnd;

    @Column(name = "geofence_lat")
    private Double geofenceLat;

    @Column(name = "geofence_lng")
    private Double geofenceLng;

    @Column(name = "geofence_radius_m")
    @Builder.Default
    private Integer geofenceRadiusM = 100;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}