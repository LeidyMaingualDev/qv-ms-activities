package com.qvenly.qv_ms_activities.model.dto.response;

import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.model.enums.ActivityStatus;
import com.qvenly.qv_ms_activities.model.enums.ConfirmationStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgendaItemResponse {
    private Long activityId;
    private String activityTitle;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private ActivityStatus activityStatus;
    private Long eventId;
    private String eventTitle;
    private ActivityMemberRole role;
    private ConfirmationStatus confirmationStatus;
}