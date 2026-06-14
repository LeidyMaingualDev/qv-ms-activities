package com.qvenly.qv_ms_activities.model.dto.response;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.model.enums.ConfirmationStatus;
import com.qvenly.qv_ms_activities.model.enums.MemberStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityMemberResponse {
    private Long id;
    private Long activityId;
    private Long eventId;
    private String userEmail;
    private ActivityMemberRole eventRole;
    private String functionDescription;
    private MemberStatus status;
    private ConfirmationStatus confirmationStatus;
    private String cancelReason;
    private LocalDateTime assignedAt;
    private LocalDateTime respondedAt;
}
