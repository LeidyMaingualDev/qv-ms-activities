package com.qvenly.qv_ms_activities.repository;
import com.qvenly.qv_ms_activities.model.entity.ActivityMember;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.model.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ActivityMemberRepository extends JpaRepository<ActivityMember, Long> {
    List<ActivityMember> findByActivityIdAndStatus(Long activityId, MemberStatus status);
    List<ActivityMember> findByUserEmailAndStatus(String userEmail, MemberStatus status);
    Optional<ActivityMember> findByActivityIdAndUserEmailAndEventRole(Long activityId, String userEmail, ActivityMemberRole role);
    boolean existsByActivityIdAndUserEmailAndStatus(Long activityId, String userEmail, MemberStatus status);
    long countByActivityIdAndEventRoleAndStatus(Long activityId, ActivityMemberRole role, MemberStatus status);
    List<ActivityMember> findByUserEmailAndEventRoleAndStatus(String userEmail, ActivityMemberRole eventRole, MemberStatus status);
}
