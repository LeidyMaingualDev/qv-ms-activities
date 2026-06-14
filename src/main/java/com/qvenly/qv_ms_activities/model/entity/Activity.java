package com.qvenly.qv_ms_activities.model.entity;

import com.qvenly.qv_ms_activities.model.enums.ActivityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "activity")
public class Activity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(length = 255)
    private String location;
    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;
    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status = ActivityStatus.PENDING;
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;
    @Column(name = "created_by_email", nullable = false, length = 100)
    private String createdByEmail;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
