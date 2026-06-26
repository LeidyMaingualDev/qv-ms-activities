package com.qvenly.qv_ms_activities.controller;

import com.qvenly.qv_ms_activities.model.dto.request.AssignMemberRequest;
import com.qvenly.qv_ms_activities.model.dto.request.CancelParticipationRequest;
import com.qvenly.qv_ms_activities.model.dto.response.ActivityMemberResponse;
import com.qvenly.qv_ms_activities.model.dto.response.ApiResponse;
import com.qvenly.qv_ms_activities.model.entity.Activity;
import com.qvenly.qv_ms_activities.model.enums.ActivityMemberRole;
import com.qvenly.qv_ms_activities.service.ActivityMemberService;
import com.qvenly.qv_ms_activities.service.ActivityService;
import com.qvenly.qv_ms_activities.service.EventAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/activities/{activityId}")
@RequiredArgsConstructor
public class ActivityMemberController {

    private final ActivityMemberService memberService;
    private final ActivityService activityService;
    private final EventAuthorizationService eventAuthorizationService;

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<ActivityMemberResponse>>> getMembers(
            @PathVariable Long activityId,
            @RequestParam(required = false) ActivityMemberRole role,
            @RequestHeader("X-User-Email") String userEmail) {
        Activity activity = activityService.findById(activityId);
        eventAuthorizationService.assertIsOrganizer(activity.getEventId(), userEmail);
        List<ActivityMemberResponse> members = memberService.getMembersByActivity(activityId);
        if (role != null) {
            members = members.stream().filter(m -> m.getEventRole() == role).toList();
        }
        return ResponseEntity.ok(ApiResponse.success("Miembros obtenidos.", members));
    }

    @GetMapping("/members/me")
    public ResponseEntity<ApiResponse<ActivityMemberResponse>> getMyAssignment(
            @PathVariable Long activityId,
            @RequestHeader("X-User-Email") String userEmail) {
        ActivityMemberResponse my = memberService.getMembersByActivity(activityId).stream()
                .filter(m -> m.getUserEmail().equalsIgnoreCase(userEmail))
                .findFirst()
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Asignación obtenida.", my));
    }

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<ActivityMemberResponse>> assign(
            @PathVariable Long activityId,
            @Valid @RequestBody AssignMemberRequest request,
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Miembro asignado.", memberService.assignMember(activityId, request, userEmail)));
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Long activityId, @PathVariable Long memberId,
            @RequestHeader("X-User-Email") String userEmail) {
        memberService.removeMember(activityId, memberId, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Miembro removido."));
    }

    @PatchMapping("/members/confirm")
    public ResponseEntity<ApiResponse<ActivityMemberResponse>> confirm(
            @PathVariable Long activityId, @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Participación confirmada.", memberService.confirmParticipation(activityId, userEmail)));
    }

    @PatchMapping("/members/cancel")
    public ResponseEntity<ApiResponse<ActivityMemberResponse>> cancelParticipation(
            @PathVariable Long activityId,
            @Valid @RequestBody CancelParticipationRequest request,
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Participación cancelada.", memberService.cancelParticipation(activityId, userEmail, request)));
    }

    @PostMapping("/enroll")
    public ResponseEntity<ApiResponse<ActivityMemberResponse>> enroll(
            @PathVariable Long activityId,
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inscripción realizada.", memberService.enrollSelf(activityId, userEmail)));
    }
}