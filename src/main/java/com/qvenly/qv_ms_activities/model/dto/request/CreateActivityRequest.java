package com.qvenly.qv_ms_activities.model.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateActivityRequest {
    @NotNull(message = "El ID del evento es obligatorio")
    private Long eventId;
    @NotBlank(message = "El título de la actividad es obligatorio")
    private String title;
    private String description;
    private String location;
    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime startDatetime;
    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime endDatetime;
}
