package com.qvenly.qv_ms_activities.service;

import com.qvenly.qv_ms_activities.client.EventClientService;
import com.qvenly.qv_ms_activities.client.NotificationClient;
import com.qvenly.qv_ms_activities.exception.BusinessException;
import com.qvenly.qv_ms_activities.model.dto.request.AssignMemberRequest;
import com.qvenly.qv_ms_activities.model.dto.request.CancelParticipationRequest;
import com.qvenly.qv_ms_activities.model.dto.response.ActivityMemberResponse;
import com.qvenly.qv_ms_activities.model.dto.response.ActivityResponse;
import com.qvenly.qv_ms_activities.model.dto.response.AgendaItemResponse;
import com.qvenly.qv_ms_activities.model.entity.Activity;
import com.qvenly.qv_ms_activities.model.entity.ActivityMember;
import com.qvenly.qv_ms_activities.model.enums.*;
import com.qvenly.qv_ms_activities.repository.ActivityMemberRepository;
import com.qvenly.qv_ms_activities.repository.ActivityRepository;
import com.qvenly.qv_ms_activities.specification.ActivitySpecification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityMemberService {

    private final ActivityMemberRepository memberRepository;
    private final ActivityService activityService;
    private final AuditService auditService;
    private final NotificationClient notificationClient;
    private final EventClientService eventClientService;
    private final ActivityRepository activityRepository;

    @Transactional
    public ActivityMemberResponse assignMember(Long activityId, AssignMemberRequest req, String performerEmail) {
        Activity activity = activityService.findById(activityId);
        if (activity.getStatus() == com.qvenly.qv_ms_activities.model.enums.ActivityStatus.FINISHED
                || activity.getStatus() == com.qvenly.qv_ms_activities.model.enums.ActivityStatus.CANCELLED) {
            throw new BusinessException("No se puede asignar miembros a una actividad " + activity.getStatus(), HttpStatus.CONFLICT);
        }
        if (memberRepository.findByActivityIdAndUserEmailAndEventRole(activityId, req.getUserEmail(), req.getEventRole()).isPresent()) {
            throw new BusinessException("El usuario ya está asignado con ese rol en esta actividad.", HttpStatus.CONFLICT);
        }
        ActivityMember m = new ActivityMember();
        m.setActivityId(activityId);
        m.setEventId(activity.getEventId());
        m.setUserEmail(req.getUserEmail());
        m.setEventRole(req.getEventRole());
        m.setFunctionDescription(req.getFunctionDescription());
        m.setStatus(MemberStatus.ACTIVE);
        m.setConfirmationStatus(ConfirmationStatus.PENDING);
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
        m.setStatus(MemberStatus.CANCELLED);
        m.setRespondedAt(LocalDateTime.now());
        memberRepository.save(m);
        Activity activity = activityService.findById(activityId);
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
        m.setConfirmationStatus(ConfirmationStatus.CONFIRMED);
        m.setRespondedAt(LocalDateTime.now());
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
        m.setCancelReason(req.getCancelReason());
        m.setRespondedAt(LocalDateTime.now());
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
        r.setId(m.getId());
        r.setActivityId(m.getActivityId());
        r.setEventId(m.getEventId());
        r.setUserEmail(m.getUserEmail());
        r.setEventRole(m.getEventRole());
        r.setFunctionDescription(m.getFunctionDescription());
        r.setStatus(m.getStatus());
        r.setConfirmationStatus(m.getConfirmationStatus());
        r.setCancelReason(m.getCancelReason());
        r.setAssignedAt(m.getAssignedAt());
        r.setRespondedAt(m.getRespondedAt());
        return r;
    }

    @Transactional
    public ActivityMemberResponse enrollSelf(Long activityId, String userEmail) {
        Activity activity = activityService.findById(activityId);

        if (activity.getStatus() == com.qvenly.qv_ms_activities.model.enums.ActivityStatus.FINISHED
                || activity.getStatus() == com.qvenly.qv_ms_activities.model.enums.ActivityStatus.CANCELLED) {
            throw new BusinessException("No puedes inscribirte a una actividad " + activity.getStatus(), HttpStatus.CONFLICT);
        }

        if (!Boolean.TRUE.equals(activity.getEnrollmentEnabled())) {
            throw new BusinessException("Esta actividad no tiene inscripciones habilitadas.", HttpStatus.BAD_REQUEST);
        }

        if (memberRepository.findByActivityIdAndUserEmailAndEventRole(activityId, userEmail, ActivityMemberRole.ATTENDEE).isPresent()) {
            throw new BusinessException("Ya estás inscrito en esta actividad.", HttpStatus.CONFLICT);
        }

        if (activity.getMaxAttendees() != null) {
            long current = memberRepository.countByActivityIdAndEventRoleAndStatus(
                    activityId, ActivityMemberRole.ATTENDEE, MemberStatus.ACTIVE);
            if (current >= activity.getMaxAttendees()) {
                throw new BusinessException("No hay cupos disponibles para esta actividad.", HttpStatus.CONFLICT);
            }
        }

        checkScheduleConflict(userEmail, activity);

        ActivityMember m = new ActivityMember();
        m.setActivityId(activityId);
        m.setEventId(activity.getEventId());
        m.setUserEmail(userEmail);
        m.setEventRole(ActivityMemberRole.ATTENDEE);
        m.setStatus(MemberStatus.ACTIVE);
        // Se autoinscribe, no necesita confirmación posterior (a diferencia de una asignación)
        m.setConfirmationStatus(ConfirmationStatus.CONFIRMED);
        m.setRespondedAt(LocalDateTime.now());
        ActivityMember saved = memberRepository.save(m);

        auditService.log(activityId, activity.getEventId(), AuditActionType.MEMBER_ASSIGNED,
                userEmail, ActivityMemberRole.ATTENDEE.name(), "El usuario se inscribió como asistente.");

        notificationClient.sendActivityAssigned(
                userEmail, null, null, activity.getTitle(), activityId, activity.getEventId(),
                null, ActivityMemberRole.ATTENDEE.name(), null,
                activity.getStartDatetime() != null ? activity.getStartDatetime().toString() : null
        );

        return toResponse(saved);
    }

    /**
     * Valida que la actividad a inscribir no choque de horario con otra
     * actividad activa (asignada o inscrita) del mismo usuario (RF101).
     */
    private void checkScheduleConflict(String userEmail, Activity newActivity) {
        List<ActivityMember> existing = memberRepository.findByUserEmailAndStatus(userEmail, MemberStatus.ACTIVE);
        for (ActivityMember m : existing) {
            Activity other = activityService.findById(m.getActivityId());
            boolean overlap = newActivity.getStartDatetime().isBefore(other.getEndDatetime())
                    && other.getStartDatetime().isBefore(newActivity.getEndDatetime());
            if (overlap) {
                throw new BusinessException(
                        "Ya tienes una actividad programada en ese horario: '" + other.getTitle() + "'.",
                        HttpStatus.CONFLICT);
            }
        }
    }

    /**
     * Retorna las actividades en las que el usuario está inscrito como asistente (RF103).
     */
    public List<ActivityResponse> getMyEnrollments(String userEmail) {
        return memberRepository.findByUserEmailAndEventRoleAndStatus(
                        userEmail, ActivityMemberRole.ATTENDEE, MemberStatus.ACTIVE)
                .stream()
                .map(m -> activityService.getActivityById(m.getActivityId()))
                .toList();
    }

    /**
     * Retorna la agenda completa del usuario: todas las actividades en las que
     * tiene algún rol (STAFF, JUDGE, PARTICIPANT, ATTENDEE), sin importar
     * si fue asignado por el organizador o se inscribió por sí mismo.
     */
    public List<AgendaItemResponse> getMyAgenda(String userEmail, String name, Long eventId, LocalDate date, ActivityStatus status) {
        List<ActivityMember> members = memberRepository.findByUserEmailAndStatus(userEmail, MemberStatus.ACTIVE);

        if (members.isEmpty()) {
            return List.of();
        }

        // Mapa activityId -> member, para reconstruir el rol y estado de confirmación después
        Map<Long, ActivityMember> memberByActivityId = members.stream()
                .collect(Collectors.toMap(ActivityMember::getActivityId, m -> m));

        List<Long> activityIds = new ArrayList<>(memberByActivityId.keySet());

        Specification<Activity> spec = Specification.where(ActivitySpecification.hasIds(activityIds))
                .and(ActivitySpecification.hasEventId(eventId))
                .and(ActivitySpecification.hasName(name))
                .and(ActivitySpecification.hasStatus(status))
                .and(ActivitySpecification.onDate(date));

        List<Activity> activities = activityRepository.findAll(spec, Sort.by("startDatetime").ascending());

        return activities.stream()
                .map(activity -> {
                    ActivityMember m = memberByActivityId.get(activity.getId());
                    AgendaItemResponse item = new AgendaItemResponse();
                    item.setActivityId(activity.getId());
                    item.setActivityTitle(activity.getTitle());
                    item.setStartDatetime(activity.getStartDatetime());
                    item.setEndDatetime(activity.getEndDatetime());
                    item.setActivityStatus(activity.getStatus());
                    item.setEventId(activity.getEventId());
                    item.setEventTitle(eventClientService.getEventTitle(activity.getEventId()));
                    item.setRole(m.getEventRole());
                    item.setConfirmationStatus(m.getConfirmationStatus());
                    return item;
                })
                .toList();
    }
}
