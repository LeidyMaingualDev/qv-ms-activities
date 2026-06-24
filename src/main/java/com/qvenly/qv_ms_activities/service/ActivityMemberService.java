package com.qvenly.qv_ms_activities.service;

import com.qvenly.qv_ms_activities.client.NotificationClient;
import com.qvenly.qv_ms_activities.exception.BusinessException;
import com.qvenly.qv_ms_activities.model.dto.request.AssignMemberRequest;
import com.qvenly.qv_ms_activities.model.dto.request.CancelParticipationRequest;
import com.qvenly.qv_ms_activities.model.dto.response.ActivityMemberResponse;
import com.qvenly.qv_ms_activities.model.entity.Activity;
import com.qvenly.qv_ms_activities.model.entity.ActivityMember;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.model.enums.AuditActionType;
import com.qvenly.qv_ms_activities.model.enums.ConfirmationStatus;
import com.qvenly.qv_ms_activities.model.enums.MemberStatus;
import com.qvenly.qv_ms_activities.repository.ActivityMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityMemberService {

    private final ActivityMemberRepository memberRepository;
    private final ActivityService activityService;
    private final AuditService auditService;
    private final NotificationClient notificationClient;
    private final EventAuthorizationService eventAuthorizationService;

    @Transactional
    public ActivityMemberResponse assignMember(Long activityId, AssignMemberRequest req, String performerEmail) {
        Activity activity = activityService.findById(activityId);

        // Solo el organizador del evento puede asignar miembros.
        eventAuthorizationService.assertIsOrganizer(activity.getEventId(), performerEmail);
        // La persona a asignar debe poder recibir ese rol según su rol en el evento.
        eventAuthorizationService.assertAssignableRole(activity.getEventId(), req.getUserEmail(), req.getEventRole());

        if (activity.getStatus() == com.qvenly.qv_ms_activities.model.enums.ActivityStatus.FINISHED
                || activity.getStatus() == com.qvenly.qv_ms_activities.model.enums.ActivityStatus.CANCELLED) {
            throw new BusinessException("No se puede asignar miembros a una actividad " + activity.getStatus(), HttpStatus.CONFLICT);
        }
        if (memberRepository.findByActivityIdAndUserEmailAndEventRole(activityId, req.getUserEmail(), req.getEventRole()).isPresent()) {
            throw new BusinessException("El usuario ya está asignado con ese rol en esta actividad.", HttpStatus.CONFLICT);
        }
        ActivityMember m = new ActivityMember();
        m.setActivityId(activityId); m.setEventId(activity.getEventId());
        m.setUserEmail(req.getUserEmail()); m.setEventRole(req.getEventRole());
        m.setFunctionDescription(req.getFunctionDescription());
        m.setStatus(MemberStatus.ACTIVE); m.setConfirmationStatus(ConfirmationStatus.PENDING);
        ActivityMember saved = memberRepository.save(m);
        auditService.log(activityId, activity.getEventId(), AuditActionType.MEMBER_ASSIGNED,
                performerEmail, req.getEventRole().name(),
                "Miembro " + req.getUserEmail() + " asignado como " + req.getEventRole());
        notificationClient.sendActivityAssigned(
                req.getUserEmail(),
                null,
                null,
                activity.getTitle(),
                activityId,
                activity.getEventId(),
                null,
                req.getEventRole().name(),
                req.getFunctionDescription(),
                activity.getStartDatetime() != null ? activity.getStartDatetime().toString() : null
        );
        return toResponse(saved);
    }

    @Transactional
    public void removeMember(Long activityId, Long memberId, String performerEmail) {
        ActivityMember m = findActiveMember(memberId, activityId);
        Activity activity = activityService.findById(activityId);

        // Solo el organizador del evento puede remover miembros de una actividad.
        eventAuthorizationService.assertIsOrganizer(activity.getEventId(), performerEmail);

        m.setStatus(MemberStatus.CANCELLED); m.setRespondedAt(LocalDateTime.now());
        memberRepository.save(m);
        auditService.log(activityId, activity.getEventId(), AuditActionType.MEMBER_REMOVED,
                performerEmail, null, "Miembro " + m.getUserEmail() + " removido.");
    }

    @Transactional
    public ActivityMemberResponse confirmParticipation(Long activityId, String userEmail) {
        ActivityMember m = memberRepository.findByActivityIdAndStatus(activityId, MemberStatus.ACTIVE)
                .stream().filter(x -> x.getUserEmail().equalsIgnoreCase(userEmail)).findFirst()
                .orElseThrow(() -> new BusinessException("No estás asignado a esta actividad.", HttpStatus.FORBIDDEN));
        if (m.getConfirmationStatus() == ConfirmationStatus.CONFIRMED) {
            throw new BusinessException("Ya confirmaste tu participación.", HttpStatus.CONFLICT);
        }
        m.setConfirmationStatus(ConfirmationStatus.CONFIRMED); m.setRespondedAt(LocalDateTime.now());
        ActivityMember updated = memberRepository.save(m);
        Activity activity = activityService.findById(activityId);
        auditService.log(activityId, activity.getEventId(), AuditActionType.MEMBER_CONFIRMED,
                userEmail, m.getEventRole().name(), "Participación confirmada.");
        return toResponse(updated);
    }

    @Transactional
    public ActivityMemberResponse cancelParticipation(Long activityId, String userEmail, CancelParticipationRequest req) {
        ActivityMember m = memberRepository.findByActivityIdAndStatus(activityId, MemberStatus.ACTIVE)
                .stream().filter(x -> x.getUserEmail().equalsIgnoreCase(userEmail)).findFirst()
                .orElseThrow(() -> new BusinessException("No estás asignado a esta actividad.", HttpStatus.FORBIDDEN));
        m.setConfirmationStatus(ConfirmationStatus.CANCELLED);
        m.setCancelReason(req.getCancelReason()); m.setRespondedAt(LocalDateTime.now());
        ActivityMember updated = memberRepository.save(m);
        Activity activity = activityService.findById(activityId);
        auditService.log(activityId, activity.getEventId(), AuditActionType.MEMBER_CANCELLED,
                userEmail, m.getEventRole().name(), "Cancelación: " + req.getCancelReason());
        return toResponse(updated);
    }

    public List<ActivityMemberResponse> getMembersByActivity(Long activityId) {
        return memberRepository.findByActivityIdAndStatus(activityId, MemberStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    private ActivityMember findActiveMember(Long memberId, Long activityId) {
        ActivityMember m = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Miembro no encontrado: " + memberId));
        if (!m.getActivityId().equals(activityId)) {
            throw new BusinessException("El miembro no pertenece a esta actividad.", HttpStatus.BAD_REQUEST);
        }
        if (m.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException("El miembro ya no está activo.", HttpStatus.CONFLICT);
        }
        return m;
    }

    public ActivityMemberResponse toResponse(ActivityMember m) {
        ActivityMemberResponse r = new ActivityMemberResponse();
        r.setId(m.getId()); r.setActivityId(m.getActivityId()); r.setEventId(m.getEventId());
        r.setUserEmail(m.getUserEmail()); r.setEventRole(m.getEventRole());
        r.setFunctionDescription(m.getFunctionDescription()); r.setStatus(m.getStatus());
        r.setConfirmationStatus(m.getConfirmationStatus()); r.setCancelReason(m.getCancelReason());
        r.setAssignedAt(m.getAssignedAt()); r.setRespondedAt(m.getRespondedAt());
        return r;
    }
}