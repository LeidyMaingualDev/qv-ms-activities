package com.qvenly.qv_ms_activities.model.entity;

import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.model.enums.ConfirmationStatus;
import com.qvenly.qv_ms_activities.model.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "activity_member",
    uniqueConstraints = @UniqueConstraint(name = "uq_activity_member",
        columnNames = {"activity_id", "user_email", "event_role"}))
public class ActivityMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "activity_id", nullable = false)
    private Long activityId;
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    @Column(name = "user_email", nullable = false, length = 100)
    private String userEmail;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_role", nullable = false, length = 15)
    private ActivityMemberRole eventRole;
    @Column(name = "function_description", length = 255)
    private String functionDescription;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberStatus status = MemberStatus.ACTIVE;
    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_status", nullable = false, length = 10)
    private ConfirmationStatus confirmationStatus = ConfirmationStatus.PENDING;
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    @PrePersist
    protected void onCreate() { assignedAt = LocalDateTime.now(); }
}
