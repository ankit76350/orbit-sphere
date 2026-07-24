package com.orbitastra.backend.controllers.core;

import java.util.List;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.core.CreateNotificationRequest;
import com.orbitastra.backend.models.core.Notification;
import com.orbitastra.backend.services.core.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .schoolId(request.getSchoolId())
                .recipientDocsId(request.getRecipientDocsId())
                .channel(request.getChannel())
                .message(request.getMessage())
                .build();
        Notification created = notificationService.createNotification(notification);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable String id) {
        Notification notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/recipient/{recipientDocsId}")
    public ResponseEntity<List<Notification>> getNotificationsByRecipient(@PathVariable String recipientDocsId) {
        List<Notification> notifications = notificationService.getNotificationsByRecipient(recipientDocsId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/recipient/{recipientDocsId}/unsent")
    public ResponseEntity<List<Notification>> getUnsentNotificationsByRecipient(@PathVariable String recipientDocsId) {
        List<Notification> notifications = notificationService.getNotificationsByRecipientAndSentStatus(recipientDocsId, false);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Notification>> getNotificationsBySchool(@PathVariable String schoolId) {
        List<Notification> notifications = notificationService.getNotificationsBySchool(schoolId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/mark-sent")
    public ResponseEntity<Notification> markAsSent(@PathVariable String id) {
        Notification updated = notificationService.markAsSent(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Notification deleted successfully."));
    }
}
