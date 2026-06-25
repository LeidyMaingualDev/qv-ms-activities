package com.qvenly.qv_ms_activities.service;

import com.qvenly.qv_ms_activities.client.NotificationClient;
import com.qvenly.qv_ms_activities.client.EventInternalClient;
import com.qvenly.qv_ms_activities.exception.BusinessException;
import com.qvenly.qv_ms_activities.model.dto.request.CancelActivityRequest;
import com.qvenly.qv_ms_activities.model.dto.request.CreateActivityRequest;
import com.qvenly.qv_ms_activities.model.dto.request.UpdateActivityRequest;
import com.qvenly.qv_ms_activities.model.dto.response.ActivityResponse;
import com.qvenly.qv_ms_activities.model.entity.Activity;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.model.enums.ActivityStatus;
import com.qvenly.qv_ms_activities.model.enums.AuditActionType;
import com.qvenly.qv_ms_activities.model.enums.MemberStatus;
import com.qvenly.qv_ms_activities.repository.ActivityMemberRepository;
import com.qvenly.qv_ms_activities.repository.ActivityRepository;
import com.qvenly.qv_ms_activities.specification.ActivitySpecification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final AuditService auditService;
    private final NotificationClient notificationClient;
    private final ActivityMemberRepository memberRepository;
    private final EventAuthorizationService eventAuthorizationService;
    private final EventInternalClient eventInternalClient;

    @Transactional
    public ActivityResponse createActivity(CreateActivityRequest req, String performerEmail) {
        eventAuthorizationService.assertIsOrganizer(req.getEventId(), performerEmail);
        if (!req.getEndDatetime().isAfter(req.getStartDatetime())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la fecha de inicio.", HttpStatus.BAD_REQUEST);
        }
        assertWithinEventDates(req.getEventId(), req.getStartDatetime(), req.getEndDatetime());

        if (Boolean.TRUE.equals(req.getEnrollmentEnabled()) && req.getMaxEnrollment() != null && req.getMaxEnrollment() < 1) {
            throw new BusinessException("El número máximo de asistentes debe ser mayor a cero.", HttpStatus.BAD_REQUEST);
        }

        Activity a = new Activity();
        a.setEventId(req.getEventId()); a.setTitle(req.getTitle());
        a.setDescription(req.getDescription()); a.setLocation(req.getLocation());
        a.setStartDatetime(req.getStartDatetime()); a.setEndDatetime(req.getEndDatetime());
        a.setStatus(ActivityStatus.PENDING);
        a.setCreatedByEmail(performerEmail);
        a.setEnrollmentEnabled(Boolean.TRUE.equals(req.getEnrollmentEnabled()));
        a.setMaxAttendees(req.getMaxEnrollment());

        Activity saved = activityRepository.save(a);
        auditService.log(saved.getId(), saved.getEventId(), AuditActionType.ACTIVITY_CREATED,
                performerEmail, null, "Actividad '" + saved.getTitle() + "' creada.");
        return toResponse(saved);
    }

    @Transactional
    public ActivityResponse updateActivity(Long id, UpdateActivityRequest req, String performerEmail) {
        Activity a = findById(id);
        eventAuthorizationService.assertIsOrganizer(a.getEventId(), performerEmail);
        assertEditable(a);
        String before = "título='" + a.getTitle() + "'";
        if (req.getTitle() != null) a.setTitle(req.getTitle());
        if (req.getDescription() != null) a.setDescription(req.getDescription());
        if (req.getLocation() != null) a.setLocation(req.getLocation());
        if (req.getStartDatetime() != null) a.setStartDatetime(req.getStartDatetime());
        if (req.getEndDatetime() != null) a.setEndDatetime(req.getEndDatetime());

        if (req.getEnrollmentEnabled() != null) a.setEnrollmentEnabled(req.getEnrollmentEnabled());

        if (req.getMaxEnrollment() != null) {
            if (req.getMaxEnrollment() < 1) {
                throw new BusinessException("El número máximo de asistentes debe ser mayor a cero.", HttpStatus.BAD_REQUEST);
            }
            long currentAttendees = memberRepository.countByActivityIdAndEventRoleAndStatus(
                    id, ActivityMemberRole.ATTENDEE, MemberStatus.ACTIVE);
            if (req.getMaxEnrollment() < currentAttendees) {
                throw new BusinessException(
                        "No puedes bajar el cupo a " + req.getMaxEnrollment() + ", ya hay " + currentAttendees + " asistentes inscritos.",
                        HttpStatus.CONFLICT);
            }
            a.setMaxAttendees(req.getMaxEnrollment());
        }

        if (a.getEndDatetime() != null && a.getStartDatetime() != null
                && !a.getEndDatetime().isAfter(a.getStartDatetime())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la fecha de inicio.", HttpStatus.BAD_REQUEST);
        }
        assertWithinEventDates(a.getEventId(), a.getStartDatetime(), a.getEndDatetime());
        Activity updated = activityRepository.save(a);
        auditService.log(id, a.getEventId(), AuditActionType.ACTIVITY_EDITED, performerEmail, null,
                "Antes: " + before + " | Después: título='" + updated.getTitle() + "'");
        return toResponse(updated);
    }

    @Transactional
    public ActivityResponse startActivity(Long id, String performerEmail) {
        Activity a = findById(id);
        eventAuthorizationService.assertIsOrganizer(a.getEventId(), performerEmail);
        if (a.getStatus() != ActivityStatus.PENDING) {
            throw new BusinessException("Solo se puede iniciar una actividad PENDING.", HttpStatus.CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(a.getStartDatetime())) {
            throw new BusinessException("No se puede iniciar la actividad antes de su fecha de inicio.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (now.isAfter(a.getEndDatetime())) {
            throw new BusinessException("No se puede iniciar la actividad: su rango de fechas ya finalizó. Cancélala si no se realizó.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        a.setStatus(ActivityStatus.IN_PROGRESS);
        Activity updated = activityRepository.save(a);
        auditService.log(id, a.getEventId(), AuditActionType.ACTIVITY_STARTED, performerEmail, null, "Actividad iniciada.");
        return toResponse(updated);
    }

    @Transactional
    public ActivityResponse finishActivity(Long id, String performerEmail) {
        Activity a = findById(id);
        eventAuthorizationService.assertIsOrganizer(a.getEventId(), performerEmail);
        if (a.getStatus() != ActivityStatus.IN_PROGRESS) {
            throw new BusinessException("Solo se puede finalizar una actividad IN_PROGRESS.", HttpStatus.CONFLICT);
        }
        if (LocalDateTime.now().isBefore(a.getStartDatetime())) {
            throw new BusinessException("No se puede finalizar una actividad que aún no ha iniciado.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        a.setStatus(ActivityStatus.FINISHED);
        Activity updated = activityRepository.save(a);
        auditService.log(id, a.getEventId(), AuditActionType.ACTIVITY_FINISHED, performerEmail, null, "Actividad finalizada.");
        return toResponse(updated);
    }

    @Transactional
    public ActivityResponse cancelActivity(Long id, CancelActivityRequest req, String performerEmail) {
        Activity a = findById(id);
        eventAuthorizationService.assertIsOrganizer(a.getEventId(), performerEmail);
        assertEditable(a);
        a.setStatus(ActivityStatus.CANCELLED); a.setCancelReason(req.getCancelReason());
        Activity updated = activityRepository.save(a);
        auditService.log(id, a.getEventId(), AuditActionType.ACTIVITY_CANCELLED, performerEmail, null,
                "Motivo: " + req.getCancelReason());
        memberRepository.findByActivityIdAndStatus(
                        id, com.qvenly.qv_ms_activities.model.enums.MemberStatus.ACTIVE)
                .forEach(m -> notificationClient.sendActivityCancelled(
                        m.getUserEmail(), null, null,
                        updated.getTitle(), id, updated.getEventId(), null, req.getCancelReason()
                ));
        return toResponse(updated);
    }

    public List<ActivityResponse> getActivitiesByEvent(Long eventId, String name, LocalDate date, ActivityStatus status) {
        Specification<Activity> spec = Specification.where(ActivitySpecification.hasEventId(eventId))
                .and(ActivitySpecification.hasName(name))
                .and(ActivitySpecification.hasStatus(status))
                .and(ActivitySpecification.onDate(date));

        return activityRepository.findAll(spec, Sort.by("startDatetime").ascending())
                .stream().map(this::toResponse).toList();
    }

    public ActivityResponse getActivityById(Long id) { return toResponse(findById(id)); }

    public Activity findById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada: " + id));
    }

    private void assertEditable(Activity a) {
        if (a.getStatus() == ActivityStatus.FINISHED || a.getStatus() == ActivityStatus.CANCELLED) {
            throw new BusinessException("No se puede modificar una actividad en estado " + a.getStatus(), HttpStatus.CONFLICT);
        }
    }

    /**
     * Valida que la actividad caiga completamente dentro del rango de fechas del evento.
     * Consulta las fechas del evento vía cliente interno (fail-closed).
     */
    private void assertWithinEventDates(Long eventId, LocalDateTime activityStart, LocalDateTime activityEnd) {
        EventInternalClient.EventDates ev = eventInternalClient.getEventDates(eventId);
        if (activityStart.isBefore(ev.startDatetime()) || activityEnd.isAfter(ev.endDatetime())) {
            throw new BusinessException(
                    String.format("La actividad debe estar dentro del rango del evento (%s a %s).",
                            ev.startDatetime(), ev.endDatetime()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public ActivityResponse toResponse(Activity a) {
        ActivityResponse r = new ActivityResponse();
        r.setId(a.getId()); r.setEventId(a.getEventId()); r.setTitle(a.getTitle());
        r.setDescription(a.getDescription()); r.setLocation(a.getLocation());
        r.setStartDatetime(a.getStartDatetime()); r.setEndDatetime(a.getEndDatetime());
        r.setStatus(a.getStatus()); r.setCancelReason(a.getCancelReason());
        r.setCreatedByEmail(a.getCreatedByEmail()); r.setCreatedAt(a.getCreatedAt()); r.setUpdatedAt(a.getUpdatedAt());
        r.setEnrollmentEnabled(a.getEnrollmentEnabled());
        r.setMaxEnrollment(a.getMaxAttendees());
        r.setCurrentEnrollments((int) memberRepository.countByActivityIdAndEventRoleAndStatus(
                a.getId(), ActivityMemberRole.ATTENDEE, MemberStatus.ACTIVE));
        return r;
    }
}