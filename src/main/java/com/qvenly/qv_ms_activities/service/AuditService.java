package com.qvenly.qv_ms_activities.service;

import com.qvenly.qv_ms_activities.model.dto.response.AuditLogResponse;
import com.qvenly.qv_ms_activities.model.entity.ActivityAuditLog;
import com.qvenly.qv_ms_activities.model.enums.AuditActionType;
import com.qvenly.qv_ms_activities.repository.ActivityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final ActivityAuditLogRepository repository;

    public void log(Long activityId, Long eventId, AuditActionType action,
                    String performedByEmail, String performedByRole, String detail) {
        ActivityAuditLog entry = new ActivityAuditLog();
        entry.setActivityId(activityId); entry.setEventId(eventId);
        entry.setActionType(action); entry.setPerformedByEmail(performedByEmail);
        entry.setPerformedByRole(performedByRole); entry.setChangeDetail(detail);
        repository.save(entry);
    }

    public List<AuditLogResponse> getAuditLog(Long activityId) {
        return repository.findByActivityIdOrderByPerformedAtDesc(activityId)
                .stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(ActivityAuditLog e) {
        AuditLogResponse r = new AuditLogResponse();
        r.setId(e.getId()); r.setActivityId(e.getActivityId()); r.setEventId(e.getEventId());
        r.setActionType(e.getActionType()); r.setPerformedByEmail(e.getPerformedByEmail());
        r.setPerformedByRole(e.getPerformedByRole()); r.setChangeDetail(e.getChangeDetail());
        r.setPerformedAt(e.getPerformedAt());
        return r;
    }
}
