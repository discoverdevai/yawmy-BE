package com.discoverdev.nms.nursery;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
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

    // Nullable: a nursery is created "bare" (id only) when a super-admin invites its
    // admin. The admin fills in name/logo/location/hours/geofence after accepting.
    @Column(length = 200)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // Storage key for the uploaded logo file; served via GET /api/files/nursery/{id}/logo.
    @Column(name = "logo_key", length = 300)
    private String logoKey;

    // Days of the week the nursery operates (e.g. MONDAY..FRIDAY). The open/close time
    // (hoursStart/hoursEnd) applies to each working day.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "nursery_working_days", joinColumns = @JoinColumn(name = "nursery_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 16)
    @Builder.Default
    private Set<DayOfWeek> workingDays = new LinkedHashSet<>();

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