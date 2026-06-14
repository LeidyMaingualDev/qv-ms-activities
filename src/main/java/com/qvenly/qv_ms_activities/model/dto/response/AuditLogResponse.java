package com.qvenly.qv_ms_activities.model.dto.response;
import com.qvenly.qv_ms_activities.model.enums.AuditActionType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long id;
    private Long activityId;
    private Long eventId;
    private AuditActionType actionType;
    private String performedByEmail;
    private String performedByRole;
    private String changeDetail;
    private LocalDateTime performedAt;
}
