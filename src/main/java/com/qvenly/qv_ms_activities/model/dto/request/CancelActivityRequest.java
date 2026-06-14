package com.qvenly.qv_ms_activities.model.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelActivityRequest {
    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String cancelReason;
}
