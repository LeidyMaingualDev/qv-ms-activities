package com.qvenly.qv_ms_activities.service;

import com.qvenly.qv_ms_activities.client.EventInternalClient;
import com.qvenly.qv_ms_activities.exception.BusinessException;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Valida permisos de actividad consultando el rol de evento en qv-ms-events.
 * Fail-closed: si no se puede confirmar el rol, se deniega la operación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventAuthorizationService {

    private final EventInternalClient eventInternalClient;

    /**
     * Verifica que la persona sea ORGANIZER activo del evento.
     * Lanza 403 si no lo es (o si no se pudo confirmar el rol).
     */
    public void assertIsOrganizer(Long eventId, String performerEmail) {
        String role = eventInternalClient.getMemberRole(eventId, performerEmail);
        if (!"ORGANIZER".equals(role)) {
            throw new BusinessException(
                    "Solo los organizadores del evento pueden realizar esta acción.",
                    HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Verifica que la persona sea miembro activo del evento (cualquier rol:
     * ORGANIZER, STAFF o MEMBER). Lanza 403 si no lo es.
     */
    public void assertIsActiveMember(Long eventId, String email) {
        String role = eventInternalClient.getMemberRole(eventId, email);
        if (role == null) {
            throw new BusinessException(
                    "No tienes acceso a esta actividad: no eres miembro de este evento.",
                    HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Valida que la persona a asignar pueda recibir el rol de actividad indicado,
     * según su rol dentro del evento:
     *   ORGANIZER → no puede asignarse a ninguna actividad
     *   STAFF     → solo como STAFF de actividad
     *   MEMBER    → solo como PARTICIPANT o JUDGE
     *   (no miembro activo) → no se puede asignar
     */
    public void assertAssignableRole(Long eventId, String targetEmail, ActivityMemberRole activityRole) {
        String eventRole = eventInternalClient.getMemberRole(eventId, targetEmail);

        if (eventRole == null) {
            throw new BusinessException(
                    "La persona no es miembro activo del evento y no puede ser asignada.",
                    HttpStatus.FORBIDDEN);
        }

        switch (eventRole) {
            case "ORGANIZER" -> throw new BusinessException(
                    "Un organizador del evento no puede ser asignado a actividades.",
                    HttpStatus.UNPROCESSABLE_ENTITY);

            case "STAFF" -> {
                if (activityRole != ActivityMemberRole.STAFF) {
                    throw new BusinessException(
                            "El personal de apoyo solo puede asignarse con el rol STAFF en la actividad.",
                            HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }

            case "MEMBER" -> {
                if (activityRole != ActivityMemberRole.PARTICIPANT) {
                    throw new BusinessException(
                            "Un miembro solo puede asignarse como PARTICIPANT.",
                            HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }

            default -> throw new BusinessException(
                    "Rol de evento no reconocido: " + eventRole,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}