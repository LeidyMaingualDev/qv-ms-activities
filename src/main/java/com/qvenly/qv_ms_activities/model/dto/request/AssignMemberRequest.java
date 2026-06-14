package com.qvenly.qv_ms_activities.model.dto.request;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignMemberRequest {
    @NotBlank(message = "El email del usuario es obligatorio")
    @Email(message = "El email no tiene formato válido")
    private String userEmail;
    @NotNull(message = "El rol es obligatorio")
    private ActivityMemberRole eventRole;
    private String functionDescription;
}
