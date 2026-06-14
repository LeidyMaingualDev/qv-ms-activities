package com.qvenly.qv_ms_activities.repository;
import com.qvenly.qv_ms_activities.model.entity.ActivityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityAuditLogRepository extends JpaRepository<ActivityAuditLog, Long> {
    List<ActivityAuditLog> findByActivityIdOrderByPerformedAtDesc(Long activityId);
}
