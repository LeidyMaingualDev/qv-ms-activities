package com.qvenly.qv_ms_activities.controller;

import com.qvenly.qv_ms_activities.model.dto.response.ApiResponse;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.model.enums.MemberStatus;
import com.qvenly.qv_ms_activities.repository.ActivityMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/activities")
@RequiredArgsConstructor
public class InternalActivityController {

    private final ActivityMemberRepository memberRepository;

    /**
     * Devuelve los emails únicos de miembros activos de un evento con los roles indicados.
     * Solo para uso interno entre microservicios — no expuesto por el Gateway.
     */
    @GetMapping("/members-by-event")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMembersByEventAndRoles(
            @RequestParam Long eventId,
            @RequestParam List<ActivityMemberRole> roles) {

        List<Map<String, Object>> result = memberRepository
                .findEmailsByEventIdAndRoles(eventId, roles)
                .stream()
                .map(row -> Map.<String, Object>of(
                        "email", row[0],
                        "eventId", row[1]))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Miembros obtenidos.", result));
    }

    @GetMapping("/roles-by-member")
     public ResponseEntity<ApiResponse<List<String>>> getRolesByMember(
                @RequestParam String email,
                @RequestParam Long eventId) {

        List<String> roles = memberRepository
                .findByUserEmailAndStatus(email, MemberStatus.ACTIVE)
                .stream()
                .filter(m -> m.getEventId().equals(eventId))
                .map(m -> m.getEventRole().name())
                .distinct()
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Roles obtenidos.", roles));
        }
}