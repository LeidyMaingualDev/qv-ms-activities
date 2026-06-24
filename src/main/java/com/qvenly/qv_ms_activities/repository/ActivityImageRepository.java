package com.qvenly.qv_ms_activities.repository;

import com.qvenly.qv_ms_activities.model.entity.ActivityImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityImageRepository extends JpaRepository<ActivityImage, Long> {

    List<ActivityImage> findByActivityId(Long activityId);
}