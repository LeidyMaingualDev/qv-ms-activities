package com.qvenly.qv_ms_activities.model.dto.request;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UpdateActivityRequest {
    private String title;
    private String description;
    private String location;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Boolean enrollmentEnabled;
    private Integer maxEnrollment;
}
