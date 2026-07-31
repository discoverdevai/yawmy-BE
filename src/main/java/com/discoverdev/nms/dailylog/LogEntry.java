package com.discoverdev.nms.dailylog;

import com.discoverdev.nms.user.User;
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
@Table(
    name = "log_entries",
    indexes = {
        @Index(name = "idx_le_log", columnList = "daily_log_id"),
        @Index(name = "idx_le_log_published", columnList = "daily_log_id, published_at"),
        @Index(name = "idx_le_author", columnList = "author_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LogEntryCategory category;

    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String detail;

    /** NAP only. Derived from the nap window on write, so readers need not subtract. */
    @Column(name = "nap_minutes")
    private Integer napMinutes;

    // ---- structured answers, one set per category ---------------------------
    // Stored as constants rather than the words shown on screen, so either app
    // can be reworded without the two drifting apart or stored rows aging badly.

    /** MOOD only. */
    @Enumerated(EnumType.STRING)
    @Column(name = "mood", length = 24)
    private MoodOption mood;

    /** MEAL only — how much of it was eaten. */
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_amount", length = 16)
    private MealAmount mealAmount;

    /** NAP only — the window the child actually slept for. */
    @Column(name = "nap_start_time")
    private LocalTime napStartTime;

    @Column(name = "nap_end_time")
    private LocalTime napEndTime;

    /** NAP only — how the nap went. */
    @Enumerated(EnumType.STRING)
    @Column(name = "nap_quality", length = 16)
    private NapQuality napQuality;

    @Column(name = "published_at")
    private Instant publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isPublished() {
        return publishedAt != null;
    }
}
