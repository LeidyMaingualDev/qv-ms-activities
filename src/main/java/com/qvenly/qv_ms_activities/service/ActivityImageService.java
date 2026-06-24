package com.qvenly.qv_ms_activities.service;

import com.qvenly.qv_ms_activities.exception.BusinessException;
import com.qvenly.qv_ms_activities.model.dto.response.ActivityImageResponse;
import com.qvenly.qv_ms_activities.model.entity.ActivityImage;
import com.qvenly.qv_ms_activities.repository.ActivityImageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityImageService {

    private static final int MAX_IMAGES_PER_ACTIVITY = 10;

    private final ActivityImageRepository imageRepository;
    private final SupabaseStorageService storageService;

    @Transactional
    public ActivityImageResponse uploadImage(Long activityId, MultipartFile file) {
        long currentCount = imageRepository.findByActivityId(activityId).size();
        if (currentCount >= MAX_IMAGES_PER_ACTIVITY) {
            throw new BusinessException(
                    "Esta actividad ya tiene el máximo de " + MAX_IMAGES_PER_ACTIVITY + " imágenes permitidas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        validateImageFile(file);

        String imageUrl = storageService.uploadFile(file, "activities/" + activityId);

        ActivityImage image = new ActivityImage();
        image.setActivityId(activityId);
        image.setImageUrl(imageUrl);
        ActivityImage saved = imageRepository.save(image);

        return toResponse(saved);
    }

    @Transactional
    public void deleteImage(Long activityId, Long imageId) {
        ActivityImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("Imagen no encontrada.", HttpStatus.NOT_FOUND));

        if (!image.getActivityId().equals(activityId)) {
            throw new BusinessException("La imagen no pertenece a esta actividad.", HttpStatus.BAD_REQUEST);
        }

        storageService.deleteFile(image.getImageUrl());
        imageRepository.delete(image);
    }

    public List<ActivityImageResponse> getImagesByActivity(Long activityId) {
        return imageRepository.findByActivityId(activityId).stream().map(this::toResponse).toList();
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío.", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("El archivo debe ser una imagen (JPG, PNG, WEBP).", HttpStatus.BAD_REQUEST);
        }
        long maxSizeBytes = 5 * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException("La imagen no debe superar los 5MB.", HttpStatus.BAD_REQUEST);
        }
    }

    private ActivityImageResponse toResponse(ActivityImage img) {
        ActivityImageResponse r = new ActivityImageResponse();
        r.setId(img.getId());
        r.setActivityId(img.getActivityId());
        r.setImageUrl(img.getImageUrl());
        r.setUploadedAt(img.getUploadedAt());
        return r;
    }
}