package com.qvenly.qv_ms_activities.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityImageResponse {
    private Long id;
    private Long activityId;
    private String imageUrl;
    private LocalDateTime uploadedAt;
}