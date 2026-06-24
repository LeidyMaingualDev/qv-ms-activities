package com.qvenly.qv_ms_activities.controller;

import com.qvenly.qv_ms_activities.model.dto.request.CancelActivityRequest;
import com.qvenly.qv_ms_activities.model.dto.request.CreateActivityRequest;
import com.qvenly.qv_ms_activities.model.dto.request.UpdateActivityRequest;
import com.qvenly.qv_ms_activities.model.dto.response.ActivityImageResponse;
import com.qvenly.qv_ms_activities.model.dto.response.ActivityResponse;
import com.qvenly.qv_ms_activities.model.dto.response.AgendaItemResponse;
import com.qvenly.qv_ms_activities.model.dto.response.ApiResponse;
import com.qvenly.qv_ms_activities.model.dto.response.AuditLogResponse;
import com.qvenly.qv_ms_activities.model.enums.ActivityStatus;
import com.qvenly.qv_ms_activities.service.ActivityImageService;
import com.qvenly.qv_ms_activities.service.ActivityMemberService;
import com.qvenly.qv_ms_activities.service.ActivityService;
import com.qvenly.qv_ms_activities.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final AuditService auditService;
    private final ActivityImageService activityImageService;

    private final ActivityMemberService activityMemberService;

    @PostMapping
    public ResponseEntity<ApiResponse<ActivityResponse>> create(
            @Valid @RequestBody CreateActivityRequest request,
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Actividad creada.", activityService.createActivity(request, userEmail)));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getByEvent(
            @PathVariable Long eventId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ActivityStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Actividades obtenidas.",
                activityService.getActivitiesByEvent(eventId, name, date, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ActivityResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Actividad obtenida.", activityService.getActivityById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ActivityResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateActivityRequest request,
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Actividad actualizada.", activityService.updateActivity(id, request, userEmail)));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<ApiResponse<ActivityResponse>> start(
            @PathVariable Long id, @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Actividad iniciada.", activityService.startActivity(id, userEmail)));
    }

    @PatchMapping("/{id}/finish")
    public ResponseEntity<ApiResponse<ActivityResponse>> finish(
            @PathVariable Long id, @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Actividad finalizada.", activityService.finishActivity(id, userEmail)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ActivityResponse>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelActivityRequest request,
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Actividad cancelada.", activityService.cancelActivity(id, request, userEmail)));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAudit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Auditoría obtenida.", auditService.getAuditLog(id)));
    }

    @GetMapping("/my-enrollments")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getMyEnrollments(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Actividades inscritas obtenidas.",
                activityMemberService.getMyEnrollments(userEmail)));
    }

    @GetMapping("/my-agenda")
    public ResponseEntity<ApiResponse<List<AgendaItemResponse>>> getMyAgenda(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ActivityStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Agenda obtenida.",
                activityMemberService.getMyAgenda(userEmail, name, eventId, date, status)));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<ActivityImageResponse>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Imagen subida exitosamente.",
                        activityImageService.uploadImage(id, file)));
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<ActivityImageResponse>>> getImages(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Imágenes obtenidas.",
                activityImageService.getImagesByActivity(id)));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        activityImageService.deleteImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Imagen eliminada.", null));
    }
}