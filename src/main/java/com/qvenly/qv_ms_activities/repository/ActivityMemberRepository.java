package com.qvenly.qv_ms_activities.repository;
import com.qvenly.qv_ms_activities.model.entity.ActivityMember;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.model.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ActivityMemberRepository extends JpaRepository<ActivityMember, Long> {
    List<ActivityMember> findByActivityIdAndStatus(Long activityId, MemberStatus status);
    List<ActivityMember> findByUserEmailAndStatus(String userEmail, MemberStatus status);
    Optional<ActivityMember> findByActivityIdAndUserEmailAndEventRole(Long activityId, String userEmail, ActivityMemberRole role);
    boolean existsByActivityIdAndUserEmailAndStatus(Long activityId, String userEmail, MemberStatus status);
    long countByActivityIdAndEventRoleAndStatus(Long activityId, ActivityMemberRole role, MemberStatus status);
    List<ActivityMember> findByUserEmailAndEventRoleAndStatus(String userEmail, ActivityMemberRole eventRole, MemberStatus status);

    @Query("SELECT DISTINCT m.userEmail, m.eventId FROM ActivityMember m " +
       "WHERE m.eventId = :eventId AND m.eventRole IN :roles AND m.status = 'ACTIVE'")
        List<Object[]> findEmailsByEventIdAndRoles(
                @Param("eventId") Long eventId,
                @Param("roles") List<ActivityMemberRole> roles);
}
