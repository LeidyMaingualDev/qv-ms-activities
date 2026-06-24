package com.qvenly.qv_ms_activities.client;

import com.qvenly.qv_ms_activities.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cliente HTTP para qv-ms-events (uso interno).
 * Consulta el rol de evento de una persona y las fechas del evento.
 */
@Slf4j
@Component
public class EventInternalClient {

    private final WebClient webClient;
    private final String internalApiKey;

    public EventInternalClient(
            WebClient.Builder builder,
            @Value("${services.events.url}") String eventsUrl,
            @Value("${internal.api-key}") String internalApiKey) {
        this.webClient = builder.baseUrl(eventsUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /**
     * Devuelve el rol de evento (ORGANIZER/STAFF/MEMBER) de una persona en un evento,
     * o null si no es miembro activo o si la llamada falla.
     */
    @SuppressWarnings("unchecked")
    public String getMemberRole(Long eventId, String email) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/internal/{eventId}/members/role")
                            .queryParam("email", email)
                            .build(eventId))
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;
            Object data = response.get("data");
            return data != null ? data.toString() : null;
        } catch (Exception e) {
            log.warn("Error consultando rol en qv-ms-events [eventId={}, email={}]: {}",
                    eventId, email, e.getMessage());
            return null;
        }
    }

    /**
     * Devuelve las fechas de inicio y fin de un evento, consultando el endpoint interno.
     * Lanza BusinessException si no se pueden obtener (fail-closed).
     */
    @SuppressWarnings("unchecked")
    public EventDates getEventDates(Long eventId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/events/internal/{id}", eventId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("data") == null) {
                throw new BusinessException(
                        "No se pudieron obtener las fechas del evento.", HttpStatus.SERVICE_UNAVAILABLE);
            }
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Object start = data.get("startDatetime");
            Object end = data.get("endDatetime");
            if (start == null || end == null) {
                throw new BusinessException(
                        "El evento no tiene fechas válidas.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return new EventDates(
                    LocalDateTime.parse(start.toString()),
                    LocalDateTime.parse(end.toString()));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error consultando fechas del evento [eventId={}]: {}", eventId, e.getMessage());
            throw new BusinessException(
                    "No se pudo verificar las fechas del evento. Intenta más tarde.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** Fechas de inicio y fin de un evento. */
    public record EventDates(LocalDateTime startDatetime, LocalDateTime endDatetime) {}
}