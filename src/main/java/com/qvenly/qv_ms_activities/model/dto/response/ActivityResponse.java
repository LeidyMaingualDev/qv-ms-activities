package com.qvenly.qv_ms_activities.model.dto.response;
import com.qvenly.qv_ms_activities.model.enums.ActivityStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityResponse {
    private Long id;
    private Long eventId;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Boolean enrollmentEnabled;
    private Integer maxEnrollment;
    private Integer currentEnrollments;
    private ActivityStatus status;
    private String cancelReason;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
