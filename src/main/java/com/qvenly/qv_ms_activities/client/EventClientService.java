package com.qvenly.qv_ms_activities.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
@Slf4j
public class EventClientService {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.events.url}")
    private String eventsServiceUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public EventClientService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @SuppressWarnings("unchecked")
    public String getEventTitle(Long eventId) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(eventsServiceUrl + "/api/events/internal/" + eventId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("data") == null) return null;
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return (String) data.get("title");

        } catch (Exception e) {
            log.error("Error al consultar evento ID {}: {}", eventId, e.getMessage());
            return null;
        }
    }
}