package com.qvenly.qv_ms_activities.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente HTTP para qv-ms-notifications.
 * Llamadas fire-and-forget — si falla no detiene el flujo principal.
 */
@Slf4j
@Component
public class NotificationClient {

    private final WebClient webClient;

    public NotificationClient(
            WebClient.Builder builder,
            @Value("${services.notifications.url}") String notificationsUrl) {
        this.webClient = builder.baseUrl(notificationsUrl).build();
    }

    /** RF70 — Asignado a actividad */
    public void sendActivityAssigned(String recipientEmail, String recipientName,
                                     Long recipientUserId, String activityTitle,
                                     Long activityId, Long eventId, String eventTitle,
                                     String role, String function, String activityDatetime) {
        Map<String, Object> body = new HashMap<>();
        body.put("type",              "ACTIVITY_ASSIGNED");
        body.put("activityTitle",     activityTitle);
        body.put("activityId",        activityId);
        body.put("eventId",           eventId);
        body.put("eventTitle",        eventTitle != null ? eventTitle : "");
        body.put("recipientEmail",    recipientEmail);
        body.put("recipientName",     recipientName != null ? recipientName : "");
        body.put("recipientUserId",   recipientUserId);
        body.put("detail",            role);
        body.put("activityDatetime",  activityDatetime != null ? activityDatetime : "");
        post("/api/notifications/activity-assigned", body);
    }

    /** RF73.1 — Actividad cancelada */
    public void sendActivityCancelled(String recipientEmail, String recipientName,
                                      Long recipientUserId, String activityTitle,
                                      Long activityId, Long eventId, String eventTitle,
                                      String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("type",            "ACTIVITY_CANCELLED");
        body.put("activityTitle",   activityTitle);
        body.put("activityId",      activityId);
        body.put("eventId",         eventId);
        body.put("eventTitle",      eventTitle != null ? eventTitle : "");
        body.put("recipientEmail",  recipientEmail);
        body.put("recipientName",   recipientName != null ? recipientName : "");
        body.put("recipientUserId", recipientUserId);
        body.put("detail",          reason != null ? reason : "");
        post("/api/notifications/activity-cancelled", body);
    }

    /** Actividad actualizada */
    public void sendActivityUpdated(String recipientEmail, String recipientName,
                                    Long recipientUserId, String activityTitle,
                                    Long activityId, Long eventId, String eventTitle,
                                    String detail) {
        Map<String, Object> body = new HashMap<>();
        body.put("type",            "ACTIVITY_UPDATED");
        body.put("activityTitle",   activityTitle);
        body.put("activityId",      activityId);
        body.put("eventId",         eventId);
        body.put("eventTitle",      eventTitle != null ? eventTitle : "");
        body.put("recipientEmail",  recipientEmail);
        body.put("recipientName",   recipientName != null ? recipientName : "");
        body.put("recipientUserId", recipientUserId);
        body.put("detail",          detail != null ? detail : "");
        post("/api/notifications/activity-updated", body);
    }

    private void post(String uri, Object body) {
        try {
            webClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                            null,
                            err -> log.warn("Notificación fallida [{}]: {}", uri, err.getMessage())
                    );
        } catch (Exception e) {
            log.warn("Error al llamar a qv-ms-notifications [{}]: {}", uri, e.getMessage());
        }
    }
}