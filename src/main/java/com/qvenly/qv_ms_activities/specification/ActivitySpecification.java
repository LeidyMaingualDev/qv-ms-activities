package com.qvenly.qv_ms_activities.specification;

import com.qvenly.qv_ms_activities.model.entity.Activity;
import com.qvenly.qv_ms_activities.model.enums.ActivityStatus;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.List;

public class ActivitySpecification {

    public static Specification<Activity> hasEventId(Long eventId) {
        return (root, query, cb) -> eventId == null ? null : cb.equal(root.get("eventId"), eventId);
    }

    public static Specification<Activity> hasName(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("title")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Activity> hasStatus(ActivityStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Activity> onDate(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return null;
            var start = date.atStartOfDay();
            var end = date.plusDays(1).atStartOfDay();
            return cb.between(root.get("startDatetime"), start, end);
        };
    }

    public static Specification<Activity> hasIds(List<Long> ids) {
        return (root, query, cb) -> (ids == null || ids.isEmpty())
                ? cb.disjunction() // si no hay ids, no devuelve nada
                : root.get("id").in(ids);
    }
}