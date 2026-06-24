package com.qvenly.qv_ms_activities.repository;

import com.qvenly.qv_ms_activities.model.entity.Activity;
import com.qvenly.qv_ms_activities.model.enums.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long>,
        JpaSpecificationExecutor<Activity> {

    List<Activity> findByEventIdOrderByStartDatetimeAsc(Long eventId);
    List<Activity> findByEventIdAndStatus(Long eventId, ActivityStatus status);
    long countByEventIdAndStatusNot(Long eventId, ActivityStatus status);
}