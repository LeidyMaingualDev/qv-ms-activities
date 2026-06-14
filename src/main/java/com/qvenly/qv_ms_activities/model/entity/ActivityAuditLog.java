package com.qvenly.qv_ms_activities.model.entity;

import com.qvenly.qv_ms_activities.model.enums.AuditActionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "activity_audit_log")
public class ActivityAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "activity_id", nullable = false)
    private Long activityId;
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private AuditActionType actionType;
    @Column(name = "performed_by_email", nullable = false, length = 100)
    private String performedByEmail;
    @Column(name = "performed_by_role", length = 20)
    private String performedByRole;
    @Column(name = "change_detail", columnDefinition = "TEXT")
    private String changeDetail;
    @Column(name = "performed_at", updatable = false)
    private LocalDateTime performedAt;
    @PrePersist
    protected void onCreate() { performedAt = LocalDateTime.now(); }
}
